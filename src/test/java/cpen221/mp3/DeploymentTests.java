package cpen221.mp3;

import cpen221.mp3.client.*;
import cpen221.mp3.entity.*;
import cpen221.mp3.event.*;

import cpen221.mp3.server.TimeWindow;
import org.junit.jupiter.api.*;
import java.util.*;

public class DeploymentTests {
    //TODO: To properly run our deployment tests, first go to MessageHandler.java and run the main() method.
    // then while main() is running, come back here and run a test.
    // (Preferably a single test at a time because it doesn't make sense that our MessageHandler should handle
    // multiple clients with clientId "0" at once.)
    //Note: We didn't know how to test deployment properly with JUnit.
    // So, we wrote down what you should expect to see on the terminal when our tests finish instead.
    Client client0;
    Sensor sensor1;
    Sensor sensor2;
    Sensor sensor3;
    Sensor sensor4;
    Actuator actuator1;
    Actuator actuator2;
    Actuator actuator3;
    Actuator actuator4;
    Actuator actuator5;
    Client client1;
    Sensor sensor5;
    Client client2;
    Client client3;
    Sensor sensor6;
    Actuator actuator6;
    List<Event> client0Events;
    List<Event> concurrentClientEvents;
    List<Request> client0Requests;
    List<Request> concurrentClientRequests;

    //TODO: hide this setUp method if you want. Expand it to check for details. It is quite long.
    @BeforeEach
    public void setUp() {
        client0 = new Client(0, "something@gmail.com", "127.0.0.1", 4949);
        sensor1 = new Sensor(1, 0, "TempSensor");
        sensor1.setEndpoint("127.0.0.1", 4949);
        sensor2 = new Sensor(2, 0, "TempSensor", "127.0.0.1", 4949);
        sensor3 = new Sensor(3, 0, "PressureSensor", "127.0.0.1", 4949);
        sensor4 = new Sensor(4, "CO2Sensor", "127.0.0.1", 4949);
        sensor4.registerForClient(0); //Tests weird ways to initialize entities
        actuator1 = new Actuator(97, 0, "Switch", true, "127.0.0.1", 4949);
        actuator2 = new Actuator(98, 0, "Switch", false, "127.0.0.1", 4949);
        actuator3 = new Actuator(99, 0, "Switch", true, "127.0.0.1", 4949);
        actuator4 = new Actuator(100, "switch", true, "127.0.0.1", 4949);
        actuator4.registerForClient(0); //Tests weird ways to initialize entities
        actuator5 = new Actuator(101, 0, "switch", true);
        actuator5.setEndpoint("127.0.0.1", 4949); //Tests weird ways to initialize entities

        client1 = new Client(1, "something@gmail.com", "127.0.0.1", 4949);
        sensor5 = new Sensor(5, 1, "PressureSensor", "127.0.0.1", 4949);
        client2 = new Client(2, "something@gmail.com", "127.0.0.1", 4949);
        client3 = new Client(3, "something@gmail.com", "127.0.0.1", 4949);
        sensor6 = new Sensor(6, 2, "CO2Sensor", "127.0.0.1", 4949);
        actuator6 = new Actuator(102, 2, "switch", true, "127.0.0.1", 4949);
    }

    @Test
    public void testDeploymentSingleClient() {
        //Note: we set sensor1's frequency really high so that it becomes the most active entity.
        sensor1.setEventGenerationFrequency(1);

        //Sleep the thread, wait for events to accumulate
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        //Then, we let the server tell the client which is the most active entity.
        // the response (on the client's terminal) should be a System.out.println with "1".
        // This is the case because entity 1 (sensor1) is the most active entity.
        // You can also see the time it took for the round trip (including the time it took the request to travel through sockets).
        // So the time shown there is  a bit longer than how long we actually took to process the request.
        client0.sendRequest(new Request(RequestType.ANALYSIS,RequestCommand.ANALYSIS_GET_MOST_ACTIVE_ENTITY, ""));

        //Sleep the thread, wait for a response
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testDeploymentSingleClientMultipleEntities() {
        sensor1.setEventGenerationFrequency(0.5);
        sensor2.setEventGenerationFrequency(1);

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        client0.sendRequest(new Request(RequestType.ANALYSIS,RequestCommand.ANALYSIS_GET_MOST_ACTIVE_ENTITY, ""));

        //Sleep the thread, wait for a response
        // the response (on the client's terminal) should be a System.out.println with "1".
        // This is the case because entity 1 (sensor1) is the most active entity.
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testDeploymentMultipleClients() {
        sensor1.setEventGenerationFrequency(0.5);
        sensor2.setEventGenerationFrequency(1);
        sensor5.setEventGenerationFrequency(1);
        sensor6.setEventGenerationFrequency(0.5);


        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        client0.sendRequest(new Request(RequestType.ANALYSIS,RequestCommand.ANALYSIS_GET_MOST_ACTIVE_ENTITY, ""));
        client1.sendRequest(new Request(RequestType.ANALYSIS,RequestCommand.ANALYSIS_GET_MOST_ACTIVE_ENTITY, ""));

        //Sleep the thread, wait for a response
        // the response (on the client's terminal) should be a System.out.println with a bunch of random event toString()'s.
        // (note that the toStrings may look weird, but that is entirely for our parse() method).
        // And sorry, we unfortunately can't predict these events :(
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    @Test
    public void testDeploymentMultipleClients5() {
        sensor1.setEventGenerationFrequency(0.1);
        sensor2.setEventGenerationFrequency(0.4);
        sensor5.setEventGenerationFrequency(0.5);

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        TimeWindow client0Window = new TimeWindow(System.currentTimeMillis() - 10000, System.currentTimeMillis());
        TimeWindow client1Window = new TimeWindow(System.currentTimeMillis() - 5000, System.currentTimeMillis());

        client0.sendRequest(new Request(RequestType.ANALYSIS,RequestCommand.ANALYSIS_GET_EVENTS_IN_WINDOW, client0Window.toString()));
        client1.sendRequest(new Request(RequestType.ANALYSIS,RequestCommand.ANALYSIS_GET_EVENTS_IN_WINDOW, client1Window.toString()));

        //Sleep the thread, wait for a response
        // the response (on the client's terminal) should be a System.out.println
        // with a bunch of random entity ID's for client 0, and two or three "5"s for client1
        // This represents all the entity ID's of events collected during the past 10 seconds (For client 0) and 5 seconds (for client 1)
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

