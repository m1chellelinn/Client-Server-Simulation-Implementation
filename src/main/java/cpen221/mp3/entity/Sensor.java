package cpen221.mp3.entity;

import cpen221.mp3.event.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.net.*;

/**
 * Abstraction Function:
 *     Represents a sensor with a unique identifier, client ID, type, and the ability to send events to a server.
 *
 * Representation Invariant:
 *     - id, clientId, type are immutable.
 *     - serverIP, serverPort are only modified in synchronized methods.
 *     - sensorToServerSocket, sensorToServerWriter contain thread-safe datatypes and are encapsulated to this object, not shared with other threads.
 *
 * Thread Safety Argument:
 *      This class is threadsafe because:
 *      - id, clientId, type are immutable
 *      - serverIP, serverPort are only modified in synchronized methods
 *      - sensorToServerSocket, sensorToServerWriter contain thread-safe datatypes
 *        and are encapsulated to this object, not shared with other threads.
 */
public class Sensor implements Entity, Runnable {
    private final int id;
    private int clientId;
    private final String type;
    private double eventGenerationFrequency = 0.2; // default value in Hz (1/s)

    // the following specifies the http endpoint that the sensor should send events to
    private String serverIP = null;
    private int serverPort = 0;
    private Socket sensorToServerSocket;
    //This will be our output stream to server
    private PrintWriter sensorToServerWriter;
    /**
     * Constructs a new Sensor with the given ID and type.
     * The sensor is unregistered for any client and does not have a specified server endpoint.
     *
     * @param id the unique identifier of the sensor
     * @param type the type of the sensor
     */
    public Sensor(int id, String type) {
        this.id = id;
        this.clientId = -1;         // remains unregistered
        this.type = type;

        /*Thread sensorThread = new Thread(this);
        sensorThread.start();*/
    }
    /**
     * Constructs a new Sensor with the given ID, client ID, and type.
     * The sensor is registered for the specified client and does not have a specified server endpoint.
     *
     * @param id the unique identifier of the sensor
     * @param clientId the client ID to which the sensor is registered
     * @param type the type of the sensor
     */
    public Sensor(int id, int clientId, String type) {
        this.id = id;
        this.clientId = clientId;   // registered for the client
        this.type = type;

        /*Thread sensorThread = new Thread(this);
        sensorThread.start();*/
    }
    /**
     * Constructs a new Sensor with the given ID, type, and server endpoint.
     * The sensor is unregistered for any client.
     *
     * @param id the unique identifier of the sensor
     * @param type the type of the sensor
     * @param serverIP the IP address of the server endpoint
     * @param serverPort the port number of the server endpoint
     */
    public Sensor(int id, String type, String serverIP, int serverPort) {
        this.id = id;
        this.clientId = -1;   // remains unregistered
        this.type = type;
        this.serverIP = serverIP;
        this.serverPort = serverPort;

        Thread sensorThread = new Thread(this);
        sensorThread.start();
    }
    /**
     * Constructs a new Sensor with the given ID, client ID, type, and server endpoint.
     * The sensor is registered for the specified client.
     *
     * @param id the unique identifier of the sensor
     * @param clientId the client ID to which the sensor is registered
     * @param type the type of the sensor
     * @param serverIP the IP address of the server endpoint
     * @param serverPort the port number of the server endpoint
     */
    public Sensor(int id, int clientId, String type, String serverIP, int serverPort) {
        this.id = id;
        this.clientId = clientId;   // registered for the client
        this.type = type;
        this.serverIP = serverIP;
        this.serverPort = serverPort;

        Thread sensorThread = new Thread(this);
        sensorThread.start();
    }
    /**
     * The main execution loop for the Sensor.
     * Connects to the server, registers itself, and continuously sends events to the server.
     * Uses separate threads for the sensor-to-server connection.
     */
    public void run() {
        this.instantiateSocket();
    }
    /**
     * Instantiates the socket for communication from the sensor to the server.
     * Creates a new Socket and connects to the specified server IP and port.
     * Initializes the output stream for sending events to the server.
     *
     * @throws RuntimeException if the instantiation of the sensor-to-server connection or output stream fails
     */
    private void instantiateSocket() {
        sensorToServerSocket = new Socket();
        while(!sensorToServerSocket.isConnected()) {
            try {
                //Instantiate the sockets objects, we only need the direction from sensor to server
                this.sensorToServerSocket = new Socket(serverIP, serverPort);
            }
            catch (IOException e) {
                //Do nothing
            }
        }
        //Since we're only sending stuff to server (and not receiving anything back),
        // we only instantiate a PrintWriter
        try {
            this.sensorToServerWriter = new PrintWriter(sensorToServerSocket.getOutputStream()); // <-- guys this PrintWriter constructor has an additional "autoFlush" field. might b useful
        } catch (IOException e) {
            throw new RuntimeException(e + "... sensor to server bytestream instantiation failed");
        }

        //This next infinite loop generates random events and sends them to server
        Random random = new Random();
        while (true) {
            try {
                Thread.sleep((long) (1000 / eventGenerationFrequency));
            } catch (InterruptedException e) {
                throw new RuntimeException(e + "... sensor thread sleep failed");
            }
            int randomValue = 0;
            switch (this.type) {
                case "TempSensor" : randomValue = random.nextInt(5) + 20;
                    break;
                case "PressureSensor" : randomValue = random.nextInt(5) + 1020;
                    break;
                case "CO2Sensor" : randomValue = random.nextInt(51) + 400;
                    break;
                default : randomValue = random.nextInt(100);
            }
            Event event = new SensorEvent(System.currentTimeMillis(), this.clientId, this.id, this.type, randomValue);
            sendEvent(event);
        }
    }
    /**
     * Returns the unique identifier of the sensor.
     *
     * @return the sensor's identifier
     */
    public int getId() {
        return id;
    }
    /**
     * Returns the client identifier to which the sensor is registered.
     *
     * @return the client identifier of the sensor
     */
    public int getClientId() {
        return clientId;
    }
    /**
     * Returns the type of the sensor.
     *
     * @return the sensor's type
     */
    public String getType() {
        return type;
    }
    /**
     * Returns whether the entity is an actuator. For sensors, this method always returns false.
     *
     * @return false, indicating that the entity is not an actuator
     */
    public boolean isActuator() {
        return false;
    }

