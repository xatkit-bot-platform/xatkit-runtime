package com.xatkit.core.platform.io;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.XatkitException;
import com.xatkit.intent.ContextInstance;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.ContextParameterValue;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.EventInstance;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.xatkit.dsl.DSL.event;
import static org.assertj.core.api.Assertions.assertThat;

public class JsonEventMatcherTest extends AbstractXatkitTest {

    private static JsonEventMatcher.HeaderValue validHeaderValue = JsonEventMatcher.HeaderValue.of("hkey", "hvalue");

    private static JsonEventMatcher.FieldValue validFieldValue = JsonEventMatcher.FieldValue.of("fkey", "fvalue");

    private static EventDefinition eventWithDataJsonValue;

    private static EventDefinition eventWithoutDataJsonValue;

    private static List<Header> validHeaders;

    private static JsonElement validPayload;

    private JsonEventMatcher matcher;

    /*
     * Create the following Json object used in the test:
     * <pre>
     * {@code
     * {
     *   "fkey" = "fvalue",
     *   "top-level" = {
     *     "inner-field" = "inner-value"
     *   }
     * }
     * }
     * </pre>
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        validPayload = new JsonObject();
        ((JsonObject) validPayload).addProperty(validFieldValue.getKey(), validFieldValue.getValue());
        JsonObject innerObject = new JsonObject();
        innerObject.addProperty("inner-field", "inner-value");
        ((JsonObject) validPayload).add("top-level", innerObject);
        validHeaders = new ArrayList<>();
        validHeaders.add(new BasicHeader(validHeaderValue.getKey(), validHeaderValue.getValue()));
        eventWithDataJsonValue = event("EventWithDataJsonValue")
                .parameter("json")
                .getEventDefinition();
        eventWithoutDataJsonValue = event("EventWithoutDataJsonValue")
                .getEventDefinition();
    }

    @Before
    public void setUp() {
        this.matcher = new JsonEventMatcher();
    }

    @Test(expected = NullPointerException.class)
    public void addMatchableEventNullHeaderValue() {
        matcher.addMatchableEvent(null, validFieldValue, eventWithDataJsonValue);
    }

    @Test(expected = NullPointerException.class)
    public void addMatchableEventNullFieldValue() {
        matcher.addMatchableEvent(validHeaderValue, null, eventWithDataJsonValue);
    }

    @Test(expected = NullPointerException.class)
    public void addMatchableEventNullEventTypeName() {
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, null);
    }

    @Test
    public void addMatchableEventSingleEvent() {
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, eventWithDataJsonValue);
        assertThat(matcher.matchableEvents).as("Matchable events map size is 1").hasSize(1);
        assertThat(matcher.matchableEvents).as("Matchable events map contains the given HeaderValue").containsKey
                (validHeaderValue);
        Map<JsonEventMatcher.FieldValue, EventDefinition> fieldValues = matcher.matchableEvents.get(validHeaderValue);
        assertThat(fieldValues).as("Matchable events map HeaderValue value is not null").isNotNull();
        assertThat(fieldValues).as("FieldValue sub map size is 1").hasSize(1);
        assertThat(fieldValues).as("FieldValue sub map contains the given FieldValue").containsKey(validFieldValue);
        EventDefinition eventDefinition = fieldValues.get(validFieldValue);
        assertThat(eventDefinition).isNotNull();
        assertThat(eventDefinition).isEqualTo(eventWithDataJsonValue);
    }

    @Test
    public void addMatchableEventsSameHeaderValue() {
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, eventWithDataJsonValue);
        JsonEventMatcher.FieldValue validFieldValue2 = JsonEventMatcher.FieldValue.of("fkey2", "fvalue2");
        EventDefinition event2 = event("Event2")
                .parameter("json")
                .getEventDefinition();
        String validEventTypeName2 = "EventName2";
        matcher.addMatchableEvent(validHeaderValue, validFieldValue2, event2);
        assertThat(matcher.matchableEvents).as("Matchable events map not empty").isNotEmpty();
        assertThat(matcher.matchableEvents).as("Matchable events map size is 1").hasSize(1);
        assertThat(matcher.matchableEvents).as("Matchable events map contains the given HeaderValue").containsKey
                (validHeaderValue);
        Map<JsonEventMatcher.FieldValue, EventDefinition> fieldValues = matcher.matchableEvents.get(validHeaderValue);
        assertThat(fieldValues).as("Matchable events map HeaderValue value is not null").isNotNull();
        assertThat(fieldValues).as("FieldValue sub map size is 2").hasSize(2);
        assertThat(fieldValues).as("FieldValue sub map contains the first FieldValue").containsKey(validFieldValue);
        assertThat(fieldValues).as("FieldValue sub map contains the second FieldValue").containsKey(validFieldValue2);
        EventDefinition storedEvent1 = fieldValues.get(validFieldValue);
        assertThat(storedEvent1).isNotNull();
        assertThat(storedEvent1).as("Valid EventTypeName1").isEqualTo(eventWithDataJsonValue);
        EventDefinition storedEvent2 = fieldValues.get(validFieldValue2);
        assertThat(storedEvent2).isNotNull();
        assertThat(storedEvent2).isEqualTo(event2);
    }

    @Test
    public void addMatchableEventsDifferentHeaderValuesSameFieldValues() {
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, eventWithDataJsonValue);
        JsonEventMatcher.HeaderValue validHeaderValue2 = JsonEventMatcher.HeaderValue.of("hkey2", "hvalue2");
        EventDefinition event2 = event("Event2")
                .parameter("json")
                .getEventDefinition();
        matcher.addMatchableEvent(validHeaderValue2, validFieldValue, event2);
        assertThat(matcher.matchableEvents).as("Matchable events map not empty").isNotEmpty();
        assertThat(matcher.matchableEvents).as("Matchable events map size is 2").hasSize(2);
        assertThat(matcher.matchableEvents).as("Matchable events map contains the first HeaderValue").containsKey
                (validHeaderValue);
        Map<JsonEventMatcher.FieldValue, EventDefinition> fieldValues1 = matcher.matchableEvents.get(validHeaderValue);
        assertThat(fieldValues1).as("Matchable events map first HeaderValue value is not null").isNotNull();
        assertThat(fieldValues1).as("FieldValue1 sub map size is 1").hasSize(1);
        assertThat(fieldValues1).as("FieldValue1 sub map contains the valid FieldValue").containsKey(validFieldValue);
        EventDefinition storedEvent1 = fieldValues1.get(validFieldValue);
        assertThat(storedEvent1).isNotNull();
        assertThat(storedEvent1).isEqualTo(eventWithDataJsonValue);
        Map<JsonEventMatcher.FieldValue, EventDefinition> fieldValues2 = matcher.matchableEvents.get(validHeaderValue2);
        assertThat(fieldValues2).as("Matchable events map second HeaderValue value is not null").isNotNull();
        assertThat(fieldValues2).as("FieldValue2 sub map size is 1").hasSize(1);
        assertThat(fieldValues2).as("FieldValue2 sub map contains the valid FieldValue").containsKey(validFieldValue);
        EventDefinition storedEvent2 = fieldValues2.get(validFieldValue);
        assertThat(storedEvent2).isNotNull();
        assertThat(storedEvent2).isEqualTo(event2);
    }

    @Test
    public void addMatchableEventsSameHeaderValuesSameFieldValuesSameEventNames() {
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, eventWithDataJsonValue);
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, eventWithDataJsonValue);
        assertThat(matcher.matchableEvents).as("Matchable events map not empty").isNotEmpty();
        assertThat(matcher.matchableEvents).as("Matchable events map size is 1").hasSize(1);
        assertThat(matcher.matchableEvents).as("Matchable events map contains the given HeaderValue").containsKey
                (validHeaderValue);
        Map<JsonEventMatcher.FieldValue, EventDefinition> fieldValues = matcher.matchableEvents.get(validHeaderValue);
        assertThat(fieldValues).as("Matchable events map HeaderValue value is not null").isNotNull();
        assertThat(fieldValues).as("FieldValue sub map size is 1").hasSize(1);
        assertThat(fieldValues).as("FieldValue sub map contains the valid FieldValue").containsKey(validFieldValue);
        EventDefinition storedEvent = fieldValues.get(validFieldValue);
        assertThat(storedEvent).isNotNull();
        assertThat(storedEvent).isEqualTo(eventWithDataJsonValue);
    }

    @Test(expected = XatkitException.class)
    public void addMatchableEventsSameHeaderValuesFirstEmptyFieldValuesSameEventNames() {
        matcher.addMatchableEvent(validHeaderValue, JsonEventMatcher.FieldValue.EMPTY_FIELD_VALUE, eventWithDataJsonValue);
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, eventWithDataJsonValue);
    }

    @Test(expected = XatkitException.class)
    public void addMatchableEventsSameHeaderValuesFirstValidSecondEmptyFieldValuesSameEventNames() {
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, eventWithDataJsonValue);
        matcher.addMatchableEvent(validHeaderValue, JsonEventMatcher.FieldValue.EMPTY_FIELD_VALUE, eventWithDataJsonValue);
    }

    @Test
    public void addMatchableEventsSameHeaderValuesBothEmptyFieldValuesSameEventName() {
        matcher.addMatchableEvent(validHeaderValue, JsonEventMatcher.FieldValue.EMPTY_FIELD_VALUE, eventWithDataJsonValue);
        matcher.addMatchableEvent(validHeaderValue, JsonEventMatcher.FieldValue.EMPTY_FIELD_VALUE, eventWithDataJsonValue);
        assertThat(matcher.matchableEvents).as("Matchable events map not empty").isNotEmpty();
        assertThat(matcher.matchableEvents).as("Matchable events map size is 1").hasSize(1);
        assertThat(matcher.matchableEvents).as("Matchable events map contains the given HeaderValue").containsKey
                (validHeaderValue);
        Map<JsonEventMatcher.FieldValue, EventDefinition> fieldValues = matcher.matchableEvents.get(validHeaderValue);
        assertThat(fieldValues).as("Matchable events map HeaderValue is not null").isNotNull();
        assertThat(fieldValues).as("FieldValue sub map size is 1").hasSize(1);
        assertThat(fieldValues).as("FieldValue sub map contains the empty FieldValue").containsKey(JsonEventMatcher
                .FieldValue.EMPTY_FIELD_VALUE);
        EventDefinition storeEvent = fieldValues.get(JsonEventMatcher.FieldValue.EMPTY_FIELD_VALUE);
        assertThat(storeEvent).isNotNull();
        assertThat(storeEvent).isEqualTo(eventWithDataJsonValue);
    }

    @Test(expected = XatkitException.class)
    public void addMatchableEventsSameHeaderValuesSameFieldValuesDifferentEventNames() {
        EventDefinition event2 = event("Event2")
                .parameter("json")
                .getEventDefinition();
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, eventWithDataJsonValue);
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, event2);
    }

    @Test(expected = NullPointerException.class)
    public void matchNullHeaders() {
        matcher.match(null, validPayload);
    }

    @Test(expected = NullPointerException.class)
    public void matchNullContent() {
        matcher.match(validHeaders, null);
    }

    @Test
    public void matchNotAddedMatchableEvent() {
        EventInstance eventInstance = matcher.match(validHeaders, validPayload);
        assertThat(eventInstance).as("Null EventInstance").isNull();
    }

    @Test
    public void matchValidEventDefinition() {
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, eventWithDataJsonValue);
        EventInstance eventInstance = matcher.match(validHeaders, validPayload);
        assertThat(eventInstance).as("Not null EventInstance").isNotNull();
        assertThat(eventInstance.getDefinition()).as("Not null EventDefinition").isNotNull();
        assertThat(eventInstance.getDefinition()).as("Valid EventDefinition").isEqualTo(eventWithDataJsonValue);
        assertThat(eventInstance.getOutContextInstances()).as("Event Instance contains one out context instance")
                .hasSize(1);
        ContextInstance outContextInstance = eventInstance.getOutContextInstances().get(0);
        assertThat(outContextInstance).as("Not null out context instance").isNotNull();
        assertThat(outContextInstance.getDefinition()).as("Not null out context instance definition").isNotNull();
        assertThat(outContextInstance.getDefinition().getName()).as("Valid out context instance definition")
                .isEqualTo("XATKITCONTEXT");
        assertThat(outContextInstance.getValues()).as("Out context instance contains two out values").hasSize
                (1);
        ContextParameterValue value = outContextInstance.getValues().get(0);
        assertThat(value.getContextParameter()).as("Value1 context parameter not null").isNotNull();
        ContextParameter contextParameter1 = value.getContextParameter();
        assertThat(contextParameter1.getName()).as("Valid value1 context parameter name").isEqualTo
                ("json");
        assertThat(value.getValue()).isInstanceOf(JsonElement.class);
        assertThat(value.getValue()).isEqualTo(validPayload);
    }

    @Test(expected = IllegalArgumentException.class)
    public void matchInvalidEventDefinition() {
        /*
         * In this test the EventDefinition does not define a data.json context value.
         */
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, eventWithoutDataJsonValue);
        matcher.match(validHeaders, validPayload);
    }
}
