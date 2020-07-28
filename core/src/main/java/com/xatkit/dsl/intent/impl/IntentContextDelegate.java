package com.xatkit.dsl.intent.impl;

import com.xatkit.dsl.intent.IntentContextLifespanStep;
import com.xatkit.dsl.intent.IntentContextParameterFragmentStep;
import com.xatkit.dsl.intent.IntentContextParameterStep;
import com.xatkit.intent.Context;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.IntentFactory;
import lombok.NonNull;

public class IntentContextDelegate extends IntentDefinitionProviderImpl implements IntentContextLifespanStep {

    private Context context;

    public IntentContextDelegate(@NonNull IntentDefinition intent, @NonNull Context context) {
        super(intent);
        this.context = context;
    }

    @Override
    public IntentContextParameterStep lifespan(int lifespan) {
        this.context.setLifeSpan(lifespan);
        return this;
    }

    @Override
    public @NonNull IntentContextParameterFragmentStep parameter(@NonNull String parameterName) {
        ContextParameter parameter = IntentFactory.eINSTANCE.createContextParameter();
        parameter.setName(parameterName);
        this.context.getParameters().add(parameter);
        return new IntentContextParameterDelegate(this.intent, this.context, parameter);
    }

    @Override
    public @NonNull IntentContextLifespanStep context(@NonNull String name) {
        // TODO duplicated from intent definition delegate
        Context context = IntentFactory.eINSTANCE.createContext();
        context.setName(name);
        this.event.getOutContexts().add(context);
        /*
         * Create a new delegate with the created context
         */
        return new IntentContextDelegate(this.intent, context);
    }
}
