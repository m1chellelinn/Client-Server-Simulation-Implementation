package cpen221.mp3.server;


import cpen221.mp3.client.*;
import cpen221.mp3.entity.*;
import cpen221.mp3.event.*;
import cpen221.mp3.handler.*;

import java.util.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstraction Function:
 *      Maps each object to a server that can answer to clients' requests.
 *
 * Representation Invariant:
 *      The client exists. And for each entity, it is registered to the client.
 *
 * Thread Safety Argument:
 *   This class is threadsafe because:
 *    - The ConcurrentHashMap entityEvents is used to store entities and their associated events,
 *      providing thread-safe access and modification.
 *    - The eventsInOrder list is declared as a synchronized list to ensure thread-safe operations
 *      when adding events.
 *    - The logs list is accessed and cleared in a synchronized manner in the readLogs() method.
 *    - Other data fields, such as actuatorSockets and lastFilterChange, are accessed in a
 *      thread-safe manner.
 *    - Methods that modify filters (logIf, toggleActuatorStateIf, setActuatorStateIf) update
 *      the filters in a synchronized way.
 *
 *
 */
public class Server {
    private final Client client;
    private double maxWaitTime = 2; // in seconds
    private final Map<Entity, List<Event>> entityEvents;
    private final Map<Integer, Socket> actuatorSockets; //maps actuator ID to its socket. Used for sending setState commands to actuators
    private final List<Event> eventsInOrder;
    private final List<Integer> logs;

    private Filter logFilter;
    private Filter toggleFilter;
    private Filter setFilter;
    private long lastFilterChange;

    private PrintWriter serverToClientWriter;

    // you may need to add additional private fields

    public Server(Client client) {
        this.client = client;
        this.entityEvents = new ConcurrentHashMap<>();
        this.eventsInOrder = Collections.synchronizedList(new ArrayList<>());
        this.logs = new ArrayList<>();
        this.actuatorSockets = new ConcurrentHashMap<>();

        logFilter = new Filter("timestamp", DoubleOperator.EQUALS, -1); //start with an unsatisfiable filter
        toggleFilter = new Filter("timestamp", DoubleOperator.EQUALS, -1);
        setFilter = new Filter("timestamp", DoubleOperator.EQUALS, -1);

        lastFilterChange = System.currentTimeMillis();

        //TODO: somehow find a way to communicate to actuators even without a messagehandler
        // nvm that was fast, we get pointers to actuators in set/toggleActuatorIf
    }

    public void setPrintWriter(PrintWriter out){
        this.serverToClientWriter = out;
    }

    public void addActuator(Actuator actuator) {
        try {
            System.out.println("Server attempt to connect to actuator " + actuator.getId() + " at " + actuator.getIP() + ":" + actuator.getPort());
            Socket serverToActuatorSocket = new Socket(actuator.getIP(), actuator.getPort());
            actuatorSockets.put(actuator.getId(), serverToActuatorSocket);
        } catch (IOException e) {
            throw new RuntimeException(e + " actuator " + actuator.getId() + " refused to let server connect.");
        }

    }



    /**
     * Update the max wait time for the client.
     * The max wait time is the maximum amount of time
     * that the server can wait for before starting to process each event of the client:
     * It is the difference between the time the message was received on the server
     * (not the event timeStamp from above) and the time it started to be processed.
     *
     * @param maxWaitTime the new max wait time
     */
    public synchronized void updateMaxWaitTime(double maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
    }

    /**
     * Set the actuator state if the given filter is satisfied by the latest event.
     * Here the latest event is the event with the latest timestamp not the event 
     * that was received by the server the latest.
     *
     * If the actuator is not registered for the client, then this method should do nothing.
     * 
     * @param filter the filter to check
     * @param actuator the actuator to set the state of as true
     */
    public void setActuatorStateIf(Filter filter, Actuator actuator) {
        this.setFilter = filter;

        if(actuator.getClientId() == client.getClientId() &&
           filter.satisfies(eventsInOrder.get(eventsInOrder.size() - 1))) {
            actuator.updateState(true);
        } else {
            actuator.updateState(false);
        }
        synchronized (entityEvents){
            for (Entity entity : entityEvents.keySet()) {
                if (entity.getId() == actuator.getId()) {
                    ((Actuator) entity).updateState(actuator.getState());
                    break;
                }
            }
        }

        if (actuatorSockets.containsKey(actuator.getId())) {
            try {
                PrintWriter oneTimeWriter = new PrintWriter(new OutputStreamWriter(actuatorSockets.get(actuator.getId()).getOutputStream()));
                oneTimeWriter.println(new Request(RequestType.CONTROL, RequestCommand.CONTROL_SET_ACTUATOR_STATE, (actuator.getState() ? "True" : "False")));
                oneTimeWriter.flush();
            } catch (IOException e) {
                throw new RuntimeException(e + " actuator " + actuator.getId() + " refused to give server the PrintWriter.");
            }
        }

    }
    
