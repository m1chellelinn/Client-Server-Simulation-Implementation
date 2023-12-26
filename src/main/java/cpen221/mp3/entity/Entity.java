package cpen221.mp3.entity;

import cpen221.mp3.event.Event;

public interface Entity {
    // returns the id of the entity
    int getId();

    // returns the client id of the entity
    int getClientId();

    // returns the type of the entity
    String getType();

    // returns true if the entity is an actuator
    boolean isActuator();

    boolean registerForClient(int clientId);

    // sets or updates the http endpoint of the entity
    void setEndpoint(String serverIP, int serverPort);

    void setEventGenerationFrequency(double frequency);

    // sends an event to the endpoint
    void sendEvent(Event event);
}