package cpen221.mp3.handler;

import cpen221.mp3.event.*;
import cpen221.mp3.client.*;
import cpen221.mp3.server.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.*;

/**
 * Abstraction Function:
 *     Represents a message handler that processes incoming sockets, requests, and events for multiple clients.
 * Representation Invariant:
 *     - serverSocket is initialized and running on the specified port.
 *     - port is immutable.
 *     - socketQueue, eventQueue, requestQueue, queuedRequests are thread-safe datatypes.
 *     - servers is a ConcurrentHashMap, which is thread-safe.
 *     - clientWaitTimes is a HashMap, accessed sequentially in a single thread.
 * Thread Safety Argument:
 *     This class is threadsafe because:
 *     - serversocket is a thread-safe datatype and is only used locally
 *     - port is immutable.
 *          port may be passed to other threads, but they never modify it.
 *     - socketQueue, eventQueue, requestQueue are thread-safe datatypes.
 *     - servers is a ConcurrentHashMap, which is thread-safe.
 *          Additionally, only a single read/write happens at a time -- no iterators are used.
 *     - clientWaitTimes is a HashMap which is not thread-safe.
 *          However, it is only accessed sequentially in this one thread.
 */
public class MessageHandler {
    private ServerSocket serverSocket;
    private final int port;

    private final BlockingQueue<Socket> socketQueue;
    private final BlockingQueue<Event> eventQueue;
    private final BlockingQueue<Request> requestQueue;

    protected Map<Integer, Server> servers; //maps client ID to server
    private final Queue<Request> queuedRequests;
    private Map<Integer, Integer> clientWaitTimes; //maps client ID to their respective maxWaitTime

    private final int COMPUTATION_TIME = 100; //in milliseconds


    // you may need to add additional private fields and methods to this class
    /**
     * Constructs a new MessageHandler with the specified port.
     *
     * @param port the port number on which the server will listen for incoming connections
     */
    public MessageHandler(int port) {
        this.port = port;
        this.socketQueue = new LinkedBlockingQueue<>();
        this.eventQueue = new LinkedBlockingQueue<>();
        this.requestQueue = new LinkedBlockingQueue<>();
        this.servers = new ConcurrentHashMap<>();
        this.clientWaitTimes = new ConcurrentHashMap<>();
        //The lambda in the Queue's constructor the relative deadlines of the requests.
        // This is to make the queue sort requests by (absolute) earliest-deadline-first.
        this.queuedRequests = new PriorityQueue<>((req1, req2) ->
                (int) ( req1.getTimeStamp() * 1000 + clientWaitTimes.getOrDefault(req1.getClientId(), 2) -
                        req2.getTimeStamp() * 1000 + clientWaitTimes.getOrDefault(req2.getClientId(), 2)));
    }
    /**
     * Processes the urgent requests in the queuedRequests queue. An urgent request is one that needs
     * urgent processing based on the time elapsed since its timestamp and the configured maximum wait time
     * for the corresponding client. If an urgent request is found, it is removed from the queue, and the
     * associated server is used to process the request.
     *
     * If no urgent requests are found or the time elapsed is greater than the configured maximum wait time,
     * then no action is taken.
     *
     * This method assumes that the queuedRequests queue is sorted by the earliest deadline.
     */
    private void processUrgentRequests() {
        Request urgentRequest = queuedRequests.peek();
        long currentTime = System.currentTimeMillis();

        int maxWaitTime = clientWaitTimes.getOrDefault(urgentRequest.getClientId(), 2);

        if(currentTime - urgentRequest.getTimeStamp() >  maxWaitTime * 1000 - COMPUTATION_TIME){
            queuedRequests.poll();
            Server server = servers.get(urgentRequest.getClientId());
            server.processIncomingRequest(urgentRequest);
        }
    }
    /**
     * Initializes and starts the server socket on the specified port.
     * Creates a new thread to handle incoming sockets.
     *
     * @throws RuntimeException if an IOException occurs during the server socket initialization
     */
    private void runInstantiation () {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port + " with address " + serverSocket.getInetAddress().getHostAddress());

            Thread socketThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        while (true) {
                            Socket incomingSocket = serverSocket.accept();
                            socketQueue.put(incomingSocket);
                        }
                    } catch (Exception e) {
                        System.err.println("Error: " + e.getMessage());
                    }
                }
            });
            socketThread.start();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * Starts the message handler, initiating the server socket and handling incoming sockets, requests, and events.
     * Continuously monitors the queues for incoming data and processes them accordingly.
     *
     * This method runs indefinitely and handles the following:
     * - Incoming sockets are put into the socket queue.
     * - Incoming requests are put into the request queue.
     * - Incoming events are put into the event queue.
     * - Urgent requests are processed.
     * - The maximum wait time for a client is updated if a CONFIG_UPDATE_MAX_WAIT_TIME request is received.
     *
     * @throws InterruptedException if an interrupted exception occurs during queue operations
     * @throws RuntimeException if an exception occurs during socket or thread handling
     *
     */
    public void start() {
        runInstantiation();

        try {
            while (true) {
                if (!socketQueue.isEmpty()) {
                    Socket incomingSocket = socketQueue.poll();
                    System.out.println("Client/Entity connected: " + incomingSocket.getInetAddress().getHostAddress());
                    // create a new thread to handle the client request or entity event
                    Thread handlerThread = new Thread(new MessageHandlerThread(incomingSocket, requestQueue, eventQueue, servers));
                    handlerThread.start();
                }

                if (!requestQueue.isEmpty()) {
                    Request req = requestQueue.poll();
                    //change the maxWaitTime for this client
                    if (req.getRequestCommand() == RequestCommand.CONFIG_UPDATE_MAX_WAIT_TIME) {
                        clientWaitTimes.put(req.getClientId(), Integer.valueOf(req.getRequestData()));
                    }
                    queuedRequests.add(req);
                    System.out.println("Queued request " + req + " for processing");
                }


                if (!eventQueue.isEmpty()) {
                    Event newEvent = eventQueue.take();
                    servers.get(newEvent.getClientId()).processIncomingEvent(newEvent);
                }

                if (!queuedRequests.isEmpty()) {
                    processUrgentRequests();
                }
            }




        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        MessageHandler handler = new MessageHandler(4949);
        handler.start();
    }
}