    /**
     * Toggle the actuator state if the given filter is satisfied by the latest event.
     * Here the latest event is the event with the latest timestamp not the event 
     * that was received by the server the latest.
     * 
     * If the actuator has never sent an event to the server, then this method should do nothing.
     * If the actuator is not registered for the client, then this method should do nothing.
     *
     * @param filter the filter to check
     * @param actuator the actuator to toggle the state of (true -> false, false -> true)
     */
    public void toggleActuatorStateIf(Filter filter, Actuator actuator) {
        this.toggleFilter = filter;

        if(actuator.getClientId() == client.getClientId() &&
           filter.satisfies(eventsInOrder.get(eventsInOrder.size() - 1))) {

            synchronized (entityEvents){
                for (Entity entity : entityEvents.keySet()) {
                    if (entity.getId() == actuator.getId()) {
                        ((Actuator) entity).updateState(!((Actuator) entity).getState());
                        actuator.updateState(((Actuator) entity).getState());
                        break;
                    }
                }
            }

            if (actuatorSockets.containsKey(actuator.getId())) {
                try {
                    PrintWriter oneTimeWriter = new PrintWriter(new OutputStreamWriter(actuatorSockets.get(actuator.getId()).getOutputStream()));
                    oneTimeWriter.println(new Request(RequestType.CONTROL, RequestCommand.CONTROL_SET_ACTUATOR_STATE, (actuator.getState() ? "True" : "False")));
                    oneTimeWriter.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e + " actuator " + actuator.getId() + " refused to give server the PrintWriter.");
                }
            }
        }
    }

    /**
     * Log the event ID for which a given filter was satisfied.
     * This method is checked for every event received by the server.
     *
     * @param filter the filter to check
     */
    public void logIf(Filter filter) {
        this.logFilter = filter;
        this.lastFilterChange = System.currentTimeMillis();
        this.logs.clear();

        for(Event event : eventsInOrder) {
            if(event.getTimeStamp() >= lastFilterChange &&
               filter.satisfies(event)) {
                logs.add(event.getEntityId());
            }
        }
    }

    /**
     * Return all the logs made by the "logIf" method so far.
     * If no logs have been made, then this method should return an empty list.
     * The list should be sorted in the order of event timestamps.
     * After the logs are read, they should be cleared from the server.
     *
     * @return list of event IDs
     */
    public synchronized List<Integer> readLogs() {
        List<Integer> currentLogs = new ArrayList<>(logs);
        logs.clear();

        return currentLogs;
    }

    /**
     * List all the events of the client that occurred in the given time window.
     * Here the timestamp of an event is the time at which the event occurred, not 
     * the time at which the event was received by the server.
     * If no events occurred in the given time window, then this method should return an empty list.
     *
     * @param timeWindow the time window of events, inclusive of the start and end times
     * @return list of the events for the client in the given time window
     */
    public List<Event> eventsInTimeWindow(TimeWindow timeWindow) {
        List<Event> eventsInWindow = new ArrayList<>();

        for (Event event : eventsInOrder){
            double eventTimestamp = event.getTimeStamp();
            if (eventTimestamp >= timeWindow.getStartTime() && eventTimestamp <= timeWindow.getEndTime()) {
                eventsInWindow.add(event);
            }
        }
        eventsInWindow.sort(Comparator.comparingDouble(Event::getTimeStamp));
        return eventsInWindow;
    }

     /**
     * Returns a set of IDs for all the entities of the client for which 
     * we have received events so far.
     * Returns an empty list if no events have been received for the client.
     * 
     * @return list of all the entities of the client for which we have received events so far
     */
    public List<Integer> getAllEntities() {
        List<Integer> entitiesID = new ArrayList<>();
        for(Entity en: entityEvents.keySet()){
            entitiesID.add(en.getId());
        }
        return entitiesID;
    }

    /**
     * List the latest n events of the client.
     * Here the order is based on the original timestamp of the events, not the time at which the events were received by the server.
     * If the client has fewer than n events, then this method should return all the events of the client.
     * If no events exist for the client, then this method should return an empty list.
     * If there are multiple events with the same timestamp in the boundary,
     * the ones with largest EntityId should be included in the list.
     *
     * @param n the max number of events to list
     * @return list of the latest n events of the client
     */
    public List<Event> lastNEvents(int n) {
        if(n > eventsInOrder.size()){
            List<Event> newResult = new ArrayList<>(eventsInOrder);
            newResult.sort(Comparator.comparingDouble(Event::getTimeStamp));
            return newResult;
        }
        List<Event> result = new ArrayList<>();
        for (int i = eventsInOrder.size() - 1; i >= 0 && n > 0; i--) {
            Event currentEvent = eventsInOrder.get(i);
            result.add(currentEvent);
            n--;
        }

        result.sort(Comparator.comparingDouble(Event::getTimeStamp));
        return result;
    }

    /**
     * returns the ID corresponding to the most active entity of the client
     * in terms of the number of events it has generated.
     *
     * If there was a tie, then this method should return the largest ID.
     * 
     * @return the most active entity ID of the client
     */
    public int mostActiveEntity() {

        int maxEventCount = 0;
        int mostActiveEntityId = -1;


        synchronized (entityEvents){
            for (Entity en : entityEvents.keySet()) {
                int eventCount = entityEvents.get(en).size();

                if (eventCount > maxEventCount || (eventCount == maxEventCount && en.getId() > mostActiveEntityId)) {
                    maxEventCount = eventCount;
                    mostActiveEntityId = en.getId();
                }
            }
        }

        return mostActiveEntityId;
    }

    /**
     * the client can ask the server to predict what will be 
     * the next n timestamps for the next n events 
     * of the given entity of the client (the entity is identified by its ID).
     * 
     * If the server has not received any events for an entity with that ID,
     * or if that Entity is not registered for the client, then this method should return an empty list.
     * 
     * @param entityId the ID of the entity
     * @param n the number of timestamps to predict
     * @return list of the predicted timestamps
     */
    public List<Double> predictNextNTimeStamps(int entityId, int n) {

        return null;
    }

    /**
     * the client can ask the server to predict what will be 
     * the next n values of the timestamps for the next n events
     * of the given entity of the client (the entity is identified by its ID).
     * The values correspond to Event.getValueDouble() or Event.getValueBoolean() 
     * based on the type of the entity. That is why the return type is List<Object>.
     * 
     * If the server has not received any events for an entity with that ID,
     * or if that Entity is not registered for the client, then this method should return an empty list.
     * 
     * @param entityId the ID of the entity
     * @param n the number of double value to predict
     * @return list of the predicted values
     */
    public List<Object> predictNextNValues(int entityId, int n) {
        synchronized(entityEvents) {
            for (Entity entity : entityEvents.keySet()) {
                if (entity.getId() == entityId) {

                }
            }
        }
        return null;
    }


    public synchronized void processIncomingEvent(Event event) {
        System.out.println("Event received: " + event);
        if (event == null) {
            return;
        }

        if (eventsInOrder.isEmpty()) {
            eventsInOrder.add(event);
        }
        else {
            for (int i = eventsInOrder.size() - 1; i >= 0; i--) {
                Event currentEvent = eventsInOrder.get(i);
                if (currentEvent.getTimeStamp() < event.getTimeStamp()) {
                    eventsInOrder.add(++i, event);
                    break;
                }
            }
        }

        boolean ifEntityExists = false;
        synchronized (entityEvents){
            for (Entity entity : entityEvents.keySet()) {
                //If we already know this entity exists, we just add the event to its list
                if (entity.getId() == event.getEntityId()) {
                    entityEvents.get(entity).add(event);
                    ifEntityExists = true;

                    //If the existing entity is an actuator, we should update its value too
                    if (entity instanceof Actuator) {
                        ((Actuator) entity).updateState(event.getValueBoolean());
                    }
                    break;
                }
            }
        }
        //If we don't know the entity exists, we have to create a new entity and add it to the map
        if (!ifEntityExists) {
            List<Event> newEventList = new ArrayList<>();
            newEventList.add(event);
            //Depending on the type of event, we create either Sensor or Actuator
            if (event instanceof ActuatorEvent) {
                entityEvents.put(new Actuator(event.getEntityId(), event.getEntityType(), event.getValueBoolean()), newEventList);
            }
            else if (event instanceof SensorEvent) {
                entityEvents.put(new Sensor(event.getEntityId(), event.getEntityType()), newEventList);
            }
        }

    }

    public synchronized void processIncomingRequest(Request request) {
        System.out.println("Request received: " + request);
        String serverResponse = ""; //This is the string response that we will send back to the client

        //This following switch statement is really long.
        // Feel free to collapse it if you want to get a better picture of our method.
        switch (request.getRequestCommand()) {
            case CONFIG_UPDATE_MAX_WAIT_TIME:
                updateMaxWaitTime(Double.valueOf(request.getRequestData()));
                break;

            case CONTROL_SET_ACTUATOR_STATE:
                String[] requestData = request.getRequestData().split("#");
                int actuatorId = Integer.parseInt(requestData[0]);
                Filter conditionFilter = Filter.parse(requestData[1]);

                Actuator actuatorToSet = null;

                synchronized (entityEvents){
                    for (Entity entity : entityEvents.keySet()) {
                        if (entity.getId() == actuatorId) {
                            actuatorToSet = (Actuator) entity;
                            break;
                        }
                    }
                }
                if (actuatorToSet != null) {
                    setActuatorStateIf(conditionFilter, actuatorToSet);
                }
                break;

            case CONTROL_TOGGLE_ACTUATOR_STATE:
                String[] toggleActuatorData = request.getRequestData().split("#");
                int toggleActuatorId = Integer.parseInt(toggleActuatorData[0]);
                Filter toggleConditionFilter = Filter.parse(toggleActuatorData[1]);

                Actuator actuatorToToggle = null;
                synchronized (entityEvents){
                    for (Entity entity : entityEvents.keySet()) {
                        if (entity.getId() == toggleActuatorId) {
                            actuatorToToggle = (Actuator) entity;
                            break;
                        }
                    }
                }
                if (actuatorToToggle != null) {
                    setActuatorStateIf(toggleConditionFilter, actuatorToToggle);
                }
                break;

            case CONTROL_NOTIFY_IF:
                String filterString = request.getRequestData();
                Filter filter = Filter.parse(filterString);

                if (filter != null) {
                    logIf(filter);
                }
                break;

            case ANALYSIS_GET_EVENTS_IN_WINDOW:
                String timeWindowString = request.getRequestData();
                TimeWindow timeWindow = TimeWindow.parse(timeWindowString);

                List<Event> events = eventsInTimeWindow(timeWindow);

                serverResponse += "These entities sent events to the server within the timewindow";
                for (Event event : events) {
                    serverResponse += ", " + event.getEntityId();
                }
                break;

            case ANALYSIS_GET_ALL_ENTITIES:
                serverResponse += "These are all the entities";
                for (Integer entity : getAllEntities()) {
                    serverResponse += ", " + entity;
                }
                break;

            case ANALYSIS_GET_LATEST_EVENTS:
                String nString = request.getRequestData();
                int n;
                try {
                    n = Integer.parseInt(nString);
                } catch (NumberFormatException e) {
                    break;
                }
                List<Event> latestEvents = lastNEvents(n);
                serverResponse += "These are the latest " + n + "events";
                for (Event event : latestEvents) {
                    serverResponse += (", " + event.toString());
                }
                break;

            case ANALYSIS_GET_MOST_ACTIVE_ENTITY:
                serverResponse += "The most active entity was: ";
                int mostActiveId = mostActiveEntity();
               serverResponse += mostActiveId;
                break;

            case PREDICT_NEXT_N_TIMESTAMPS:
                break;
            case PREDICT_NEXT_N_VALUES:
                break;
        }

        if (serverToClientWriter != null){
            serverToClientWriter.println(serverResponse);
            serverToClientWriter.flush();
        }
    }
/*
    private void runInstantiation (Socket socket) {
        try {
            this.serverToClient = socket;
            System.out.println("Server started on port " + port);

            Thread socketThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        while (true) {
                            Socket incomingSocket = serverSocket.accept();
                            System.out.println("Client/Entity connected: " + incomingSocket.getInetAddress().getHostAddress());

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
    }*/
}
