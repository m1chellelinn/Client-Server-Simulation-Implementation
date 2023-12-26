package cpen221.mp3.server;

import cpen221.mp3.event.*;

import java.util.*;

enum FilterType {
    BOOLEAN,
    DOUBLE,
    COMPOSED
}

enum DoubleOperator {
    EQUALS,
    GREATER_THAN,
    LESS_THAN,
    GREATER_THAN_OR_EQUALS,
    LESS_THAN_OR_EQUALS
}

enum BooleanOperator {
    EQUALS,
    NOT_EQUALS
}

/**
 * Abstraction Function:
 *     Represents a filter that can be applied to events. The type of filter is determined by its FilterType.
 *
 * Representation Invariant:
 *     - For Boolean filters:
 *         - booleanOperator is not null.
 *     - For Double filters:
 *         - doubleField is either "value" or "timestamp".
 *         - doubleOperator is not null.
 *     - For Composed filters:
 *         - composedFilters is not null and contains at least one filter.
 *
 * Thread Safety Argument:
 *     This class is threadsafe because it is immutable:
 *     - All fields are private and final.
 *     - The class does not provide any methods that can modify its state.
 *     - The data contained in the fields are of immutable types (e.g., primitive types, enums).
 *     - Instances of this class can be safely shared among multiple threads without the need for synchronization.
 */
public class Filter {
    // you can add private fields and methods to this class

    private FilterType filterType;

    // boolean filter fields
    private BooleanOperator booleanOperator;
    private boolean booleanValue;

    // double filter fields
    private String doubleField;
    private DoubleOperator doubleOperator;
    private double doubleValue;

    // composed filter field
    private List<Filter> composedFilters;

    /**
     * Constructs a filter that compares the boolean (actuator) event value
     * to the given boolean value using the given BooleanOperator.
     * (X (BooleanOperator) value), where X is the event's value passed by satisfies or sift methods.
     * A BooleanOperator can be one of the following:
     *
     * BooleanOperator.EQUALS
     * BooleanOperator.NOT_EQUALS
     *
     * @param operator the BooleanOperator to use to compare the event value with the given value
     * @param value the boolean value to match
     */
    public Filter(BooleanOperator operator, boolean value) {
        this.filterType = FilterType.BOOLEAN;
        this.booleanOperator = operator;
        this.booleanValue = value;
    }

    /**
     * Constructs a filter that compares a double field in events
     * with the given double value using the given DoubleOperator.
     * (X (DoubleOperator) value), where X is the event's value passed by satisfies or sift methods.
     * A DoubleOperator can be one of the following:
     *
     * DoubleOperator.EQUALS
     * DoubleOperator.GREATER_THAN
     * DoubleOperator.LESS_THAN
     * DoubleOperator.GREATER_THAN_OR_EQUALS
     * DoubleOperator.LESS_THAN_OR_EQUALS
     *
     * For non-double (boolean) value events, the satisfies method should return false.
     *
     * @param field the field to match (event "value" or event "timestamp")
     * @param operator the DoubleOperator to use to compare the event value with the given value
     * @param value the double value to match
     *
     * @throws IllegalArgumentException if the given field is not "value" or "timestamp"
     */
    public Filter(String field, DoubleOperator operator, double value) {
        if(!field.equals("value") && !field.equals("timestamp")) {
            throw new IllegalArgumentException();
        }
        this.filterType = FilterType.DOUBLE;
        this.doubleField = field;
        this.doubleOperator = operator;
        this.doubleValue = value;
    }

    /**
     * A filter can be composed of other filters.
     * in this case, the filter should satisfy all the filters in the list.
     * Constructs a complex filter composed of other filters.
     *
     * @param filters the list of filters to use in the composition
     */
    public Filter(List<Filter> filters) {
        this.filterType = FilterType.COMPOSED;
        this.composedFilters = filters;
    }

    /**
     * Returns true if the given event satisfies the filter criteria.
     *
     * @param event the event to check
     * @return true if the event satisfies the filter criteria, false otherwise
     */
    public boolean satisfies(Event event) {
        if(filterType ==  FilterType.BOOLEAN){
            return satisfiesBoolean(event);
        }
        else if(filterType ==  FilterType.DOUBLE){
            return satisfiesDouble(event);
        }
        else {
            return satisfiesComposedFilters(event);
        }
    }

    /**
     * Returns true if the event satisfies the filter criteria.
     *
     * @param event the event to check
     * @return true if the event satisfies the filter criteria, false otherwise
     */
    private boolean satisfiesBoolean(Event event) {
        boolean eventValue = event.getValueBoolean();
        return switch (booleanOperator) {
            case EQUALS -> eventValue == booleanValue;
            case NOT_EQUALS -> eventValue != booleanValue;
        };
    }

