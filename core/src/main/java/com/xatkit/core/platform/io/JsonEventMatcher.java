package com.xatkit.core.platform.io;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xatkit.core.JarvisException;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.EventInstance;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;
import org.apache.http.Header;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A matcher that creates {@link EventInstance}s from Json webhook calls.
 * <p>
 * This class is configured through the {@link #addMatchableEvent(HeaderValue, FieldValue, String)} method, that
 * allows to specify an {@link EventInstance} type for a given header and field content. Matched requests are reified
 * into {@link EventInstance}s containing all the fields of the provided payload.
 */
public class JsonEventMatcher {

    /**
     * The {@link Configuration} key to store whether to print the {@link EventInstanceBuilder} content before
     * building the matched {@link EventInstance}.
     */
    public static String PRINT_BUILDER_CONTENT_KEY = "jarvis.event.matcher.print_builder";

    /**
     * The internal {@link Map} used to store the header and field values to match.
     */
    protected Map<HeaderValue, Map<FieldValue, String>> matchableEvents;

    /**
     * The {@link EventInstanceBuilder} used to reify the received payloads into {@link EventInstance}s.
     */
    protected EventInstanceBuilder eventInstanceBuilder;

    /**
     * A flag telling whether to print the {@link EventInstanceBuilder} content before building the matched
     * {@link EventInstance}.
     */
    protected boolean printBuilder;

    /**
     * Constructs a new {@link JsonEventMatcher} with the provided {@link EventInstanceBuilder}.
     *
     * @param eventInstanceBuilder the {@link EventInstanceBuilder} used to reify the received payloads into
     *                             {@link EventInstance}s.
     * @param configuration        the {@link Configuration} used to customize the {@link JsonEventMatcher}
     * @throws NullPointerException if the provided {@code eventInstanceBuilder} is {@code null}
     */
    public JsonEventMatcher(EventInstanceBuilder eventInstanceBuilder, Configuration configuration) {
        checkNotNull(eventInstanceBuilder, "Cannot construct a %s with the provided %s %s", this.getClass()
                .getSimpleName(), EventInstanceBuilder.class.getSimpleName(), eventInstanceBuilder);
        Log.info("Starting {0}", this.getClass().getSimpleName());
        this.matchableEvents = new HashMap<>();
        this.eventInstanceBuilder = eventInstanceBuilder;
        printBuilder = configuration.getBoolean(PRINT_BUILDER_CONTENT_KEY, false);
        if (printBuilder) {
            Log.info("{0} will print the builder content before building the matched {1}", this.getClass()
                    .getSimpleName(), EventInstance.class.getSimpleName());
        }
    }

    /**
     * Registers a new {@link EventInstance} to match from the provided {@code headerValue} and {@code fieldValue}.
     *
     * @param headerValue   the {@link HeaderValue} to match the received request against
     * @param fieldValue    the {@link FieldValue} to match the received payload against
     * @param eventTypeName the type of the {@link EventInstance} to return
     * @throws NullPointerException if the provided {@code headerValue}, {@code fieldValue}, or {@code eventTypeName}
     *                              is {@code null}
     * @throws JarvisException      if the provided {@code headerValue} and {@code fieldValue} are already associated to
     *                              another {@code eventTypeName}, or if calling this method would associate the
     *                              {@link FieldValue#EMPTY_FIELD_VALUE} and specialized {@link FieldValue}s to the
     *                              provided {@code headerValue}.
     * @see HeaderValue
     * @see FieldValue
     * @see FieldValue#EMPTY_FIELD_VALUE
     */
    public void addMatchableEvent(HeaderValue headerValue, FieldValue fieldValue, String eventTypeName) {
        checkNotNull(headerValue, "Cannot register the EventType %s with the provided %s %s", eventTypeName,
                HeaderValue.class.getSimpleName(), headerValue);
        checkNotNull(fieldValue, "Cannot register the EventType %s with the provided %s %s", eventTypeName,
                FieldValue.class.getSimpleName(), fieldValue);
        checkNotNull(eventTypeName, "Cannot register the provided EventType %s", eventTypeName);
        if (matchableEvents.containsKey(headerValue)) {
            Map<FieldValue, String> fields = matchableEvents.get(headerValue);
            if (fields.containsKey(FieldValue.EMPTY_FIELD_VALUE) && !fieldValue.equals(FieldValue.EMPTY_FIELD_VALUE)) {
                /*
                 * Check if there is an EMPTY_FIELD_VALUE registered for the provided HeaderValue. If it is the case
                 * throw an exception: it is not possible to register specialized FieldValues along with the
                 * EMPTY_FIELD_VALUE: the engine will always match the EMPTY_FIELD_VALUE.
                 */
                throw new JarvisException(MessageFormat.format("Cannot register the provided EventType {0}, the " +
                        "empty FieldValue {1} is already registered for the HeaderValue {2}, and cannot be " +
                        "overloaded with the specialized FieldValue {3}", eventTypeName, FieldValue
                        .EMPTY_FIELD_VALUE, headerValue, fieldValue));
            } else if (!fields.keySet().isEmpty() && !fields.containsKey(FieldValue.EMPTY_FIELD_VALUE) && fieldValue
                    .equals(FieldValue.EMPTY_FIELD_VALUE)) {
                /*
                 * Check if the FieldValue to add is the EMPTY_FIELD_VALUE. If so, the fields map should be empty, or
                 * only contain a single EMPTY_FIELD_VALUE entry, because EMPTY_FIELD_VALUE cannot be registered with
                 * specialized FieldValues.
                 * Note that this condition allows to register the same EventTypeName to EMPTY_FIELD_VALUE multiple
                 * times (the duplicate insertion is handled in the next conditions).
                 */
                throw new JarvisException(MessageFormat.format("Cannot register the provided EventType {0}, a " +
                        "specialized FieldValue {1} is already registered for the HeaderValue {2}, and cannot be used" +
                        " along with the empty FieldValue {3}", eventTypeName, fieldValue, headerValue, FieldValue
                        .EMPTY_FIELD_VALUE));
            }
            if (fields.containsKey(fieldValue)) {
                if (fields.get(fieldValue).equals(eventTypeName)) {
                    Log.info("EventType {0} already associated to the pair ({1}, {2})", eventTypeName, headerValue
                            .toString(), fieldValue.toString());
                } else {
                    throw new JarvisException(MessageFormat.format("Cannot register the provided EventType {0}, the " +
                                    "pair ({1}, {2}) is already matched to {3}", eventTypeName, headerValue.toString(),
                            fieldValue.toString(), fields.get(fieldValue)));
                }
            } else {
                Log.info("Registering EventType {0} to the pair ({1}, {2})", eventTypeName, headerValue.toString
                        (), fieldValue.toString());
                fields.put(fieldValue, eventTypeName);
            }
        } else {
            Log.info("Registering EventType {0} to the pair ({1}, {2})", eventTypeName, headerValue.toString
                    (), fieldValue.toString());
            Map<FieldValue, String> fields = new HashMap<>();
            fields.put(fieldValue, eventTypeName);
            matchableEvents.put(headerValue, fields);
        }
    }

    /**
     * Matches the provided {@code headers} and {@code content} against the registered {@link EventInstance}s.
     * <p>
     * This method first iterates the provided {@code headers} and look for each registered one if the provided
     * {@code content} contains a registered field. If at least one {@code header} and one field of the {@code
     * content} are matched the corresponding {@link EventInstance} is returned.
     * <p>
     * <p>Note:</p> the current implementation only check top-level field from the provided {@code content}. (see #139)
     *
     * @param headers the array containing the {@link Header}s to match
     * @param content the {@link JsonElement} representing the content of the request
     * @return the matched {@link EventInstance} if it exists, {@code null} otherwise
     * @throws NullPointerException if the provided {@code headers} or {@code content} is {@code null}
     */
    public EventInstance match(Header[] headers, JsonElement content) {
        checkNotNull(headers, "Cannot match the provided headers %s", headers);
        checkNotNull(content, "Cannot match the provided content %s", content);
        /*
         * Iterate first on the request headers that are typically shorter than its content.
         */
        for (int i = 0; i < headers.length; i++) {
            HeaderValue headerValue = HeaderValue.of(headers[i].getName(), headers[i].getValue());
            if (matchableEvents.containsKey(headerValue)) {
                Map<FieldValue, String> fields = matchableEvents.get(headerValue);
                /*
                 * Iterate from the key set instead of the content: the key set usually contains less registered
                 * fields than the document.
                 */
                for (FieldValue fieldValue : fields.keySet()) {
                    if (fieldValue.equals(FieldValue.EMPTY_FIELD_VALUE)) {
                        /*
                         * Directly match the event associated to the EMPTY_FIELD_VALUE. The addMatchableEvent method
                         * ensures that this FieldValue is unique.
                         */
                        return createEventInstance(fields.get(FieldValue.EMPTY_FIELD_VALUE), content);
                    } else {
                        JsonElement jsonValue = content.getAsJsonObject().get(fieldValue.getKey());
                        /*
                         * If jsonValue is not null the field corresponding to fieldValue.getKey() exists.
                         */
                        if (nonNull(jsonValue) && jsonValue.getAsString().equals(fieldValue.getValue())) {
                            return createEventInstance(fields.get(fieldValue), content);
                        }
                    }
                }
            }
        }
        Log.warn("Cannot find an EventDefinition matching the provided headers and content");
        /*
         * We should probably provide some debug information here to help building the EventDefinition to match.
         */
        return null;
    }

    /**
     * Creates an {@link EventInstance} from the provided {@code eventDefinitionName} and {@code content}.
     * <p>
     * The built {@link EventInstance} is associated to the {@link EventDefinition} matching the given {@code
     * eventDefinitionName} if it exists, and its out contexts are set with the fields of the provided {@code
     * content} {@link JsonElement}.
     * <p>
     * Note that the built {@link EventInstance} contains all the fields containing literal values of the provided
     * {@code content}, formatted by the {@link #convertJsonObjectToOutContext(JsonObject, EventInstanceBuilder)}
     * method.
     *
     * @param eventDefinitionName the name of the {@link EventDefinition} to create an
     *                            {@link EventInstance} from
     * @param content             the {@link JsonElement} to set as the created {@link EventInstance} out context
     * @return an {@link EventInstance} matching the provided {@code eventDefinitionName} and containing the {@code
     * content} out context values.
     * @throws NullPointerException if the provided {@code eventDefinitionName} or {@code content} is {@code null}
     * @see #convertJsonObjectToOutContext(JsonObject, EventInstanceBuilder)
     */
    protected EventInstance createEventInstance(String eventDefinitionName, JsonElement content) {
        checkNotNull(eventDefinitionName, "Cannot create an %s from the provided %s name %s", EventInstance.class
                .getSimpleName(), EventDefinition.class.getSimpleName(), eventDefinitionName);
        checkNotNull(content, "Cannot create an %s from the provided %s content %s", EventInstance.class
                .getSimpleName(), JsonElement.class.getSimpleName(), content);
        eventInstanceBuilder.clear();
        eventInstanceBuilder.setEventDefinitionName(eventDefinitionName);
        convertJsonObjectToOutContext(content.getAsJsonObject(), eventInstanceBuilder);
        if (printBuilder) {
            Log.info("{0}", eventInstanceBuilder.prettyPrintEventDefinition());
        }
        return eventInstanceBuilder.build();
    }

    /**
     * Converts the provided {@code jsonObject}'s fields into out context values set in the provided {@code builder}.
     * <p>
     * This method formats the {@code jsonObject} fields following this pattern:
     * <ul>
     * <li>Top-level field names are reused to set out context keys (e.g. {@code "content"})</li>
     * <li>Contained field names are prefixed by {@code '_'}, followed by their containing field name (e.g. {@code
     * "content_field1")}</li>
     * <li>Only fields containing literal values are set in the {@link EventInstance}'s out context</li>
     * </ul>
     * <p>
     * This method is a convenience wrapper for
     * {@link #convertJsonObjectToOutContext(String, JsonObject, EventInstanceBuilder)} with
     * the empty string as its first parameter.
     * <p>
     * <b>Note:</b> this method does not handle {@link com.google.gson.JsonArray}s for now (see #141)
     *
     * @param jsonObject the {@link JsonObject} to extract the fields from
     * @param builder    the {@link EventInstanceBuilder} used to set out context values
     * @throws NullPointerException if the provided {@code jsonObject} or {@code builder} is {@code null}
     * @see #convertJsonObjectToOutContext(String, JsonObject, EventInstanceBuilder)
     */
    protected void convertJsonObjectToOutContext(JsonObject jsonObject, EventInstanceBuilder builder) {
        checkNotNull(jsonObject, "Cannot convert the fields of the provided %s %s", JsonObject.class.getSimpleName(),
                jsonObject);
        checkNotNull(builder, "Cannot set the out context values using the provided %s %s", EventInstanceBuilder
                .class.getSimpleName(), builder);
        convertJsonObjectToOutContext("", jsonObject, builder);
    }

    /**
     * Converts the provided {@code jsonObject}'s field into out context values set in the provided {@code builder}.
     * <p>
     * This method formats the {@code jsonObject} fields following this pattern:
     * <ul>
     * <li>Top-level field names are reused to set out context keys (e.g. {@code "content"})</li>
     * <li>Contained field names are prefixed by {@code '_'}, followed by their containing field name (e.g.
     * {@code "content_field1")}</li>
     * <li>Only fields containing literal values are set in the {@link EventInstance}'s out context</li>
     * </ul>
     * <p>
     * <b>Note:</b> this method does not handle {@link com.google.gson.JsonArray}s for now (see #141)
     *
     * @param parentKey  the key of the parent of the provided {@code jsonObject}
     * @param jsonObject the {@link JsonObject} to extract the fields from
     * @param builder    the {@link EventInstanceBuilder} used to set out context values
     * @throws NullPointerException if the provided {@code parentKey}, {@code jsonObject}, or {@code builder} is {@code
     *                              null}
     */
    protected void convertJsonObjectToOutContext(String parentKey, JsonObject jsonObject, EventInstanceBuilder
            builder) {
        checkNotNull(parentKey, "Cannot convert the fields of the provided %s using the given parent key %s",
                JsonObject.class.getSimpleName(), parentKey);
        checkNotNull(jsonObject, "Cannot convert the fields of the provided %s %s", JsonObject.class.getSimpleName(),
                jsonObject);
        checkNotNull(builder, "Cannot set the out context values using the provided %s %s", EventInstanceBuilder
                .class.getSimpleName(), builder);
        for (String key : jsonObject.keySet()) {
            JsonElement value = jsonObject.get(key);
            String newKey;
            if (parentKey.isEmpty()) {
                newKey = key.toLowerCase();
            } else {
                newKey = parentKey + "->" + key.toLowerCase();
            }
            if (value.isJsonObject()) {
                convertJsonObjectToOutContext(newKey, value.getAsJsonObject(), builder);
            } else if (value.isJsonArray()) {
                Log.warn("Json Arrays are not handled by the EventMatcher for now, setting the out context {0} with " +
                        "a placeholder String", newKey);
                builder.setOutContextValue(newKey, "[array]");
            } else if (value.isJsonPrimitive()) {
                builder.setOutContextValue(newKey, value.getAsString());
            } else if (value.isJsonNull()) {
                Log.info("Null Json value, setting the out context {0} with an empty String", newKey);
                builder.setOutContextValue(newKey, "");
            }
        }
    }

    /**
     * A pair representing a {@link Header} value to match.
     *
     * @see #addMatchableEvent(HeaderValue, FieldValue, String)
     * @see #match(Header[], JsonElement)
     */
    public static class HeaderValue {

        /**
         * The key of the {@link Header} to match.
         */
        private String key;

        /**
         * The value of the {@link Header} to match.
         */
        private String value;

        /**
         * Constructs a new {@link HeaderValue} from the provided {@code key} and {@code value}.
         * <p>
         * This method is a shortcut for {@link #HeaderValue(String, String)}.
         *
         * @param key   the key of the {@link Header} to match
         * @param value the value of the {@link Header} to match
         * @return the created {@link HeaderValue}
         * @throws NullPointerException if the provided {@code key} or {@code value} is {@code null}
         * @see #HeaderValue(String, String)
         */
        public static HeaderValue of(String key, String value) {
            return new HeaderValue(key, value);
        }

        /**
         * Constructs a new {@link HeaderValue} from the provided {@code key} and {@code value}.
         *
         * @param key   the key of the {@link Header} to match
         * @param value the value of the {@link Header} to match
         * @throws NullPointerException if the provided {@code key} or {@code value} is {@code null}
         * @see #of(String, String)
         */
        public HeaderValue(String key, String value) {
            checkNotNull(key, "Cannot build a %s with the provided key %s", HeaderValue.class.getSimpleName(), key);
            checkNotNull(value, "Cannot build a %s with the provided value %s", HeaderValue.class.getSimpleName(),
                    value);
            this.key = key;
            this.value = value;
        }

        /**
         * Returns the key of the {@link HeaderValue}.
         *
         * @return the key of the {@link HeaderValue}
         */
        public String getKey() {
            return this.key;
        }

        /**
         * Returns the value of the {@link HeaderValue}.
         *
         * @return the value of the {@link HeaderValue}
         */
        public String getValue() {
            return this.value;
        }

        /**
         * Checks if the provided {@code obj} is equal to this {@link HeaderValue}.
         * <p>
         * Two {@link HeaderValue}s are equal if their {@code keys} and {@code values} are respectively equal.
         *
         * @param obj the {@link Object} to check
         * @return {@code true} if the provided {@code obj} is equal to this {@link HeaderValue}, {@code false}
         * otherwise
         */
        @Override
        public boolean equals(Object obj) {
            if (isNull(obj) || !(obj instanceof HeaderValue)) {
                return false;
            } else {
                return key.equals(((HeaderValue) obj).key) && value.equals(((HeaderValue) obj).value);
            }
        }

        /**
         * Computes the hash code associated to this {@link HeaderValue}.
         * <p>
         * {@link HeaderValue}'s hash code is computed by summing the hash codes of its key and value ({@code key
         * .hashCode() + value.hashCode()}.
         *
         * @return the hash code associated to this {@link HeaderValue}
         * @see String#hashCode()
         */
        @Override
        public int hashCode() {
            return key.hashCode() + value.hashCode();
        }

        /**
         * Returns a {@link String} representation of this {@link HeaderValue}.
         * <p>
         * The returned {@link String} follows the pattern {@code "Header[key]=value"}.
         *
         * @return a {@link String} representation of this {@link HeaderValue}
         */
        @Override
        public String toString() {
            return MessageFormat.format("Header[{0}]={1}", key, value);
        }
    }

    /**
     * A pair representing a field value to match.
     *
     * @see #addMatchableEvent(HeaderValue, FieldValue, String)
     * @see #match(Header[], JsonElement)
     */
    public static class FieldValue {

        /**
         * The key of the field to match.
         */
        private String key;

        /**
         * The value of the field to match.
         */
        private String value;

        /**
         * Constructs a new {@link FieldValue} from the provided {@code key} and {@code value}.
         * <p>
         * This method is a shortcut for {@link #FieldValue(String, String)}.
         *
         * @param key   the key of the field to match
         * @param value the value of the field to match
         * @return the created {@link FieldValue}
         * @throws NullPointerException if the provided {@code key} or {@code value} is {@code null}
         * @see #FieldValue(String, String)
         */
        public static FieldValue of(String key, String value) {
            return new FieldValue(key, value);
        }

        /**
         * A static {@link FieldValue} used to match requests without inspecting its Json fields.
         * <p>
         * This {@link FieldValue} should be used when the {@link HeaderValue} provided in
         * {@link #addMatchableEvent(HeaderValue, FieldValue, String)} is sufficient to uniquely identify an event.
         *
         * @see #addMatchableEvent(HeaderValue, FieldValue, String)
         * @see #match(Header[], JsonElement)
         */
        public static FieldValue EMPTY_FIELD_VALUE = of("", "");

        /**
         * Constructs a new {@link FieldValue} from the provided {@code key} and {@code value}.
         *
         * @param key   the key of the field to match
         * @param value the value of the field to match
         * @throws NullPointerException if the provided {@code key} or {@code value} is {@code null}
         * @see #of(String, String)
         */
        public FieldValue(String key, String value) {
            checkNotNull(key, "Cannot build a %s with the provided key", FieldValue.class.getSimpleName(), key);
            checkNotNull(value, "Cannot build a %s with the provided value", FieldValue.class.getSimpleName(), value);
            this.key = key;
            this.value = value;
        }

        /**
         * Returns the key of the {@link FieldValue}.
         *
         * @return the key of the {@link FieldValue}
         */
        public String getKey() {
            return this.key;
        }

        /**
         * Returns the value of the {@link FieldValue}.
         *
         * @return the value of the {@link FieldValue}
         */
        public String getValue() {
            return this.value;
        }

        /**
         * Checks if the provided {@code obj} is equal to this {@link FieldValue}.
         * <p>
         * Two {@link FieldValue}s are equal if their {@code keys} and {@code values} are respectively equal.
         *
         * @param obj the {@link Object} to check
         * @return {@code true} if the provided {@code obj} is equal to this {@link FieldValue}, {@code false} otherwise
         */
        @Override
        public boolean equals(Object obj) {
            if (isNull(obj) || !(obj instanceof FieldValue)) {
                return false;
            } else {
                return key.equals(((FieldValue) obj).key) && value.equals(((FieldValue) obj).value);
            }
        }

        /**
         * Computes the hash code associated to this {@link FieldValue}.
         * <p>
         * {@link FieldValue}'s hash code is computed by summing the hash codes of its key and value ({@code key
         * .hashCode() + value.hashCode()}.
         *
         * @return the hash code associated to this {@link FieldValue}
         * @see String#hashCode()
         */
        @Override
        public int hashCode() {
            return key.hashCode() + value.hashCode();
        }

        /**
         * Returns a {@link String} representation of this {@link FieldValue}.
         * <p>
         * The returned {@link String} follows the pattern {@code "Field[key]=value"}.
         *
         * @return a {@link String} representation of this {@link FieldValue}
         */
        @Override
        public String toString() {
            return MessageFormat.format("Field[{0}]={1}", key, value);
        }
    }
}
