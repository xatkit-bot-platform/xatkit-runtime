package com.xatkit.dsl.intent.impl;

import com.xatkit.dsl.intent.EventContextLifespanStep;
import com.xatkit.dsl.intent.EventContextParameterStep;
import com.xatkit.intent.Context;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.IntentFactory;
import lombok.NonNull;

public class EventContextBuilder extends EventDefinitionProviderImpl implements EventContextLifespanStep {

    protected Context context;

    public EventContextBuilder(@NonNull EventDefinition parent) {
        this.event = parent;
        this.context = IntentFactory.eINSTANCE.createContext();
        /*
         * Add the context right now because we don't know what will be the last call to define it (we can have
         * multiple parameters in a context).
         */
        this.event.getOutContexts().add(this.context);
    }

    public @NonNull EventContextBuilder name(@NonNull String name) {
        this.context.setName(name);
        return this;
    }


    @Override
    public @NonNull EventContextParameterStep lifespan(int lifespan) {
        this.context.setLifeSpan(lifespan);
        return this;
    }

    @Override
    public @NonNull EventContextParameterStep parameter(@NonNull String name) {
        ContextParameter parameter = IntentFactory.eINSTANCE.createContextParameter();
        parameter.setName(name);
        this.context.getParameters().add(parameter);
        return this;
    }

    @Override
    public @NonNull EventContextLifespanStep context(@NonNull String name) {
        /*
         * Create a new builder with the created context.
         */
        EventContextBuilder eventContextBuilder = new EventContextBuilder(this.event);
        eventContextBuilder.name(name);
        return eventContextBuilder;
    }
}
