package cpen221.mp3.client;

import cpen221.mp3.entity.Entity;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Abstraction Function:
 *     Represents a client with a unique identifier, email address, server IP, and server port.
 *
 * Representation Invariant:
 *     - clientId and serverPort are immutable types.
 *     - Pointers to email and serverIP are used locally only.
 *     - clientToServerSocket, clientToServerWriter, serverToClientReader contain thread-safe datatypes in the Java library and are used locally.
 *
 * Thread Safety Argument:
 *      This class is threadsafe because:
 *      - clientId, serverPort are immutable types
 *      - pointers to email, serverIP are used locally only
 *      - clientToServerSocket, clientToServerWriter, serverToClientReader contain thread-safe datatypes in the Java library
 *        and only used locally
 */
public class Client implements Runnable{

    private final int clientId;
    private String email;
    private String serverIP;
    private int serverPort;

    private Socket clientToServerSocket;
    private PrintWriter clientToServerWriter;
    private BufferedReader serverToClientReader;

    private double lastRequestSend = 0;

    // you would need additional fields to enable functionalities required for this class

    /**
     * Make a new client with a given id and email address.
     *
     * @param clientId the id of the client. Must be unique among all clients.
     * @param email the email address of the client.
     * @param serverIP the IP address of the server.
     *                 Requires: serverIP is a valid IP and isn't null.
     * @param serverPort the port number of the server.
     */
    public Client(int clientId, String email, String serverIP, int serverPort) {
        this.clientId = clientId;
        this.email = email;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.clientToServerSocket = new Socket();

        Thread clientThread = new Thread(this);
        clientThread.start();
    }

    /**
     * Make a new client with a given id. This version of the Client constructor does not initiate a Thread.
     *      This constructor is only used by our server end to create "images" of clients
     *
     * @param clientId the id of the client. Must be unique among all clients.
     */
    public Client(int clientId) {
        this.clientId = clientId;
        this.email = "lol@gmail.com";
        this.serverIP = "127.0.0.1";
        this.serverPort = 4949;
    }
    /**
     * The main execution loop for the Client.
     * Connects to the server, and continuously waits for and prints responses from the server.
     * Uses a separate thread for the client-to-server connection.
     */
    public void run() {
        //Infinite loop, loop until socket connects
        this.instantiateSocket();

        //TODO: After connecting, start waiting for server responses
        while(true) {
            String newResponse = null;
            try {
                while (newResponse == null) {
                    newResponse = serverToClientReader.readLine();
                }
            } catch (IOException e) {
                throw new RuntimeException(e + "... reading from server failed");
            }

            System.out.println("Response from server for client " + clientId + ": " + newResponse + "\n   - Received in about " + (System.currentTimeMillis() - lastRequestSend) + "ms (round trip)");
        }

    }
    /**
     * Instantiates the socket for communication from the client to the server.
     * Creates a new Socket and connects to the specified server IP and port.
     * Initializes the output stream for sending requests to the server and the input stream for receiving responses.
     *
     * @throws RuntimeException if the instantiation of the client-to-server connection or input/output streams fails
     */
    private void instantiateSocket() {
        clientToServerSocket = new Socket();
        while (!clientToServerSocket.isConnected()) {
            try {
                this.clientToServerSocket = new Socket(serverIP, serverPort);
            } catch (IOException e) {
                //Do nothing
            }
        }
        //TODO: instantiate a serverToClientPrinter here to listen for responses
        try {
            this.clientToServerWriter = new PrintWriter(new OutputStreamWriter(clientToServerSocket.getOutputStream()));
            this.serverToClientReader = new BufferedReader(new InputStreamReader(clientToServerSocket.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e + "... client to server bytestream instantiation failed");
        }
    }
    /**
     * Returns the unique identifier of the client.
     *
     * @return the client's identifier
     */
    public int getClientId() {
        return clientId;
    }

    /**
     * Registers an entity for the client
     *
     * @return true if the entity is new and gets successfully registered, false if the Entity is already registered
     */
    public boolean addEntity(Entity entity) {
        return entity.registerForClient(clientId);
    } //FIXME: client needs to keep track of these entities probably

    /**
     * Sends a request to the server
     *
     * @param request the request to be sent
     */
    public void sendRequest(Request request) {
        Request properFormattedRequest = new Request(clientId, request.getTimeStamp(),
                request.getRequestType(), request.getRequestCommand(), request.getRequestData());

        if (clientToServerWriter != null){
            clientToServerWriter.println(properFormattedRequest);
            clientToServerWriter.flush();
        }
        System.out.println("Request sent by client " + clientId + ": " + request);

        lastRequestSend = System.currentTimeMillis();
    }

}