    /**
     * Returns true if the non-double (boolean) value event satisfies the filter criteria.
     *
     * @param event the event to check
     * @return true if the event satisfies the filter criteria, false otherwise
     */
    private boolean satisfiesDouble(Event event) {
        if (doubleField.equals("value")) {
            double eventValue = event.getValueDouble();
            return switch (doubleOperator) {
                case EQUALS -> eventValue == doubleValue;
                case GREATER_THAN -> eventValue > doubleValue;
                case LESS_THAN -> eventValue < doubleValue;
                case GREATER_THAN_OR_EQUALS -> eventValue >= doubleValue;
                case LESS_THAN_OR_EQUALS -> eventValue <= doubleValue;
            };
        }
        else{
            double eventTimeStamp = event.getTimeStamp();
            return switch (doubleOperator) {
                case EQUALS -> eventTimeStamp == doubleValue;
                case GREATER_THAN -> eventTimeStamp > doubleValue;
                case LESS_THAN -> eventTimeStamp < doubleValue;
                case GREATER_THAN_OR_EQUALS -> eventTimeStamp >= doubleValue;
                case LESS_THAN_OR_EQUALS -> eventTimeStamp <= doubleValue;
            };
        }
    }
    /**
     * Returns true if the composed filters event satisfies the filter criteria.
     *
     * @param event the event to check
     * @return true if the event satisfies the filter criteria, false otherwise
     */
    private boolean satisfiesComposedFilters(Event event) {
        for (Filter filter : composedFilters) {
            if (!filter.satisfies(event)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the given list of events satisfies the filter criteria.
     *
     * @param events the list of events to check
     * @return true if every event in the list satisfies the filter criteria, false otherwise
     */
    public boolean satisfies(List<Event> events) {
        for(Event event : events) {
            if(!this.satisfies(event)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a new event if it satisfies the filter criteria.
     * If the given event does not satisfy the filter criteria, then this method should return null.
     *
     * @param event the event to sift
     * @return a new event if it satisfies the filter criteria, null otherwise
     */
    public Event sift(Event event) {
        if(this.satisfies(event)) {
            if(event instanceof ActuatorEvent) {
                return new ActuatorEvent(event.getTimeStamp(),
                                         event.getClientId(),
                                         event.getEntityId(),
                                         event.getEntityType(),
                                         event.getValueBoolean());
            } else if(event instanceof SensorEvent) {
                return new SensorEvent(event.getTimeStamp(),
                                       event.getClientId(),
                                       event.getEntityId(),
                                       event.getEntityType(),
                                       event.getValueDouble());
            }
        }
        return null;
    }

    /**
     * Returns a list of events that contains only the events in the given list that satisfy the filter criteria.
     * If no events in the given list satisfy the filter criteria, then this method should return an empty list.
     *
     * @param events the list of events to sift
     * @return a list of events that contains only the events in the given list that satisfy the filter criteria
     *        or an empty list if no events in the given list satisfy the filter criteria
     */
    public List<Event> sift(List<Event> events) {
        List<Event> siftedEvents = new ArrayList<>();

        for (Event event : events) {
            Event siftedEvent = this.sift(event);
            if (siftedEvent != null) {
                siftedEvents.add(siftedEvent);
            }
        }
        return siftedEvents;
    }

    /**
     * Constructs a new Filter from a String.
     * May be used to reconstruct an equivalent Filter from toString().
     * @param filterString a string generated by toString().
     * @return a new Filter, equivalent to the original before being serialized.
     */
    public static Filter parse(String filterString) {
        if(filterString.contains("ComposedFilter")) {
            return parseComposedFilter(filterString);
        }
        if(filterString.contains("BooleanFilter")) {
            return parseBooleanFilter(filterString);
        }
        if(filterString.contains("DoubleFilter")) {
            return parseDoubleFilter(filterString);
        }
        return null;
    }

    /**
     * A helper method for parse(); handles parsing boolean Filters.
     * @param filterString the String representation of a boolean Filter.
     * @return the Filter corresponding to filterString.
     */
    private static Filter parseBooleanFilter(String filterString) {
        List<String> filterComponents = Arrays.asList(filterString.split(","));

        BooleanOperator booleanOperator = switch(filterComponents.get(1)) {
            case "EQUALS" -> BooleanOperator.EQUALS;
            case "NOT_EQUALS" -> BooleanOperator.NOT_EQUALS;
            default -> null;
        };

        boolean booleanValue = Boolean.parseBoolean(filterComponents.get(2));

        return new Filter(booleanOperator, booleanValue);
    }

    /**
     * A helper method for parse(); handles parsing double Filters.
     * @param filterString the String representation of a double Filter.
     * @return the Filter corresponding to filterString.
     */
    private static Filter parseDoubleFilter(String filterString) {
        List<String> filterComponents = Arrays.asList(filterString.split(","));

        String doubleField = filterComponents.get(1);

        DoubleOperator doubleOperator = switch(filterComponents.get(2)) {
            case "EQUALS" -> DoubleOperator.EQUALS;
            case "GREATER_THAN" -> DoubleOperator.GREATER_THAN;
            case "LESS_THAN" -> DoubleOperator.LESS_THAN;
            case "GREATER_THAN_OR_EQUALS" -> DoubleOperator.GREATER_THAN_OR_EQUALS;
            case "LESS_THAN_OR_EQUALS" -> DoubleOperator.LESS_THAN_OR_EQUALS;
            default -> null;
        };

        double doubleValue = Double.parseDouble(filterComponents.get(3));

        return new Filter(doubleField, doubleOperator, doubleValue);
    }

    /**
     * A helper method for parse(); handles parsing composed Filters.
     * @param filterString the String representation of a composed Filter.
     * @return the Filter corresponding to filterString.
     */
    private static Filter parseComposedFilter(String filterString) {
        List<String> filterComponents = Arrays.asList(filterString.split(":"));

        List<Filter> componentFilters = new ArrayList<>();
        for(String filterSubstring : filterComponents) {
            if(!filterSubstring.equals("ComposedFilter{") && !filterSubstring.equals("}")) {
                componentFilters.add(Filter.parse(filterSubstring));
            }
        }

        return new Filter(componentFilters);
    }

/*  @Override
    public String toString() {
        if(filterType ==  FilterType.BOOLEAN){
            return "BooleanFilter{," + booleanOperator + "," + booleanValue + ",}";
        }
        else if(filterType ==  FilterType.DOUBLE){
            return "DoubleFilter{," + doubleField + "," + doubleOperator + "," + doubleValue + ",}";
        }
        else{
            StringBuilder composedFilterString = new StringBuilder("ComposedFilter{,");
            for (int i = 0; i < composedFilters.size(); i++) {
                composedFilterString.append(composedFilters.get(i).toString());
                if (i < composedFilters.size() - 1) {
                    composedFilterString.append(",");
                }
            }
            composedFilterString.append(",}");
            return composedFilterString.toString();
        }
    } */

    /**
     * Constructs a new String corresponding to the Filter.
     * @return a new String corresponding to the Filter.
     */
    @Override
    public String toString() {
        if(filterType.equals(FilterType.BOOLEAN)) {
            return toStringBooleanFilter();
        }
        if(filterType.equals(FilterType.DOUBLE)) {
            return toStringDoubleFilter();
        }
        if(filterType.equals(FilterType.COMPOSED)) {
            return toStringComposedFilter();
        }
        return null;
    }

    /**
     * A helper method for toString(); handles boolean Filters.
     * @return a new String corresponding to the filter.
     */
    private String toStringBooleanFilter() {
        return "BooleanFilter{" +
                "," + booleanOperator +
                "," + booleanValue +
                "," + "}";
    }

    /**
     * A helper method for toString(); handles double Filters.
     * @return a new String corresponding to the filter.
     */
    private String toStringDoubleFilter() {
        return "DoubleFilter{" +
                "," + doubleField +
                "," + doubleOperator +
                "," + doubleValue +
                "," + "}";
    }

    /**
     * A helper method for toString(); handles composed Filters.
     * @return a new String corresponding to the filter.
     */
    private String toStringComposedFilter() {
        StringBuilder composedFilterString = new StringBuilder();
        composedFilterString.append("ComposedFilter{:");
        for(Filter filter : composedFilters) {
            composedFilterString.append(filter.toString());
            composedFilterString.append(":");
        }
        composedFilterString.append("}");
        return composedFilterString.toString();
    }

    /**
     * A helper method for equals(); gets the Set of double and boolean Filters that compose a filter.
     * @return the Set of double and boolean Filters that compose a filter
     */
    private Set<Filter> decompose() {
        Set<Filter> filters = new HashSet<>();
        if(this.filterType == FilterType.COMPOSED) {
            for(Filter filter : composedFilters) {
                filters.addAll(filter.decompose());
            }
            return filters;
        } else {
            filters.add(this);
            return filters;
        }
    }

    /**
     * Determines whether this is equivalent to another Object.
     * @param other the Object that this is being compared to.
     * @return true if this equals other; false otherwise
     */
    @Override
    public boolean equals(Object other) {
        if(other instanceof Filter) {
            if (other == this) {
                return true;
            }
            Filter otherFilter = (Filter) other;
            if(this.filterType == FilterType.BOOLEAN && otherFilter.filterType == FilterType.BOOLEAN) {
                return this.booleanValue == otherFilter.booleanValue &&
                        this.booleanOperator == otherFilter.booleanOperator;
            }
            if(this.filterType == FilterType.DOUBLE && otherFilter.filterType == FilterType.DOUBLE) {
                return this.doubleField.equals(otherFilter.doubleField) &&
                        this.doubleOperator == otherFilter.doubleOperator &&
                        this.doubleValue == otherFilter.doubleValue;
            }
            if(this.filterType == FilterType.COMPOSED && otherFilter.filterType == FilterType.COMPOSED) {
                return this.decompose().equals(otherFilter.decompose());
            }
        }
        return false;
    }

    /**
     * Generates a unique hash code for the Filter.
     * @return the Filter's hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(filterType, booleanValue, booleanOperator, doubleValue, doubleField, doubleOperator, composedFilters);
    }
}
