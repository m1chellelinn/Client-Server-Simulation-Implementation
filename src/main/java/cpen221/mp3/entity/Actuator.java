package cpen221.mp3.entity;

import cpen221.mp3.client.*;
import cpen221.mp3.event.*;

import java.util.*;
import java.io.*;
import java.net.*;

/**
 * Abstraction Function:
 *     Represents an actuator with a unique identifier, client ID, type, state, and the ability to send/receive events/commands from/to a server.
 *
 * Representation Invariant:
 *     - id, type are immutable and final.
 *     - clientId, state, serverPort, serverIP are only modified in synchronized methods.
 *     - host, port are never modified except in the constructor.
 *     - actuatorToServerSocket, actuatorToServerWriter, serverToActuatorSocket, serverToActuatorRemote, serverToActuatorReader contain thread-safe datatypes.
 *
 * Thread Safety Argument:
 *      This class is threadsafe because:
 *      - id, type are immutable and final
 *      - clientId, state, serverPort, serverIP are only modified in synchronized methods
 *      - host, port are never modified except in the constructor
 *      - actuatorToServerSocket, actuatorToServerWriter, serverToActuatorSocket, serverToActuatorRemote, serverToActuatorReader
 *          contain thread-safe datatypes
 */
public class Actuator implements Entity, Runnable {
    private final int id;
    private int clientId;
    private final String type;
    private boolean state;
    private double eventGenerationFrequency = 0.2; // default value in Hz (1/s)

    // the following specifies the http endpoint that the actuator should send events to
    private String serverIP = "127.0.0.1";
    private int serverPort = 0;
    public Socket actuatorToServerSocket;
    //This will be our output stream to server
    private PrintWriter actuatorToServerWriter;

    // the following specifies the http endpoint that the actuator should be able to receive commands on from server
    private String host = "127.0.0.1";
    private int port = 0;
    public ServerSocket serverToActuatorSocket;
    public Socket serverToActuatorRemote;
    //This will be our input stream from server
    private BufferedReader serverToActuatorReader;