    /**
     * Registers the sensor for the given client
     *
     * @return true if the sensor is new (clientID is -1 already) and gets successfully registered or if it is already registered for clientId, else false
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
     * the sensor should send events to
     *
     * @param serverIP the IP address of the endpoint
     * @param serverPort the port number of the endpoint
     */
    public synchronized void setEndpoint(String serverIP, int serverPort){
        this.serverIP = serverIP;
        this.serverPort = serverPort;

        Thread sensorThread = new Thread(this);
        sensorThread.start();
    }

    /**
     * Sets the frequency of event generation
     *
     * @param frequency the frequency of event generation in Hz (1/s)
     */
    public void setEventGenerationFrequency(double frequency){
        this.eventGenerationFrequency = frequency;
    }
    /**
     * Sends the given event to the server by writing its string representation to the output stream.
     *
     * @param event the event to be sent to the server
     */
    public void sendEvent(Event event) {
        sensorToServerWriter.println(event.toString());
        sensorToServerWriter.flush();
        // note that Event is a complex object that you need to serialize before sending
    }

    /**
     * Constructs a new String corresponding to the Sensor.
     * @return a new String corresponding to the Sensor.
     */
    @Override
    public String toString() {
        return "Sensor{" +
                "," + getId() +
                "," + getClientId() +
                "," + getType() +
                ",}";
    }

    /**
     * Determines whether this is equivalent to another Object.
     * @param o the Object that this is being compared to.
     * @return true if this equals o; false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Sensor) {
            Sensor other = (Sensor) o;
            return this.getId() == (other.getId());
        }
        else {
            return false;
        }

    }
}