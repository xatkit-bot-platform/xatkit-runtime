package fr.zelus.jarvis.io;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.zelus.jarvis.AbstractJarvisTest;
import fr.zelus.jarvis.core.EventDefinitionRegistry;
import fr.zelus.jarvis.core.JarvisException;
import fr.zelus.jarvis.intent.*;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonEventMatcherTest extends AbstractJarvisTest {

    private static JsonEventMatcher.HeaderValue validHeaderValue = JsonEventMatcher.HeaderValue.of("hkey", "hvalue");

    private static JsonEventMatcher.FieldValue validFieldValue = JsonEventMatcher.FieldValue.of("fkey", "fvalue");

    private static String validEventTypeName = "EventName";

    private static Header[] validHeaders;

    private static JsonElement validPayload;

    private static EventDefinitionRegistry eventRegistry;

    private static EventDefinition validEventDefinition;

    private static EventInstanceBuilder builder;

    private JsonEventMatcher matcher;

    @BeforeClass
    public static void setUpBeforeClass() {
        validPayload = new JsonObject();
        ((JsonObject) validPayload).addProperty(validFieldValue.getKey(), validFieldValue.getValue());
        JsonObject innerObject = new JsonObject();
        innerObject.addProperty("inner-field", "inner-value");
        ((JsonObject) validPayload).add("top-level", innerObject);
        validHeaders = new Header[1];
        validHeaders[0] = new BasicHeader(validHeaderValue.getKey(), validHeaderValue.getValue());
        eventRegistry = new EventDefinitionRegistry();
        validEventDefinition = IntentFactory.eINSTANCE.createEventDefinition();
        validEventDefinition.setName(validEventTypeName);
        Context outContext = IntentFactory.eINSTANCE.createContext();
        validEventDefinition.getOutContexts().add(outContext);
        outContext.setName("outContext");
        ContextParameter param = IntentFactory.eINSTANCE.createContextParameter();
        param.setName(validFieldValue.getKey());
        ContextParameter param2 = IntentFactory.eINSTANCE.createContextParameter();
        param2.setName("top-level->inner-field");
        outContext.getParameters().add(param);
        outContext.getParameters().add(param2);
        eventRegistry.registerEventDefinition(validEventDefinition);
        builder = EventInstanceBuilder.newBuilder(eventRegistry);
    }

    @Before
    public void setUp() {
        this.matcher = new JsonEventMatcher(builder, new BaseConfiguration());
    }

    @Test(expected = NullPointerException.class)
    public void constructNullEventInstanceBuilder() {
        matcher = new JsonEventMatcher(null, new BaseConfiguration());
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        matcher = new JsonEventMatcher(builder, null);
    }

    @Test
    public void constructValidEventInstanceBuilder() {
        matcher = new JsonEventMatcher(builder, new BaseConfiguration());
        assertThat(matcher.eventInstanceBuilder).as("Valid EventInstanceBuilder").isEqualTo(builder);
        assertThat(matcher.matchableEvents).as("Empty matchable events map").isEmpty();
        assertThat(matcher.printBuilder).as("Valid print builder flag").isFalse();
    }

    @Test
    public void constructPrintBuilderTrue() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JsonEventMatcher.PRINT_BUILDER_CONTENT_KEY, true);
        matcher = new JsonEventMatcher(builder, configuration);
        assertThat(matcher.eventInstanceBuilder).as("Valid EventInstanceBuilder").isEqualTo(builder);
        assertThat(matcher.matchableEvents).as("Empty matchable events map").isEmpty();
        assertThat(matcher.printBuilder).as("Valid print builder flag").isTrue();
    }

    @Test
    public void constructPrintBuilderFalse() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JsonEventMatcher.PRINT_BUILDER_CONTENT_KEY, false);
        matcher = new JsonEventMatcher(builder, configuration);
        assertThat(matcher.eventInstanceBuilder).as("Valid EventInstanceBuilder").isEqualTo(builder);
        assertThat(matcher.matchableEvents).as("Empty matchable events map").isEmpty();
        assertThat(matcher.printBuilder).as("Valid print builder flag").isFalse();
    }

    @Test(expected = NullPointerException.class)
    public void addMatchableEventNullHeaderValue() {
        matcher.addMatchableEvent(null, validFieldValue, validEventTypeName);
    }

    @Test(expected = NullPointerException.class)
    public void addMatchableEventNullFieldValue() {
        matcher.addMatchableEvent(validHeaderValue, null, validEventTypeName);
    }

    @Test(expected = NullPointerException.class)
    public void addMatchableEventNullEventTypeName() {
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, null);
    }

    @Test
    public void addMatchableEventSingleEvent() {
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, validEventTypeName);
        assertThat(matcher.matchableEvents).as("Matchable events map size is 1").hasSize(1);
        assertThat(matcher.matchableEvents).as("Matchable events map contains the given HeaderValue").containsKey
                (validHeaderValue);
        Map<JsonEventMatcher.FieldValue, String> fieldValues = matcher.matchableEvents.get(validHeaderValue);
        assertThat(fieldValues).as("Matchable events map HeaderValue value is not null").isNotNull();
        assertThat(fieldValues).as("FieldValue sub map size is 1").hasSize(1);
        assertThat(fieldValues).as("FieldValue sub map contains the given FieldValue").containsKey(validFieldValue);
        String eventTypeName = fieldValues.get(validFieldValue);
        assertThat(eventTypeName).as("Not null EventTypeName").isNotNull();
        assertThat(eventTypeName).as("Valid EventTypeName").isEqualTo(validEventTypeName);
    }

    @Test
    public void addMatchableEventsSameHeaderValue() {
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, validEventTypeName);
        JsonEventMatcher.FieldValue validFieldValue2 = JsonEventMatcher.FieldValue.of("fkey2", "fvalue2");
        String validEventTypeName2 = "EventName2";
        matcher.addMatchableEvent(validHeaderValue, validFieldValue2, validEventTypeName2);
        assertThat(matcher.matchableEvents).as("Matchable events map not empty").isNotEmpty();
        assertThat(matcher.matchableEvents).as("Matchable events map size is 1").hasSize(1);
        assertThat(matcher.matchableEvents).as("Matchable events map contains the given HeaderValue").containsKey
                (validHeaderValue);
        Map<JsonEventMatcher.FieldValue, String> fieldValues = matcher.matchableEvents.get(validHeaderValue);
        assertThat(fieldValues).as("Matchable events map HeaderValue value is not null").isNotNull();
        assertThat(fieldValues).as("FieldValue sub map size is 2").hasSize(2);
        assertThat(fieldValues).as("FieldValue sub map contains the first FieldValue").containsKey(validFieldValue);
        assertThat(fieldValues).as("FieldValue sub map contains the second FieldValue").containsKey(validFieldValue2);
        String storedEventType1 = fieldValues.get(validFieldValue);
        assertThat(storedEventType1).as("Not null EventTypeName1").isNotNull();
        assertThat(storedEventType1).as("Valid EventTypeName1").isEqualTo(validEventTypeName);
        String storedEventType2 = fieldValues.get(validFieldValue2);
        assertThat(storedEventType2).as("Not null EventTypeName2").isNotNull();
        assertThat(storedEventType2).as("Valid EventTypeName2").isEqualTo(validEventTypeName2);
    }

    @Test
    public void addMatchableEventsDifferentHeaderValuesSameFieldValues() {
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, validEventTypeName);
        JsonEventMatcher.HeaderValue validHeaderValue2 = JsonEventMatcher.HeaderValue.of("hkey2", "hvalue2");
        String validEventTypeName2 = "EventName2";
        matcher.addMatchableEvent(validHeaderValue2, validFieldValue, validEventTypeName2);
        assertThat(matcher.matchableEvents).as("Matchable events map not empty").isNotEmpty();
        assertThat(matcher.matchableEvents).as("Matchable events map size is 2").hasSize(2);
        assertThat(matcher.matchableEvents).as("Matchable events map contains the first HeaderValue").containsKey
                (validHeaderValue);
        Map<JsonEventMatcher.FieldValue, String> fieldValues1 = matcher.matchableEvents.get(validHeaderValue);
        assertThat(fieldValues1).as("Matchable events map first HeaderValue value is not null").isNotNull();
        assertThat(fieldValues1).as("FieldValue1 sub map size is 1").hasSize(1);
        assertThat(fieldValues1).as("FieldValue1 sub map contains the valid FieldValue").containsKey(validFieldValue);
        String storedEventType1 = fieldValues1.get(validFieldValue);
        assertThat(storedEventType1).as("Not null EventTypeName1").isNotNull();
        assertThat(storedEventType1).as("Valid EventTypeName1").isEqualTo(validEventTypeName);
        Map<JsonEventMatcher.FieldValue, String> fieldValues2 = matcher.matchableEvents.get(validHeaderValue2);
        assertThat(fieldValues2).as("Matchable events map second HeaderValue value is not null").isNotNull();
        assertThat(fieldValues2).as("FieldValue2 sub map size is 1").hasSize(1);
        assertThat(fieldValues2).as("FieldValue2 sub map contains the valid FieldValue").containsKey(validFieldValue);
        String storedEventType2 = fieldValues2.get(validFieldValue);
        assertThat(storedEventType2).as("Not null EventTypeName2").isNotNull();
        assertThat(storedEventType2).as("Valid EventTypeName2").isEqualTo(validEventTypeName2);
    }

    @Test
    public void addMatchableEventsSameHeaderValuesSameFieldValuesSameEventNames() {
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, validEventTypeName);
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, validEventTypeName);
        assertThat(matcher.matchableEvents).as("Matchable events map not empty").isNotEmpty();
        assertThat(matcher.matchableEvents).as("Matchable events map size is 1").hasSize(1);
        assertThat(matcher.matchableEvents).as("Matchable events map contains the given HeaderValue").containsKey
                (validHeaderValue);
        Map<JsonEventMatcher.FieldValue, String> fieldValues = matcher.matchableEvents.get(validHeaderValue);
        assertThat(fieldValues).as("Matchable events map HeaderValue value is not null").isNotNull();
        assertThat(fieldValues).as("FieldValue sub map size is 1").hasSize(1);
        assertThat(fieldValues).as("FieldValue sub map contains the valid FieldValue").containsKey(validFieldValue);
        String storedEventType = fieldValues.get(validFieldValue);
        assertThat(storedEventType).as("Not null EventTypeName").isNotNull();
        assertThat(storedEventType).as("Valid EventTypeName").isEqualTo(validEventTypeName);
    }

    @Test(expected = JarvisException.class)
    public void addMatchableEventsSameHeaderValuesFirstEmptyFieldValuesSameEventNames() {
        matcher.addMatchableEvent(validHeaderValue, JsonEventMatcher.FieldValue.EMPTY_FIELD_VALUE, validEventTypeName);
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, validEventTypeName);
    }

    @Test(expected = JarvisException.class)
    public void addMatchableEventsSameHeaderValuesFirstValidSecondEmptyFieldValuesSameEventNames() {
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, validEventTypeName);
        matcher.addMatchableEvent(validHeaderValue, JsonEventMatcher.FieldValue.EMPTY_FIELD_VALUE, validEventTypeName);
    }

    @Test
    public void addMatchableEventsSameHeaderValuesBothEmptyFieldValuesSameEventName() {
        matcher.addMatchableEvent(validHeaderValue, JsonEventMatcher.FieldValue.EMPTY_FIELD_VALUE, validEventTypeName);
        matcher.addMatchableEvent(validHeaderValue, JsonEventMatcher.FieldValue.EMPTY_FIELD_VALUE, validEventTypeName);
        assertThat(matcher.matchableEvents).as("Matchable events map not empty").isNotEmpty();
        assertThat(matcher.matchableEvents).as("Matchable events map size is 1").hasSize(1);
        assertThat(matcher.matchableEvents).as("Matchable events map contains the given HeaderValue").containsKey
                (validHeaderValue);
        Map<JsonEventMatcher.FieldValue, String> fieldValues = matcher.matchableEvents.get(validHeaderValue);
        assertThat(fieldValues).as("Matchable events map HeaderValue is not null").isNotNull();
        assertThat(fieldValues).as("FieldValue sub map size is 1").hasSize(1);
        assertThat(fieldValues).as("FieldValue sub map contains the empty FieldValue").containsKey(JsonEventMatcher
                .FieldValue.EMPTY_FIELD_VALUE);
        String storedEventType = fieldValues.get(JsonEventMatcher.FieldValue.EMPTY_FIELD_VALUE);
        assertThat(storedEventType).as("Not null EventTypeName").isNotNull();
        assertThat(storedEventType).as("Valid EventTypeName").isEqualTo(validEventTypeName);
    }

    @Test(expected = JarvisException.class)
    public void addMatchableEventsSameHeaderValuesSameFieldValuesDifferentEventNames() {
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, validEventTypeName);
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, "EventName2");
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
    public void matchValidRegisteredEventDefinition() {
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, validEventTypeName);
        EventInstance eventInstance = matcher.match(validHeaders, validPayload);
        assertThat(eventInstance).as("Not null EventInstance").isNotNull();
        assertThat(eventInstance.getDefinition()).as("Not null EventDefinition").isNotNull();
        assertThat(eventInstance.getDefinition()).as("Valid EventDefinition").isEqualTo(validEventDefinition);
        assertThat(eventInstance.getOutContextValues()).as("Event Instance contains two out context values").hasSize
                (2);
        ContextParameterValue value1 = eventInstance.getOutContextValues().get(0);
        assertThat(value1.getContextParameter()).as("Value1 context parameter not null").isNotNull();
        ContextParameter contextParameter1 = value1.getContextParameter();
        assertThat(contextParameter1.getName()).as("Valid value1 context parameter name").isEqualTo
                (validFieldValue.getKey());
        assertThat(value1.getValue()).as("Valid value1 value").isEqualTo(validFieldValue.getValue());
        ContextParameterValue value2 = eventInstance.getOutContextValues().get(1);
        assertThat(value2.getContextParameter()).as("Value2 context parameter not null").isNotNull();
        ContextParameter contextParameter2 = value2.getContextParameter();
        assertThat(contextParameter2.getName()).as("Valid value2 context parameter name").isEqualTo("top-level->inner-field");
        assertThat(value2.getValue()).as("Valid value2 value").isEqualTo("inner-value");
    }

    @Test(expected = JarvisException.class)
    public void matchValidNotRegisteredEventDefinition() {
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, "EventName2");
        matcher.match(validHeaders, validPayload);
    }

    @Test(expected = JarvisException.class)
    public void matchValidRegisteredEventDefinitionMissingOutContext() {
        EventDefinitionRegistry registry = new EventDefinitionRegistry();
        EventDefinition eventDefinition = IntentFactory.eINSTANCE.createEventDefinition();
        eventDefinition.setName(validEventTypeName);
        Context outContext = IntentFactory.eINSTANCE.createContext();
        eventDefinition.getOutContexts().add(outContext);
        outContext.setName("outContext");
        ContextParameter param = IntentFactory.eINSTANCE.createContextParameter();
        param.setName(validFieldValue.getKey());
        outContext.getParameters().add(param);
        registry.registerEventDefinition(eventDefinition);
        EventInstanceBuilder builder = EventInstanceBuilder.newBuilder(registry);
        matcher = new JsonEventMatcher(builder, new BaseConfiguration());
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, validEventTypeName);
        matcher.match(validHeaders, validPayload);
    }

    @Test
    public void matchValidRegisteredEventDefinitionOutContextNotInFields() {
        /*
         * This should work, the EventInstanceBuilder does not check that all the out context parameters are set.
         */
        EventDefinitionRegistry registry = new EventDefinitionRegistry();
        EventDefinition eventDefinition = IntentFactory.eINSTANCE.createEventDefinition();
        eventDefinition.setName(validEventTypeName);
        Context outContext = IntentFactory.eINSTANCE.createContext();
        eventDefinition.getOutContexts().add(outContext);
        outContext.setName("outContext");
        ContextParameter param = IntentFactory.eINSTANCE.createContextParameter();
        param.setName(validFieldValue.getKey());
        ContextParameter param2 = IntentFactory.eINSTANCE.createContextParameter();
        param2.setName("top-level->inner-field");
        ContextParameter param3 = IntentFactory.eINSTANCE.createContextParameter();
        param3.setName("another-field");
        outContext.getParameters().add(param);
        outContext.getParameters().add(param2);
        outContext.getParameters().add(param3);
        registry.registerEventDefinition(eventDefinition);
        EventInstanceBuilder builder = EventInstanceBuilder.newBuilder(registry);
        matcher = new JsonEventMatcher(builder, new BaseConfiguration());
        matcher.addMatchableEvent(validHeaderValue, validFieldValue, validEventTypeName);
        matcher.match(validHeaders, validPayload);
    }
}
