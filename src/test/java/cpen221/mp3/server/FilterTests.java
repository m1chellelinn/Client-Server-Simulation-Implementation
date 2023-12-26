package cpen221.mp3.server;

import cpen221.mp3.CSVEventReader;
import cpen221.mp3.event.ActuatorEvent;
import cpen221.mp3.event.Event;
import cpen221.mp3.event.SensorEvent;
import static cpen221.mp3.server.BooleanOperator.EQUALS;
import static cpen221.mp3.server.BooleanOperator.NOT_EQUALS;
import static cpen221.mp3.server.DoubleOperator.GREATER_THAN_OR_EQUALS;
import static cpen221.mp3.server.DoubleOperator.LESS_THAN;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class FilterTests{

    String csvFilePath = "data/tests/single_client_1000_events_in-order.csv";
    CSVEventReader eventReader = new CSVEventReader(csvFilePath);
    List<Event> eventList = eventReader.readEvents();

    @Test
    public void testFilterTimeStampSingleEvent() {
        Event event1 = new SensorEvent(0.00011, 0,
                1,"TempSensor", 1.0);
        Event event2 = new ActuatorEvent(0.33080, 0,
                97,"Switch", false);
        Filter timeStampFilter = new Filter("timestamp", DoubleOperator.GREATER_THAN, 0.0);
        assertTrue(timeStampFilter.satisfies(event1));
        assertTrue(timeStampFilter.satisfies(event2));
    }

    @Test
    public void testFilterBooleanValueSingleEvent() {
        Event event1 = new SensorEvent(0.00011, 0,
                1,"TempSensor", 1.0);
        Event event2 = new ActuatorEvent(0.33080, 0,
                97,"Switch", true);
        Filter booleanFilter = new Filter(BooleanOperator.EQUALS, true);
        assertFalse(booleanFilter.satisfies(event1));
        assertTrue(booleanFilter.satisfies(event2));
    }

    @Test
    public void testBooleanFilter() {
        Event actuatorEvent = eventList.get(3);
        Filter sensorFilter = new Filter(EQUALS, false);
        assertEquals(true, sensorFilter.satisfies(actuatorEvent));
    }

    @Test
    public void testDoubleFilterTS() {
        Event sensorEvent = eventList.get(0);
        Filter sensorFilter = new Filter("timestamp", LESS_THAN, 1);
        assertEquals(true, sensorFilter.satisfies(sensorEvent));
    }


    @Test
    public void testDoubleFilterValue() {
        Event sensorEvent = eventList.get(0);
        Filter sensorFilter = new Filter("value", GREATER_THAN_OR_EQUALS, 23);
        assertEquals(false, sensorFilter.satisfies(sensorEvent));
    }

    @Test
    public void testComplexFilter() {
        Event sensorEvent = eventList.get(1);
        Filter sensorValueFilter = new Filter("value", GREATER_THAN_OR_EQUALS, 23);
        Filter sensorTSFilter = new Filter("timestamp", LESS_THAN, 1);
        List<Filter> filterList = new ArrayList<>();
        filterList.add(sensorValueFilter);
        filterList.add(sensorTSFilter);
        Filter complexFilter = new Filter(filterList);
        assertEquals(true, complexFilter.satisfies(sensorEvent));
    }

    @Test
    public void testMultiEventSatisfies() {
        List<Event> eventsList = new ArrayList<>();
        eventsList.add(eventList.get(0));
        eventsList.add(eventList.get(1));
        eventsList.add(eventList.get(2));
        Filter sensorValueFilter = new Filter("value", GREATER_THAN_OR_EQUALS, 23);
        Filter sensorTSFilter = new Filter("timestamp", LESS_THAN, 1);
        List<Filter> filterList = new ArrayList<>();
        filterList.add(sensorValueFilter);
        filterList.add(sensorTSFilter);
        Filter complexFilter = new Filter(filterList);
        assertEquals(false, complexFilter.satisfies(eventsList));
    }

    @Test
    public void testTrueMultiEventSatisfies() {
        List<Event> eventsList = new ArrayList<>();
        eventsList.add(eventList.get(0));
        eventsList.add(eventList.get(1));
        eventsList.add(eventList.get(2));
        Filter sensorTSFilter = new Filter("timestamp", LESS_THAN, 1);
        assertEquals(true, sensorTSFilter.satisfies(eventsList));
    }

    @Test
    public void testSift() {
        Event sensorEvent = eventList.get(1);
        Filter sensorValueFilter = new Filter("value", GREATER_THAN_OR_EQUALS, 23);
        Filter sensorTSFilter = new Filter("timestamp", LESS_THAN, 1);
        List<Filter> filterList = new ArrayList<>();
        filterList.add(sensorValueFilter);
        filterList.add(sensorTSFilter);
        Filter complexFilter = new Filter(filterList);
        assertEquals(sensorEvent, complexFilter.sift(sensorEvent));
    }

    @Test
    public void testMultiEventSift() {
        List<Event> eventsList = new ArrayList<>();
        eventsList.add(eventList.get(0));
        eventsList.add(eventList.get(1));
        eventsList.add(eventList.get(2));
        Filter sensorValueFilter = new Filter("value", GREATER_THAN_OR_EQUALS, 23);
        Filter sensorTSFilter = new Filter("timestamp", LESS_THAN, 1);
        List<Filter> filterList = new ArrayList<>();
        filterList.add(sensorValueFilter);
        filterList.add(sensorTSFilter);
        Filter complexFilter = new Filter(filterList);
        List<Event> filteredEvents = new ArrayList<>();
        filteredEvents.add(eventList.get(1));
        filteredEvents.add(eventList.get(2));
        assertEquals(filteredEvents, complexFilter.sift(eventsList));
    }

    @Test
    public void testParseComposedFilter1() {
        List<Filter> filterList = new ArrayList<>();
        filterList.add(new Filter(EQUALS, true));
        filterList.add(new Filter("timestamp", LESS_THAN, 1));
        Filter composedFilter = new Filter(filterList);

        assertEquals(composedFilter, Filter.parse(composedFilter.toString()));
    }

    @Test
    public void testParseComposedFilter2() {
        List<Filter> filterList1 = new ArrayList<>();
        filterList1.add(new Filter(EQUALS, true));
        filterList1.add(new Filter("timestamp", LESS_THAN, 1));
        Filter composedFilter1 = new Filter(filterList1);

        List<Filter> filterList2 = new ArrayList<>();
        filterList2.add(composedFilter1);
        filterList2.add(new Filter("value", GREATER_THAN_OR_EQUALS, 24));
        Filter composedFilter2 = new Filter(filterList2);

        assertEquals(composedFilter2, Filter.parse(composedFilter2.toString()));
    }

    @Test
    public void testFilterNotEqual1() {
        Filter filter1 = new Filter(EQUALS, true);
        Filter filter2 = new Filter(NOT_EQUALS, false);
        assertNotEquals(filter1, filter2);
    }

    @Test
    public void testFilterNotEqual2() {
        Filter filter1 = new Filter(EQUALS, true);
        Filter filter2 = new Filter("timestamp", LESS_THAN, 1);
        assertNotEquals(filter1, filter2);
    }

    @Test
    public void testFilterNotEqual3() {
        List<Filter> filterList1 = new ArrayList<>();
        filterList1.add(new Filter(EQUALS, true));
        filterList1.add(new Filter("timestamp", LESS_THAN, 1));
        Filter filter1 = new Filter(filterList1);

        List<Filter> filterList2 = new ArrayList<>();
        filterList2.add(new Filter(NOT_EQUALS, true));
        filterList2.add(new Filter("timestamp", LESS_THAN, 1));
        Filter filter2 = new Filter(filterList2);

        assertNotEquals(filter1, filter2);
    }

    @Test
    public void testFilterEqual1() {
        List<Filter> filterList1 = new ArrayList<>();
        filterList1.add(new Filter(EQUALS, true));
        filterList1.add(new Filter("timestamp", LESS_THAN, 1));
        Filter filter1 = new Filter(filterList1);

        List<Filter> filterList2 = new ArrayList<>();
        filterList2.add(new Filter("timestamp", LESS_THAN, 1));
        filterList2.add(new Filter(EQUALS, true));
        Filter filter2 = new Filter(filterList2);

        assertEquals(filter1, filter2);
    }

    @Test
    public void testFilterEqual2() {
        List<Filter> filterList1 = new ArrayList<>();
        filterList1.add(new Filter(EQUALS, true));
        filterList1.add(new Filter("timestamp", LESS_THAN, 1));
        Filter composedFilter1 = new Filter(filterList1);

        List<Filter> filterList2 = new ArrayList<>();
        filterList2.add(composedFilter1);
        filterList2.add(new Filter("value", GREATER_THAN_OR_EQUALS, 24));
        Filter composedFilter2 = new Filter(filterList2);

        List<Filter> filterList3 = new ArrayList<>();
        filterList3.add(new Filter(EQUALS, true));
        filterList3.add(new Filter("value", GREATER_THAN_OR_EQUALS, 24));
        Filter composedFilter3 = new Filter(filterList3);

        List<Filter> filterList4 = new ArrayList<>();
        filterList4.add(composedFilter3);
        filterList4.add(new Filter("timestamp", LESS_THAN, 1));
        Filter composedFilter4 = new Filter(filterList4);

        assertEquals(composedFilter2, composedFilter4);
    }
}

