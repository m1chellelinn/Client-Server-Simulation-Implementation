package cpen221.mp3.client;

import cpen221.mp3.entity.Actuator;
import cpen221.mp3.entity.Entity;
import cpen221.mp3.entity.Sensor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimpleClientTests{

    @Test
    public void testRegisterEntities() {
        Client client = new Client(0, "test@test.com", "127.0.0.1", 4578);

        Entity thermostat = new Sensor(0, client.getClientId(), "TempSensor");
        Entity valve = new Actuator(0, -1, "Switch", false);

        assertFalse(thermostat.registerForClient(1));   // thermostat is already registered to client 0
        assertTrue(thermostat.registerForClient(0));    // registering thermostat for existing client (client 0) is fine and should return true
        assertTrue(valve.registerForClient(1));         // valve was unregistered, and can be registered to client 1, even if it does not exist
    }

    @Test
    public void testAddEntities() {
        Client client1 = new Client(0, "test1@test.com", "127.0.0.1", 4578);
        Client client2 = new Client(1, "test2@test.com", "127.0.0.1", 4578);

        Entity valve = new Actuator(0, -1, "Switch", false);

        assertTrue(client1.addEntity(valve));
        assertFalse(client2.addEntity(valve));
    }

    @Test
    public void testParseRequest1() {
        Request request = new Request(RequestType.CONFIG, RequestCommand.CONTROL_NOTIFY_IF, "Test");
        Assertions.assertEquals(request, Request.parse(request.toString()));
    }

    @Test
    public void testParseRequest2() {
        Request request = new Request(RequestType.CONTROL, RequestCommand.CONTROL_SET_ACTUATOR_STATE, "Test,3,t");
        Assertions.assertEquals(request, Request.parse(request.toString()));
    }

    @Test
    public void testParseRequest3() {
        Request request = new Request(RequestType.ANALYSIS, RequestCommand.PREDICT_NEXT_N_VALUES, "Test");
        Assertions.assertEquals(request, Request.parse(request.toString()));
    }

    @Test
    public void testParseActuator1() {
        Actuator actuator = new Actuator(10, 10, "Test", "127.0.0.1", 10);
        Assertions.assertEquals(actuator, Actuator.parse(actuator.toString()));
    }
}