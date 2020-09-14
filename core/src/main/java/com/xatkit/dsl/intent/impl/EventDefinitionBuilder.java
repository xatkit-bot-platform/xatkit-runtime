package com.xatkit.dsl.intent.impl;

import com.xatkit.dsl.intent.EventContextLifespanStep;
import com.xatkit.dsl.intent.EventContextStep;
import com.xatkit.intent.IntentFactory;
import lombok.NonNull;

public class EventDefinitionBuilder extends EventDefinitionProviderImpl implements EventContextStep {

    public EventDefinitionBuilder() {
        this.event = IntentFactory.eINSTANCE.createEventDefinition();
    }

    public @NonNull EventDefinitionBuilder name(@NonNull String name) {
        this.event.setName(name);
        return this;
    }

    @Override
    public @NonNull EventContextLifespanStep context(@NonNull String name) {
        EventContextBuilder eventContextBuilder = new EventContextBuilder(this.event);
        eventContextBuilder.name(name);
        return eventContextBuilder;
    }
}
