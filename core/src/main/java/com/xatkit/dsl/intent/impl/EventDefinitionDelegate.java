package com.xatkit.dsl.intent.impl;

import com.xatkit.dsl.intent.EventContextLifespanStep;
import com.xatkit.dsl.intent.EventContextStep;
import com.xatkit.intent.Context;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.IntentFactory;
import lombok.NonNull;

public class EventDefinitionDelegate extends EventDefinitionProviderImpl implements EventContextStep {

    public EventDefinitionDelegate(@NonNull EventDefinition event) {
        super(event);
    }

    @Override
    public @NonNull EventContextLifespanStep context(@NonNull String name) {
        Context context = IntentFactory.eINSTANCE.createContext();
        context.setName(name);
        this.event.getOutContexts().add(context);
        return new EventContextDelegate(this.event, context);
    }
}
