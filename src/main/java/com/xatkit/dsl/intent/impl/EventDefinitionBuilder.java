package com.xatkit.dsl.intent.impl;

import com.xatkit.dsl.intent.EventContextParameterStep;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.IntentFactory;
import lombok.NonNull;

public class EventDefinitionBuilder extends EventDefinitionProviderImpl implements EventContextParameterStep {

    public EventDefinitionBuilder() {
        this.event = IntentFactory.eINSTANCE.createEventDefinition();
    }

    public @NonNull EventDefinitionBuilder name(@NonNull String name) {
        this.event.setName(name);
        return this;
    }

    @Override
    public @NonNull EventContextParameterStep parameter(@NonNull String name) {
        ContextParameter contextParameter = IntentFactory.eINSTANCE.createContextParameter();
        contextParameter.setName(name);
        this.event.getParameters().add(contextParameter);
        return this;
    }
}
