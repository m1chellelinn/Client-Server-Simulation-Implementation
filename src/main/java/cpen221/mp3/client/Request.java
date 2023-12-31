package cpen221.mp3.client;

import cpen221.mp3.event.ActuatorEvent;

import java.util.List;
import java.util.Arrays;

/**
 * Abstraction Function:
 *     Represents a request with a unique identifier, timestamp, request type, command, data, and client ID.
 *
 * Representation Invariant:
 *     - All fields are private and final. The data they contain are also immutable types.
 *
 * Thread Safety Argument:
 *     This class is threadsafe because it is immutable:
 *     - all fields are private and final. The data they contain are also immutable types.
 */
public class Request {
    private final double timeStamp;
    private final RequestType requestType;
    private final RequestCommand requestCommand;
    private final String requestData;
    private final int clientId;

    /**
     * Constructs a new Request.
     * @param requestType the requestType of the new Request.
     * @param requestCommand the requestCommand of the new Request.
     * @param requestData the requestData of the new Request.
     *                    Should be the toString() form of the relevant data.
     *                    For set/toggle actuator state, it should be formatted "[clientId]#[Filter]"
     */
    public Request(RequestType requestType, RequestCommand requestCommand, String requestData) {
        this.timeStamp = System.currentTimeMillis();
        this.requestType = requestType;
        this.requestCommand = requestCommand;
        this.requestData = requestData;
        this.clientId = -1;
    }

    /**
     * Constructs a new Request.
     * @param clientId the clientId of the new Request.
     * @param timeStamp the timeStamp of the new Request.
     * @param requestType the requestType of the new Request.
     * @param requestCommand the requestCommand of the new Request.
     * @param requestData the requestData of the new Request.
     *                    Should be the toString() form of the relevant data.
     *                    For set/toggle actuator state, it should be formatted "[clientId]#[Filter]".
     *                    Must not contain any commas.
     */
    public Request(int clientId, double timeStamp, RequestType requestType, RequestCommand requestCommand, String requestData) {
        this.timeStamp = timeStamp;
        this.requestType = requestType;
        this.requestCommand = requestCommand;
        this.requestData = requestData;
        this.clientId = clientId;
    }

    /**
     * Gets this Request's timeStamp.
     * @return this Request's timeStamp.
     */
    public double getTimeStamp() {
        return timeStamp;
    }

    /**
     * Gets this Request's requestType.
     * @return this Request's requestType.
     */
    public RequestType getRequestType() {
        return requestType;
    }

    /**
     * Gets this Request's requestCommand.
     * @return this Request's requestCommand.
     */
    public RequestCommand getRequestCommand() {
        return requestCommand;
    }

    /**
     * Gets this Request's requestData.
     * @return this Request's requestData.
     */
    public String getRequestData() {
        return requestData;
    }

    /**
     * Gets this Request's clientId.
     * @return this Request's clientId.
     */
    public int getClientId() {
        return clientId;
    }

    /**
     * Constructs a new Request from a String.
     * May be used to reconstruct a Request from toString().
     * @param requestString a string generated by toString().
     * @return a new Request, equivalent to the original before being serialized.
     */
    public static Request parse(String requestString) {
        List<String> requestComponents = Arrays.asList(requestString.split(","));

        String clientIdString = requestComponents.get(1);
        int clientId = Integer.parseInt(clientIdString);

        String timeStampString = requestComponents.get(2);
        double timeStamp = Double.parseDouble(timeStampString);

        String requestTypeString = requestComponents.get(3);
        RequestType requestType;
        switch(requestTypeString) {
            case "CONFIG":      requestType = RequestType.CONFIG;
                                break;
            case "CONTROL":     requestType = RequestType.CONTROL;
                                break;
            case "ANALYSIS":    requestType = RequestType.ANALYSIS;
                                break;
            case "PREDICT":     requestType = RequestType.PREDICT;
                                break;
            default:            requestType = null;
        }

        String requestCommandString = requestComponents.get(4);
        RequestCommand requestCommand;
        switch(requestCommandString) {
            case "CONFIG_UPDATE_MAX_WAIT_TIME":     requestCommand = RequestCommand.CONFIG_UPDATE_MAX_WAIT_TIME;
                                                    break;
            case "CONTROL_SET_ACTUATOR_STATE":      requestCommand = RequestCommand.CONTROL_SET_ACTUATOR_STATE;
                                                    break;
            case "CONTROL_TOGGLE_ACTUATOR_STATE":   requestCommand = RequestCommand.CONTROL_TOGGLE_ACTUATOR_STATE;
                                                    break;
            case "CONTROL_NOTIFY_IF":               requestCommand = RequestCommand.CONTROL_NOTIFY_IF;
                                                    break;
            case "ANALYSIS_GET_EVENTS_IN_WINDOW":   requestCommand = RequestCommand.ANALYSIS_GET_EVENTS_IN_WINDOW;
                                                    break;
            case "ANALYSIS_GET_ALL_ENTITIES":       requestCommand = RequestCommand.ANALYSIS_GET_ALL_ENTITIES;
                                                    break;
            case "ANALYSIS_GET_LATEST_EVENTS":      requestCommand = RequestCommand.ANALYSIS_GET_LATEST_EVENTS;
                                                    break;
            case "ANALYSIS_GET_MOST_ACTIVE_ENTITY": requestCommand = RequestCommand.ANALYSIS_GET_MOST_ACTIVE_ENTITY;
                                                    break;
            case "PREDICT_NEXT_N_TIMESTAMPS":       requestCommand = RequestCommand.PREDICT_NEXT_N_TIMESTAMPS;
                                                    break;
            case "PREDICT_NEXT_N_VALUES":           requestCommand = RequestCommand.PREDICT_NEXT_N_VALUES;
                                                    break;
            default:                                requestCommand = null;
        }

        // to deal with internal commas in requestData;
        // everything after requestCommand is parsed as part of requestData
        StringBuilder requestDataBuilder = new StringBuilder();
        requestDataBuilder.append(requestComponents.get(5));
        for(int i = 6; i < requestComponents.size() - 1; i++) {
            requestDataBuilder.append(',' + requestComponents.get(i));
        }
        String requestData = requestDataBuilder.toString();

        return new Request(clientId, timeStamp, requestType, requestCommand, requestData);
    }

    /**
     * Constructs a new String corresponding to the Request.
     * @return a new String corresponding to the Request.
     */
    @Override
    public String toString() {
        return "Request{" +
                "," + getClientId() +
                "," + getTimeStamp() +
                "," + getRequestType() +
                "," + getRequestCommand() +
                "," + getRequestData() +
                ",}";
    }

    /**
     * Determines whether this is equivalent to another Object.
     * @param other the Object that this is being compared to.
     * @return true if this equals other; false otherwise
     */
    @Override
    public boolean equals(Object other) {
            if(other instanceof Request) {
                if (other == this) {
                    return true;
                }
                Request otherRequest = (Request) other;
                return this.getClientId() == otherRequest.getClientId() &&
                        this.getTimeStamp() == otherRequest.getTimeStamp() &&
                        this.getRequestType() == otherRequest.getRequestType() &&
                        this.getRequestCommand() == otherRequest.getRequestCommand() &&
                        this.getRequestData().equals(otherRequest.getRequestData());
            }
            return false;
    }
}