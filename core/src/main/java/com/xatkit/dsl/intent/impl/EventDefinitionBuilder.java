package com.xatkit.dsl.intent.impl;

import com.xatkit.dsl.intent.EventContextParameterStep;
import com.xatkit.intent.Context;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.IntentFactory;
import lombok.NonNull;

import static java.util.Objects.isNull;

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
        Context context = this.event.getOutContext("XATKITCONTEXT");
        if(isNull(context)) {
            context = IntentFactory.eINSTANCE.createContext();
            context.setName("XATKITCONTEXT");
            this.event.getOutContexts().add(context);
        }
        ContextParameter contextParameter = IntentFactory.eINSTANCE.createContextParameter();
        contextParameter.setName(name);
        context.getParameters().add(contextParameter);
        return this;
    }
}