    /**
     * Constructs a new Actuator with the given ID, type, and initial state.
     * The actuator is unregistered for any client.
     *
     * @param id the unique identifier of the actuator
     * @param type the type of the actuator
     * @param init_state the initial state of the actuator
     */
    public Actuator(int id, String type, boolean init_state) {
        this.id = id;
        this.clientId = -1;         // remains unregistered
        this.type = type;
        this.state = init_state;
    }
    /**
     * Constructs a new Actuator with the given ID, client ID, type, and initial state.
     * The actuator is registered for the specified client.
     *
     * @param id the unique identifier of the actuator
     * @param clientId the client ID to which the actuator is registered
     * @param type the type of the actuator
     * @param init_state the initial state of the actuator
     */
    public Actuator(int id, int clientId, String type, boolean init_state) {
        this.id = id;
        this.clientId = clientId;   // registered for the client
        this.type = type;
        this.state = init_state;
    }
    /**
     * Constructs a new Actuator with the given ID, type, initial state, server IP, and server port.
     * The actuator is unregistered for any client.
     *
     * @param id the unique identifier of the actuator
     * @param type the type of the actuator
     * @param init_state the initial state of the actuator
     * @param serverIP the IP address of the server to which the actuator sends events
     * @param serverPort the port number of the server
     */
    public Actuator(int id, String type, boolean init_state, String serverIP, int serverPort) {
        this.id = id;
        this.clientId = -1;         // remains unregistered
        this.type = type;
        this.state = init_state;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        //randomizePort();
        Thread actuatorThread = new Thread(this);
        actuatorThread.start();
    }
    /**
     * Constructs a new Actuator with the given ID, client ID, type, initial state, server IP, and server port.
     * The actuator is registered for the specified client.
     *
     * @param id the unique identifier of the actuator
     * @param clientId the client ID to which the actuator is registered
     * @param type the type of the actuator
     * @param init_state the initial state of the actuator
     * @param serverIP the IP address of the server to which the actuator sends events
     * @param serverPort the port number of the server
     */
    public Actuator(int id, int clientId, String type, boolean init_state, String serverIP, int serverPort) {
        this.id = id;
        this.clientId = clientId;   // registered for the client
        this.type = type;
        this.state = init_state;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        //randomizePort();
        Thread actuatorThread = new Thread(this);
        actuatorThread.start();
    }
    /**
     * Constructs a new Actuator with the given ID, client ID, type, host, and port.
     *
     * @param id the unique identifier of the actuator
     * @param clientId the client ID to which the actuator is registered
     * @param type the type of the actuator
     * @param host the IP address of the actuator for receiving commands
     * @param port the port number on which the actuator listens for commands
     */
    public Actuator(int id, int clientId, String type, String host, int port) {
        this.id = id;
        this.clientId = clientId;
        this.type = type;
        this.host = host;
        this.port = port;

        //Default server IP and port, may not be needed
        this.serverIP = "127.0.0.1";
        this.serverPort = 0;
    }
    /**
     * The main execution loop for the Actuator. Connects to the server, registers itself,
     * and continuously listens for server commands to update the actuator's state.
     * Uses separate threads for the actuator-to-server and server-to-actuator connections.
     */
    public void run() {
        //Infinite loop, loop until socket connects
        this.instantiateActuatorToServer();

        while (clientId == -1) {
            //Wait here until we're assigned a client
        }

        //Then, we can instantiate the server -> actuator connection, and listen for server requests
        this.instantiateServerToActuator();


        try {

            //This next infinite loop generates random events and sends them to server
            Random random = new Random();
            while (true) {
                try {
                    Thread.sleep((long) (1000 / eventGenerationFrequency));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e + "... sensor thread sleep failed");
                }
                int randomValue = random.nextInt(2);
                Event event = new ActuatorEvent(System.currentTimeMillis(), this.clientId, this.id, this.type, (randomValue == 1));
                sendEvent(event);

                String serverMessage = serverToActuatorReader.readLine();
                if (serverMessage != null) {
                    processServerMessage(Request.parse(serverMessage));
                }
            }
        } catch (IOException e) {
            //Do nothing
        }
    }
    /**
     * Instantiates the actuator-to-server connection by creating a Socket and connecting to the server.
     * Initializes the output stream for communication with the server.
     *
     * @throws RuntimeException if the instantiation of the actuator-to-server connection or output stream fails
     */
    private void instantiateActuatorToServer() {
        //Instantiate the actuator -> server connection
        this.actuatorToServerSocket = new Socket();
        while(!actuatorToServerSocket.isConnected()) {
            try {
                //Instantiate the sockets objects, both ways
                this.actuatorToServerSocket = new Socket(serverIP, serverPort);
            } catch (IOException e) {
                //Do nothing
            }
        }
        try {
            this.actuatorToServerWriter = new PrintWriter(actuatorToServerSocket.getOutputStream());

        } catch (IOException e) {
            throw new RuntimeException(e + "... actuator PrintWriter instantiation failed");
        }
    }
    /**
     * Instantiates the server-to-actuator connection by creating a ServerSocket on a free port and
     * accepting a connection from the server. Initializes the input stream for communication with the server.
     *
     * @throws RuntimeException if the instantiation of the server-to-actuator connection or input stream fails
     */
    private void instantiateServerToActuator() {
        //Instantiate the server -> actuator connection on a free port
        int port = 4000;
        while (port < 50000) {
            try {
                this.serverToActuatorSocket = new ServerSocket(port);
                break;
            } catch (IOException e) {
                //Try again with a new port :)
            }
            port += 5;
        }
        this.port = port;

        //After creating the unique port, send "this.toString()" into the socket stream
        // so that the server knows we're ready to connect
        actuatorToServerWriter.println(this);
        actuatorToServerWriter.flush();

        try {
            System.out.println("Actuator " + this.getId() + " ready to connect on port " + this.port);
            this.serverToActuatorRemote = serverToActuatorSocket.accept();
            this.serverToActuatorReader = new BufferedReader(new InputStreamReader(serverToActuatorRemote.getInputStream()));
        }
        catch (IOException e) {
            throw new RuntimeException(e + "... actuator ServerSocket / bytestream instantiation failed");
        }
    }

    // returns the id of the entity
    public int getId() {
        return id;
    }
    // returns the client id of the entity
    public int getClientId() {
        return clientId;
    }
    // returns the type of the entity
    public String getType() {
        return type;
    }
    // returns true if the entity is an actuator
    public boolean isActuator() {
        return true;
    }
    // returns the state of the entity
    public boolean getState() {
        return state;
    }
    // returns the IP address of the entity
    public String getIP() {
        return host;
    }
    // returns the port number of the entity
    public int getPort() {
        return port;
    }
    // updates the state of the entity
    public synchronized void updateState(boolean new_state) {
        this.state = new_state;
    }

    /**
     * Registers the actuator for the given client
     * 
     * @return true if the actuator is new (clientID is -1 already) and gets successfully registered or if it is already registered for clientId, else false
     */
    public boolean registerForClient(int clientId) {
        if (this.clientId == -1) {
            this.clientId = clientId;
            return true;
        } else if (this.clientId == clientId) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets or updates the http endpoint that 
     * the actuator should send events to
     * 
     * @param serverIP the IP address of the endpoint
     * @param serverPort the port number of the endpoint
     */
    public void setEndpoint(String serverIP, int serverPort){
        this.serverIP = serverIP;
        this.serverPort = serverPort;

        Thread actuatorThread = new Thread(this);
        actuatorThread.start();
    }

    /*//randomize server IP and port
    private void randomizePort(){
        for (int portToCheck = 4000; portToCheck < 50000; portToCheck++) {
            if (isPortAvailable(portToCheck)) {
                this.port = portToCheck;
                return;
            }
        }
        throw new RuntimeException("Unable to find an available port for actuator " + this.getId());
    }
    //check if port available
    private boolean isPortAvailable(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            // Port is not available
            return false;
        }
    }*/

    /**
     * Sets the frequency of event generation
     *
     * @param frequency the frequency of event generation in Hz (1/s)
     */
    public void setEventGenerationFrequency(double frequency){
        this.eventGenerationFrequency = frequency;
    }

    public void sendEvent(Event event) {
        actuatorToServerWriter.println(event.toString());
        actuatorToServerWriter.flush();

        // note that Event is a complex object that you need to serialize before sending
    }
    /**
     * Processes a server command received as a Request and updates the actuator's state accordingly.
     *
     * @param command the Request object representing the server command
     */
    public void processServerMessage(Request command) {
        if(command.getRequestCommand() == RequestCommand.CONTROL_SET_ACTUATOR_STATE) {
            this.updateState(true);
        }
        if(command.getRequestCommand() == RequestCommand.CONTROL_TOGGLE_ACTUATOR_STATE) {
            this.updateState(!this.getState());
        }
    }

    /**
     * Constructs a new Actuator from a String.
     * May be used to reconstruct an equivalent Actuator from toString().
     * @param actuatorString a string generated by toString().
     * @return a new Actuator, equivalent to the original before being serialized.
     */
    public static Actuator parse(String actuatorString) {
        List<String> actuatorComponents = Arrays.asList(actuatorString.split(","));

        int id = Integer.parseInt(actuatorComponents.get(1));
        int clientId = Integer.parseInt(actuatorComponents.get(2));
        String type = actuatorComponents.get(3);
        String host = actuatorComponents.get(4);
        int port = Integer.parseInt(actuatorComponents.get(5));

        return new Actuator(id, clientId, type, host, port);
    }

    /**
     * Constructs a new String corresponding to the Actuator.
     * @return a new String corresponding to the Actuator.
     */
    @Override
    public String toString() {
        return "Actuator{" +
                "," + getId() +
                "," + getClientId() +
                "," + getType() +
                "," + getIP() +
                "," + getPort() +
                ",}";
    }

    /**
     * Determines whether this is equivalent to another Object.
     * @param o the Object that this is being compared to.
     * @return true if this equals o; false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Actuator) {
            Actuator other = (Actuator) o;
            return this.getId() == (other.getId());
        }
        else {
            return false;
        }

    }
}