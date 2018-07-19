package fr.zelus.jarvis.io;

import fr.zelus.jarvis.core.EventDefinitionRegistry;
import fr.zelus.jarvis.core.JarvisException;
import fr.zelus.jarvis.intent.*;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;

public class EventInstanceBuilder {

    public static EventInstanceBuilder newBuilder(EventDefinitionRegistry registry) {
        return new EventInstanceBuilder(registry);
    }

    private EventDefinitionRegistry registry;

    private String eventDefinitionName;

    private Map<String, String> contextValues;

    private EventInstanceBuilder(EventDefinitionRegistry registry) {
        checkNotNull(registry, "Cannot create a %s with a null %s", EventInstanceBuilder.class.getSimpleName(),
                EventDefinitionRegistry.class.getSimpleName());
        this.registry = registry;
        this.contextValues = new HashMap<>();
    }

    public EventInstanceBuilder setEventDefinitionName(String eventDefinitionName) {
        checkNotNull(eventDefinitionName, "Cannot construct an %s from a null %s", EventInstance.class.getSimpleName
                (), EventDefinition.class.getSimpleName());
        this.eventDefinitionName = eventDefinitionName;
        return this;
    }

    public String getEventDefinitionName() {
        return this.eventDefinitionName;
    }

    public EventInstanceBuilder setOutContextValue(String contextKey, String contextValue) {
        checkNotNull(contextKey, "Cannot set the out context value %s with the key null", contextValue);
        checkNotNull(contextValue, "Cannot set the out context value null");
        this.contextValues.put(contextKey, contextValue);
        return this;
    }

    public Map<String, String> getOutContextValues() {
        return Collections.unmodifiableMap(this.contextValues);
    }

    public EventInstance build() {
        EventInstance eventInstance = IntentFactory.eINSTANCE.createEventInstance();
        EventDefinition eventDefinition = registry.getEventDefinition(eventDefinitionName);
        if (isNull(eventDefinition)) {
            String errorMessage = MessageFormat.format("Cannot build the EventInstance, the EventDefinition {0} does " +
                    "not exist", eventDefinitionName);
            throw new JarvisException(errorMessage);
        }
        eventInstance.setDefinition(eventDefinition);
        for (String contextKey : contextValues.keySet()) {
            ContextParameter contextParameter = getContextParameter(eventDefinition, contextKey);
            if (isNull(contextParameter)) {
                String errorMessage = MessageFormat.format("Cannot build the EventInstance, the EventDefinition {0} " +
                                "does not define the output context parameter value {1}", eventDefinition.getName(),
                        contextKey);
                throw new JarvisException(errorMessage);
            }
            ContextParameterValue contextParameterValue = IntentFactory.eINSTANCE.createContextParameterValue();
            contextParameterValue.setContextParameter(contextParameter);
            contextParameterValue.setValue(contextValues.get(contextKey));
            eventInstance.getOutContextValues().add(contextParameterValue);
        }
        return eventInstance;
    }

    private ContextParameter getContextParameter(EventDefinition eventDefinition, String parameterName) {
        for (Context c : eventDefinition.getOutContexts()) {
            for (ContextParameter cp : c.getParameters()) {
                if (cp.getName().equals(parameterName)) {
                    return cp;
                }
            }
        }
        throw new JarvisException("Cannot find the parameter " + parameterName);
    }
}
