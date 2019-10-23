package com.xatkit.core.platform.io;

import com.xatkit.core.EventDefinitionRegistry;
import com.xatkit.core.XatkitException;
import com.xatkit.intent.Context;
import com.xatkit.intent.ContextInstance;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.ContextParameterValue;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.EventInstance;
import com.xatkit.intent.IntentFactory;
import fr.inria.atlanmod.commons.log.Log;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A fluent {@link EventInstance} builder.
 * <p>
 * This class eases the creation of new {@link EventInstance}s, and provides a fluent API to set the
 * {@link EventDefinition} and output context parameter values. The builder checks that the created
 * {@link EventInstance}s are valid (i.e. they are associated to a registered {@link EventDefinition} and their
 * output context parameter values correspond to existing {@link ContextParameter}s.
 */
public class EventInstanceBuilder {

    /**
     * Creates a new {@link EventInstanceBuilder} from the provided {@code registry}.
     * <p>
     * The provided {@link EventDefinitionRegistry} is used to validate the {@link EventDefinition} name provided by
     * {@link #setEventDefinitionName(String)}, and ensure that the created {@link EventInstance} is bound to a
     * registered {@link EventDefinition}.
     *
     * @param registry the {@link EventDefinitionRegistry} used to validate the {@link EventInstance}'s definition name
     * @return the created {@link EventInstanceBuilder}
     * @throws NullPointerException if the provided {@code registry} is {@code null}
     */
    public static EventInstanceBuilder newBuilder(EventDefinitionRegistry registry) {
        return new EventInstanceBuilder(registry);
    }

    /**
     * The {@link EventDefinitionRegistry} used to retrieve the {@link EventDefinition} represented by the name
     * provided by {@link #setEventDefinitionName(String)}.
     *
     * @see #setEventDefinitionName(String)
     */
    private EventDefinitionRegistry registry;

    /**
     * The name of the {@link EventDefinition} to bind to the created {@link EventInstance}.
     *
     * @see #setEventDefinitionName(String)
     */
    private String eventDefinitionName;

    /**
     * The output context values to set to the created {@link EventInstance}.
     *
     * @see #setOutContextValue(String, String)
     */
    private Map<String, String> contextValues;

    /**
     * Disables the default constructor, use {@link #newBuilder(EventDefinitionRegistry)} instead.
     */
    private EventInstanceBuilder() {

    }

    /**
     * Constructs a new {@link EventInstanceBuilder} from the provided {@code registry}.
     * <p>
     * The provided {@link EventDefinitionRegistry} is used to validate the {@link EventDefinition} name provided by
     * {@link #setEventDefinitionName(String)}, and ensure that the created {@link EventInstance} is bound to a
     * registered {@link EventDefinition}.
     * <p>
     * <b>Note:</b> this constructor is private, use {@link #newBuilder(EventDefinitionRegistry)} to get a new
     * instance of this class
     *
     * @param registry the {@link EventDefinitionRegistry} used to validate the {@link EventInstance}'s definition name
     * @throws NullPointerException if the provided {@code registry} is {@code null}
     * @see #newBuilder(EventDefinitionRegistry)
     */
    private EventInstanceBuilder(EventDefinitionRegistry registry) {
        checkNotNull(registry, "Cannot create a %s with a null %s", EventInstanceBuilder.class.getSimpleName(),
                EventDefinitionRegistry.class.getSimpleName());
        this.registry = registry;
        this.contextValues = new HashMap<>();
    }

    /**
     * Sets the name of the {@link EventDefinition} to bind to the created {@link EventInstance}.
     * <p>
     * The provided {@code eventDefinitionName} must match an existing {@link EventDefinition} name in the provided
     * {@link EventDefinitionRegistry}.
     *
     * @param eventDefinitionName the name of the {@link EventDefinition} to bind to the created {@link EventInstance}
     * @return the builder
     * @throws NullPointerException if the provided {@code eventDefinitionName} is {@code null}
     */
    public EventInstanceBuilder setEventDefinitionName(String eventDefinitionName) {
        checkNotNull(eventDefinitionName, "Cannot construct an %s from a null %s", EventInstance.class.getSimpleName
                (), EventDefinition.class.getSimpleName());
        this.eventDefinitionName = eventDefinitionName;
        return this;
    }

    /**
     * Returns the name of the {@link EventDefinition} to bind to the created {@link EventInstance}.
     *
     * @return the name of the {@link EventDefinition} to bind to the created {@link EventInstance}
     */
    public String getEventDefinitionName() {
        return this.eventDefinitionName;
    }

    /**
     * Sets the created {@link EventInstance}'s output context parameter {@code contextKey} with the given {@code
     * contextValue}.
     * <p>
     * The provided {@code contextKey} should match an existing {@link ContextParameter} name in the associated
     * {@link EventDefinition}.
     *
     * @param contextKey   the output context parameter name
     * @param contextValue the output context parameter value
     * @return the builder
     * @throws NullPointerException if the provided {@code contextKey} or {@code contextValue} is {@code null}
     */
    public EventInstanceBuilder setOutContextValue(String contextKey, String contextValue) {
        checkNotNull(contextKey, "Cannot set the out context key %s", contextValue);
        checkNotNull(contextValue, "Cannot set the out context value %s", contextValue);
        this.contextValues.put(contextKey, contextValue);
        return this;
    }

    /**
     * Returns an unmodifiable {@link Map} containing the output context parameters to bind to the
     * {@link EventInstance}.
     *
     * @return an unmodifiable {@link Map} containing the output context parameters to bind to the {@link EventInstance}
     */
    public Map<String, String> getOutContextValues() {
        return Collections.unmodifiableMap(this.contextValues);
    }

    /**
     * Creates a new {@link EventInstance} from the provided information.
     * <p>
     * This method validates the provided information before creating the {@link EventInstance}. The provided
     * {@link EventDefinition} name is used to retrieve the associated {@link EventDefinition} from the {@code
     * registry}, and the {@link EventDefinition}'s {@link ContextParameter}s are processed and matched against the
     * provided output context parameters.
     * <p>
     * <b>Note:</b> the builder will be cleared after returning the created {@link EventInstance} in order to allow
     * multiple {@link EventInstance} creations from the same {@link EventInstanceBuilder} (see {@link #clear()}).
     *
     * @return the created {@link EventInstance}
     * @throws XatkitException if there is no {@link EventDefinition} associated to the provided {@code name}, or if
     *                         the {@link EventDefinition} does not define the {@link ContextParameter}s representing
     *                         the provided ones
     * @see #setEventDefinitionName(String)
     * @see #setOutContextValue(String, String)
     * @see #clear()
     */
    public EventInstance build() {
        EventInstance eventInstance = IntentFactory.eINSTANCE.createEventInstance();
        EventDefinition eventDefinition = registry.getEventDefinition(eventDefinitionName);
        if (isNull(eventDefinition)) {
            String errorMessage = MessageFormat.format("Cannot build the EventInstance, the EventDefinition {0} does " +
                    "not exist", eventDefinitionName);
            throw new XatkitException(errorMessage);
        }
        eventInstance.setDefinition(eventDefinition);
        for (String contextKey : contextValues.keySet()) {

            Context context = null;
            for (Context outContext : eventDefinition.getOutContexts()) {
                if (nonNull(outContext.getContextParameter(contextKey))) {
                    context = outContext;
                }
            }
            if (isNull(context)) {
                /*
                 * Better log a warning than failing: the EventDefinition should contain the context parameter, but
                 * it may not be the case if the API recently evolved for example.
                 */
                Log.warn("Cannot retrieve the out context associated to the context parameter {0}", contextKey);
                continue;
            }
            /*
             * Retrieve the context instance bound to the retrieved context. Create it if it does not exist.
             */
            ContextInstance contextInstance = eventInstance.getOutContextInstance(context.getName());
            if (isNull(contextInstance)) {
                contextInstance = IntentFactory.eINSTANCE.createContextInstance();
                contextInstance.setDefinition(context);
                eventInstance.getOutContextInstances().add(contextInstance);
            }
            /*
             * Set the default lifespan here, we do not support custom lifespancounts at the instance level.
             */
            contextInstance.setLifespanCount(context.getLifeSpan());

            ContextParameter contextParameter = context.getContextParameter(contextKey);
            if (isNull(contextParameter)) {
                throw new XatkitException(MessageFormat.format("Cannot build the EventInstance, the " +
                        "EventDefinition {0} does not define the output context parameter {1}", eventDefinition
                        .getName(), contextKey));
            }
            ContextParameterValue contextParameterValue = IntentFactory.eINSTANCE.createContextParameterValue();
            contextParameterValue.setContextParameter(contextParameter);
            contextParameterValue.setValue(contextValues.get(contextKey));
            contextInstance.getValues().add(contextParameterValue);
        }
        this.clear();
        /*
         * Note: this method does not check that all the out context parameter have been filled with values (see
         * #142). This may be integrated in a future release based on the issue discussions.
         */
        return eventInstance;
    }

    /**
     * Clears the builder and reset its internal fields.
     * <p>
     * <b>Note:</b> this method is automatically called after calling {@link #build()}.
     */
    public void clear() {
        this.eventDefinitionName = null;
        this.contextValues = new HashMap<>();
    }

    /**
     * Prints a pretty representation of the current {@link EventDefinition} to instantiate with this builder.
     * <p>
     * This method provides a human-readable view of the builder content that is used for debugging purposes.
     *
     * @return a pretty representation of the current {@link EventDefinition} to instantiate with this builder
     */
    public String prettyPrintEventDefinition() {
        StringBuilder sb = new StringBuilder();
        sb.append("event ");
        sb.append(isNull(this.eventDefinitionName) ? "null" : this.eventDefinitionName).append("\n");
        /*
         * The out context name is not known in the builder, it is not required to set context parameter values.
         */
        sb.append("creates context \"unknown\" {\n");
        for (String contextKey : contextValues.keySet()) {
            sb.append("\tsets parameter \"").append(contextKey).append("\"\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

}
