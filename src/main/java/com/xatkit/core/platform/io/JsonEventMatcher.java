package com.xatkit.core.platform.io;

import com.google.gson.JsonElement;
import com.xatkit.core.XatkitException;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.ContextParameterValue;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.EventInstance;
import com.xatkit.intent.IntentFactory;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;
import org.apache.http.Header;

import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A matcher that creates {@link EventInstance}s from Json webhook calls.
 * <p>
 * This class is configured through the {@link #addMatchableEvent(HeaderValue, FieldValue, EventDefinition)} method,
 * that allows to specify an {@link EventInstance} type for a given header and field content. Matched requests are
 * reified
 * into {@link EventInstance}s containing all the fields of the provided payload.
 */
public class JsonEventMatcher {

    /**
     * The internal {@link Map} used to store the header and field values to match.
     */
    protected Map<HeaderValue, Map<FieldValue, EventDefinition>> matchableEvents = new HashMap<>();

    /**
     * Registers a new {@link EventInstance} to match from the provided {@code headerValue} and {@code fieldValue}.
     *
     * @param headerValue     the {@link HeaderValue} to match the received request against
     * @param fieldValue      the {@link FieldValue} to match the received payload against
     * @param eventDefinition the type of the {@link EventInstance} to return
     * @throws NullPointerException if the provided {@code headerValue}, {@code fieldValue}, or {@code eventTypeName}
     *                              is {@code null}
     * @throws XatkitException      if the provided {@code headerValue} and {@code fieldValue} are already associated to
     *                              another {@code eventTypeName}, or if calling this method would associate the
     *                              {@link FieldValue#EMPTY_FIELD_VALUE} and specialized {@link FieldValue}s to the
     *                              provided {@code headerValue}.
     * @see HeaderValue
     * @see FieldValue
     * @see FieldValue#EMPTY_FIELD_VALUE
     */
    public void addMatchableEvent(@NonNull HeaderValue headerValue, @NonNull FieldValue fieldValue,
                                  @NonNull EventDefinition eventDefinition) {
        if (matchableEvents.containsKey(headerValue)) {
            Map<FieldValue, EventDefinition> fields = matchableEvents.get(headerValue);
            if (fields.containsKey(FieldValue.EMPTY_FIELD_VALUE) && !fieldValue.equals(FieldValue.EMPTY_FIELD_VALUE)) {
                /*
                 * Check if there is an EMPTY_FIELD_VALUE registered for the provided HeaderValue. If it is the case
                 * throw an exception: it is not possible to register specialized FieldValues along with the
                 * EMPTY_FIELD_VALUE: the engine will always match the EMPTY_FIELD_VALUE.
                 */
                throw new XatkitException(MessageFormat.format("Cannot register the provided EventType {0}, the empty"
                                + " FieldValue {1} is already registered for the HeaderValue {2}, and cannot be "
                                + "overloaded with the specialized FieldValue {3}", eventDefinition.getName(),
                        FieldValue.EMPTY_FIELD_VALUE, headerValue, fieldValue));
            } else if (!fields.keySet().isEmpty() && !fields.containsKey(FieldValue.EMPTY_FIELD_VALUE) && fieldValue
                    .equals(FieldValue.EMPTY_FIELD_VALUE)) {
                /*
                 * Check if the FieldValue to add is the EMPTY_FIELD_VALUE. If so, the fields map should be empty, or
                 * only contain a single EMPTY_FIELD_VALUE entry, because EMPTY_FIELD_VALUE cannot be registered with
                 * specialized FieldValues.
                 * Note that this condition allows to register the same EventTypeName to EMPTY_FIELD_VALUE multiple
                 * times (the duplicate insertion is handled in the next conditions).
                 */
                throw new XatkitException(MessageFormat.format("Cannot register the provided EventType {0}, a "
                                + "specialized FieldValue {1} is already registered for the HeaderValue {2}, and "
                                + "cannot be used along with the empty FieldValue {3}", eventDefinition.getName(),
                        fieldValue, headerValue, FieldValue.EMPTY_FIELD_VALUE));
            }
            if (fields.containsKey(fieldValue)) {
                if (fields.get(fieldValue).equals(eventDefinition)) {
                    Log.info("EventType {0} already associated to the pair ({1}, {2})", eventDefinition.getName(),
                            headerValue
                                    .toString(), fieldValue.toString());
                } else {
                    throw new XatkitException(MessageFormat.format("Cannot register the provided EventType {0}, the "
                                    + "pair ({1}, {2}) is already matched to {3}", eventDefinition.getName(),
                            headerValue.toString(), fieldValue.toString(), fields.get(fieldValue)));
                }
            } else {
                Log.info("Registering EventType {0} to the pair ({1}, {2})", eventDefinition.getName(),
                        headerValue.toString(), fieldValue.toString());
                fields.put(fieldValue, eventDefinition);
            }
        } else {
            Log.info("Registering EventType {0} to the pair ({1}, {2})", eventDefinition.getName(),
                    headerValue.toString(), fieldValue.toString());
            Map<FieldValue, EventDefinition> fields = new HashMap<>();
            fields.put(fieldValue, eventDefinition);
            matchableEvents.put(headerValue, fields);
        }
    }

    /**
     * Matches the provided {@code headers} and {@code content} and creates the corresponding {@link EventInstance}.
     * <p>
     * This method first iterates the provided {@code headers} and look for each registered one if the provided
     * {@code content} contains a registered field. If at least one {@code header} and one field of the {@code
     * content} are matched the corresponding {@link EventInstance} is returned.
     * <p>
     * The returned {@link EventInstance} contains the request {@code content} in its {@code json} context parameter.
     * Note that this method will throw an {@link IllegalArgumentException} if the {@link EventInstance}'s definition
     * doesn't define such context parameter.
     * <p>
     * <b>Note:</b> the current implementation only check top-level field from the provided {@code content}. (see
     * <a href="https://github.com/xatkit-bot-platform/xatkit-runtime/issues/139">#139</a>)
     *
     * @param headers the list containing the {@link Header}s to match
     * @param content the {@link JsonElement} representing the content of the request
     * @return the matched {@link EventInstance} if it exists, {@code null} otherwise
     * @throws NullPointerException if the provided {@code headers} or {@code content} is {@code null}
     */
    public @Nullable
    EventInstance match(@NonNull List<Header> headers, @NonNull JsonElement content) {
        /*
         * Iterate first on the request headers that are typically shorter than its content.
         */
        for (Header header : headers) {
            HeaderValue headerValue = HeaderValue.of(header.getName(), header.getValue());
            if (matchableEvents.containsKey(headerValue)) {
                Map<FieldValue, EventDefinition> fields = matchableEvents.get(headerValue);
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
        return null;
    }

    /**
     * Creates an {@link EventInstance} for the provided {@code eventDefinition} and sets its context with {@code
     * content}.
     * <p>
     * The returned {@link EventInstance} contains the request {@code content} in its {@code json} context parameter.
     * Note that this method will throw an {@link IllegalArgumentException} if the provided {@code
     * eventDefinition} doesn't define such context parameter.
     * <p>
     *
     * @param eventDefinition the {@link EventDefinition} to create an instance of
     * @param content         the {@link JsonElement} to set as the created {@link EventInstance} context
     * @return an {@link EventInstance} matching the provided {@code eventDefinitionName}, with a {@code data.json}
     * parameter containing the provided {@code content}
     * @throws NullPointerException     if the provided {@code eventDefinitionName} or {@code content} is {@code null}
     * @throws IllegalArgumentException if the provided {@code eventDefinition} does not define the {@code data.json}
     *                                  context parameter
     */
    protected @NonNull EventInstance createEventInstance(@NonNull EventDefinition eventDefinition,
                                                         @NonNull JsonElement content) {
        ContextParameter jsonParameter = eventDefinition.getParameter("json");
        checkArgument(nonNull(jsonParameter), "Cannot create the %s for the provided %s %s: the %s'json parameter "
                        + "does not exist", EventInstance.class.getSimpleName(),
                EventDefinition.class.getSimpleName(), eventDefinition.getName(),
                EventDefinition.class.getSimpleName());

        EventInstance eventInstance = IntentFactory.eINSTANCE.createEventInstance();
        eventInstance.setDefinition(eventDefinition);
        ContextParameterValue jsonParameterValue = IntentFactory.eINSTANCE.createContextParameterValue();
        jsonParameterValue.setContextParameter(jsonParameter);
        jsonParameterValue.setValue(content);
        eventInstance.getValues().add(jsonParameterValue);
        return eventInstance;
    }

    /**
     * A pair representing a {@link Header} value to match.
     *
     * @see #addMatchableEvent(HeaderValue, FieldValue, EventDefinition)
     * @see #match(List, JsonElement)
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
            this.key = key.toLowerCase();
            this.value = value.toLowerCase();
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
     * @see #addMatchableEvent(HeaderValue, FieldValue, EventDefinition)
     * @see #match(List, JsonElement)
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
         * {@link #addMatchableEvent(HeaderValue, FieldValue, EventDefinition)} is sufficient to uniquely identify an
         * event.
         *
         * @see #addMatchableEvent(HeaderValue, FieldValue, EventDefinition)
         * @see #match(List, JsonElement)
         */
        public static final FieldValue EMPTY_FIELD_VALUE = of("", "");

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
            this.key = key.toLowerCase();
            this.value = value.toLowerCase();
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
