package com.xatkit.dsl.intent.impl;

import com.xatkit.dsl.intent.EventContextLifespanStep;
import com.xatkit.dsl.intent.EventContextParameterStep;
import com.xatkit.intent.Context;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.IntentFactory;
import lombok.NonNull;

public class EventContextDelegate extends EventDefinitionProviderImpl implements EventContextLifespanStep {

    protected Context context;

    public EventContextDelegate(@NonNull EventDefinition event, @NonNull Context context) {
        super(event);
        this.context = context;
    }


    @Override
    public EventContextParameterStep lifespan(int lifespan) {
        this.context.setLifeSpan(lifespan);
        return this;
    }

    @Override
    public EventContextParameterStep parameter(String name) {
        ContextParameter parameter = IntentFactory.eINSTANCE.createContextParameter();
        parameter.setName(name);
        this.context.getParameters().add(parameter);
        return this;
    }

    @Override
    public EventContextLifespanStep context(String name) {
        Context context = IntentFactory.eINSTANCE.createContext();
        context.setName(name);
        this.event.getOutContexts().add(context);
        /*
         * Create a new delegate with the created context
         */
        return new EventContextDelegate(this.event, context);
    }
}
