package cpen221.mp3.event;

public interface Event {
    
    // returns the timestamp of the event
    double getTimeStamp();

    // returns the client id of the event
    int getClientId();

    // returns the entity id of the event
    int getEntityId();

    // returns the entity type of the event
    String getEntityType();

    // returns the double value of the event if available
    // returns -1 if the event does not have a double value
    double getValueDouble();

    // returns the boolean value of the event if available
    // returns false if the event does not have a boolean value
    boolean getValueBoolean();
}
