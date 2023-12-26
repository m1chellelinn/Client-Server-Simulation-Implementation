package cpen221.mp3.event;

import java.util.List;

import cpen221.mp3.CSVEventReader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EventTests{

    String csvFilePath = "data/tests/single_client_1000_events_in-order.csv";
    CSVEventReader eventReader = new CSVEventReader(csvFilePath);
    List<Event> eventList = eventReader.readEvents();

    @Test
    public void testCreateSingleEvent() {
        Event event = new SensorEvent(0.000111818, 0, 1,"TempSensor", 1.0);
        assertEquals(0.000111818, event.getTimeStamp());
        assertEquals(0, event.getClientId());
        assertEquals(1, event.getEntityId());
        assertEquals("TempSensor", event.getEntityType());
        assertEquals(1.0, event.getValueDouble());
    }

    @Test
    public void testSensorEvent() {
        Event sensorEvent = eventList.get(0);
        assertEquals(0.00011181831359863281, sensorEvent.getTimeStamp());
        assertEquals(0, sensorEvent.getClientId());
        assertEquals(0, sensorEvent.getEntityId());
        assertEquals("TempSensor", sensorEvent.getEntityType());
        assertEquals(22.21892397393261, sensorEvent.getValueDouble());
    }

    @Test
    public void testActuatorEvent() {
        Event actuatorEvent = eventList.get(3);
        assertEquals(0.33080601692199707, actuatorEvent.getTimeStamp());
        assertEquals(0, actuatorEvent.getClientId());
        assertEquals(97, actuatorEvent.getEntityId());
        assertEquals("Switch", actuatorEvent.getEntityType());
        assertEquals(false, actuatorEvent.getValueBoolean());
    }

    @Test
    public void testParseActuatorEvent1() {
        Event actuatorEvent = eventList.get(3);
        assertEquals(actuatorEvent, ActuatorEvent.parse(actuatorEvent.toString()));
    }

    @Test
    public void testParseActuatorEvent2() {
        Event actuatorEvent = eventList.get(5);
        assertEquals(actuatorEvent, ActuatorEvent.parse(actuatorEvent.toString()));
    }

    @Test
    public void testParseActuatorEvent3() {
        Event actuatorEvent = eventList.get(7);
        assertEquals(actuatorEvent, ActuatorEvent.parse(actuatorEvent.toString()));
    }

    @Test
    public void testParseSensorEvent1() {
        Event sensorEvent = eventList.get(0);
        assertEquals(sensorEvent, SensorEvent.parse(sensorEvent.toString()));
    }

    @Test
    public void testParseSensorEvent2() {
        Event sensorEvent = eventList.get(1);
        assertEquals(sensorEvent, SensorEvent.parse(sensorEvent.toString()));
    }

    @Test
    public void testParseSensorEvent3() {
        Event sensorEvent = eventList.get(2);
        assertEquals(sensorEvent, SensorEvent.parse(sensorEvent.toString()));
    }
}
