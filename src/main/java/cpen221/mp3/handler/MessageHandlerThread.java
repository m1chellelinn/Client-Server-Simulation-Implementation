package cpen221.mp3.handler;

import cpen221.mp3.entity.*;
import cpen221.mp3.event.*;
import cpen221.mp3.client.*;
import cpen221.mp3.server.Server;

import java.net.Socket;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Abstraction Function:
 *     Represents a thread responsible for handling incoming messages from a client or entity.
 *     Processes requests, actuators, actuator events, and sensor events from the incoming socket.
 * Representation Invariant:
 *     - incomingSocket is final and contains a thread-safe type (Socket).
 *     - requestQueue, eventQueue are final and contain thread-safe types (BlockingQueue).
 *     - servers is final and contains a thread-safe type (ConcurrentHashMap).
 *     - in, out are only modified in the local thread.
 * Thread Safety Argument:
 *    This class is threadsafe because:
 *    - incomingSocket is final and contains a thread-safe type
 *      It may be shared with other threads, but it is not modified.
 *    - requestQueue, eventQueue, servers are final and contain thread-safe types.
 *      They may be shared with other threads, but only a single read/write happen at a time -- no iterators are used.
 *    - in, out are only modified in the local thread.
 */
class MessageHandlerThread implements Runnable {
    private final Socket incomingSocket;
    private BufferedReader in;
    private PrintWriter out;
    private final BlockingQueue<Request> requestQueue;
    private final BlockingQueue<Event> eventQueue;

    private final Map<Integer, Server> servers;

    /**
     * Constructs a new MessageHandlerThread with the specified parameters.
     *
     * @param incomingSocket the socket for communication with the client or entity
     * @param requestQueue the queue for incoming requests
     * @param eventQueue the queue for incoming events
     * @param Servers the map containing server instances for each client
     */
    public MessageHandlerThread(Socket incomingSocket, BlockingQueue<Request> requestQueue, BlockingQueue<Event> eventQueue, Map<Integer, Server> Servers) {
        this.incomingSocket = incomingSocket;
        this.requestQueue = requestQueue;
        this.eventQueue = eventQueue;
        this.servers = Servers;
    }

    /**
     * Reads the first message from the incoming socket and processes it.
     * Determines the type of the entity (Request, Actuator, ActuatorEvent, SensorEvent),
     * creates or updates the corresponding server instance, and adds the entity to the appropriate queue.
     *
     * @throws RuntimeException if an IOException occurs during the read operation
     * @throws RuntimeException if an InterruptedException occurs while putting entities into queues
     */
    private void readFirstMessage() {
        String messageString;
        //Reads the first message from anywhere
        while (true) {
            try {
                messageString = in.readLine();
                if (messageString != null) {
                    break;
                }
            } catch (IOException e) {
                throw new RuntimeException(e + "... reading first line failed");
            }
        }
        try {
            if (messageString.startsWith("Request{")) {
                Request request = Request.parse(messageString);
                if (!servers.containsKey(request.getClientId())) {
                    Server newServer = new Server(new Client(request.getClientId()));

                    servers.put(request.getClientId(), newServer);
                }
                servers.get(request.getClientId()).setPrintWriter(out);
                requestQueue.put(request);

            } else if (messageString.startsWith("Actuator{")) {
                Actuator actuator = Actuator.parse(messageString);

                if (!servers.containsKey(actuator.getClientId())) {
                    servers.put(actuator.getClientId(), new Server(new Client(actuator.getClientId())));

                }
                servers.get(actuator.getClientId()).addActuator(actuator);

            } else if (messageString.startsWith("ActuatorEvent{")) {
                Event event = ActuatorEvent.parse(messageString);

                if (!servers.containsKey(event.getClientId())) {
                    servers.put(event.getClientId(), new Server(new Client(event.getClientId())));
                }
                eventQueue.put(event);

            } else if (messageString.startsWith("SensorEvent{")) {
                Event event = SensorEvent.parse(messageString);

                if (!servers.containsKey(event.getClientId())) {
                    servers.put(event.getClientId(), new Server(new Client(event.getClientId())));
                }
                eventQueue.put(event);
            }

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }



    }
    /**
     * The main execution loop for the MessageHandlerThread.
     * Reads and processes incoming messages from the client or entity.
     * Continuously monitors the incoming socket for new messages and processes them accordingly.
     *
     * @throws RuntimeException if an IOException occurs during socket or thread handling
     * @throws RuntimeException if an InterruptedException occurs during queue operations
     */
    @Override
    public void run() {
        try {
            this.in = new BufferedReader(new InputStreamReader(incomingSocket.getInputStream()));
            this.out = new PrintWriter(new OutputStreamWriter(incomingSocket.getOutputStream()));
            //Reads and process the first message, because the first one will determine
            // if we need to create new servers or not.
            // Note that in our implementation, servers are created in these Threads and then
            // passed to the main MessageHandler via a concurrent-safe map.
            readFirstMessage();

            while (true){
                String messageString = in.readLine();
                if (messageString != null) {
                    if (messageString.contains("Request{")) {
                        requestQueue.put(Request.parse(messageString));
                    } else if (messageString.startsWith("ActuatorEvent{")) {
                        eventQueue.put(ActuatorEvent.parse(messageString));
                    } else if (messageString.startsWith("SensorEvent{")) {
                        eventQueue.put(SensorEvent.parse(messageString));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e + "... MessageHandlerThread failed... somehow");
        }
    }
}