package cpen221.mp3.event;

import java.util.Arrays;
import java.util.List;

/**
 * Abstraction Function:
 *     Represents a sensor event with timestamp, client ID, entity ID, entity type, and double value.
 * Representation Invariant:
 *     - All fields (TimeStamp, ClientId, EntityId, EntityType, Value) are private and final.
 *     - The data they contain are also immutable types (double, int, and String)
 * Thread Safety Argument:
 *     This class is threadsafe because it is immutable:
 *     - all fields are private and final. The data they contain are also immutable types.
 */
public class SensorEvent implements Event {
    // you can add private fields and methods to this class
    private final double TimeStamp;
    private final int ClientId;
    private final int EntityId;
    private final String EntityType;
    private final double Value;


    public SensorEvent(double TimeStamp,
                        int ClientId,
                        int EntityId, 
                        String EntityType, 
                        double Value) {
        this.TimeStamp = TimeStamp;
        this.ClientId = ClientId;
        this.EntityId = EntityId;
        this.EntityType = EntityType;
        this.Value = Value;
    }
    // returns the timestamp of the event
    public double getTimeStamp() {
        return TimeStamp;
    }
    // returns the client id of the event
    public int getClientId() {
        return ClientId;
    }
    // returns the entity id of the event
    public int getEntityId() {
        return EntityId;
    }
    // returns the entity type of the event
    public String getEntityType() {
        return EntityType;
    }
    // returns the double value of the event if available
    // returns -1 if the event does not have a double value
    public double getValueDouble() {
        return Value;
    }

    // Sensor events do not have a boolean value
    // returns the boolean value of the event if available
    // returns false if the event does not have a boolean value
    public boolean getValueBoolean() {
        return false;
    }

    /**
     * Constructs a new SensorEvent from a String.
     * May be used to reconstruct an SensorEvent from toString().
     * @param eventString a string generated by toString().
     * @return a new SensorEvent, equivalent to the original before being serialized.
     */
    public static SensorEvent parse(String eventString) {
        List<String> eventComponents = Arrays.asList(eventString.split(","));

        String timeStampString = eventComponents.get(1);
        double timeStamp = Double.parseDouble(timeStampString);

        String clientIdString = eventComponents.get(2);
        int clientId = Integer.parseInt(clientIdString);

        String entityIdString = eventComponents.get(3);
        int entityId = Integer.parseInt(entityIdString);

        String entityType = eventComponents.get(4);

        String valueDoubleString = eventComponents.get(5);
        double valueDouble = Double.parseDouble(valueDoubleString);

        return new SensorEvent(timeStamp, clientId, entityId, entityType, valueDouble);
    }

    /**
     * Constructs a new String corresponding to the SensorEvent.
     * @return a new String corresponding to the SensorEvent.
     */
    @Override
    public String toString() {
        //Default implementation: default words like "TimeStamp="
        // might make it harder to parse event on the other end

        /*return "ActuatorEvent{" +
                "TimeStamp=" + getTimeStamp() +
                ",ClientId=" + getClientId() +
                ",EntityId=" + getEntityId() +
                ",EntityType=" + getEntityType() +
                ",Value=" + getValueDouble() +
                '}';*/

        //I think we can try this instead: every field ends in a comma.
        // maybe we even have abbreviations for "ActuatorEvent"
        return "SensorEvent{" +
                "," + getTimeStamp() +
                "," + getClientId() +
                "," + getEntityId() +
                "," + getEntityType() +
                "," + getValueDouble() +
                "," + '}';
    }

    /**
     * Determines whether this is equivalent to another Object.
     * @param o the Object that this is being compared to.
     * @return true if this equals other; false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof SensorEvent) {
            if (o == this) {
                return true;
            }
            SensorEvent other = (SensorEvent) o;
            return this.TimeStamp == other.TimeStamp
                    && this.ClientId == other.ClientId
                    && this.EntityId == other.EntityId
                    && this.EntityType.equals(other.EntityType)
                    && this.Value == other.Value;
        }
        return false;
    }
}