package com.xatkit.dsl.intent.impl;

import com.xatkit.dsl.intent.IntentContextLifespanStep;
import com.xatkit.dsl.intent.IntentContextParameterFragmentStep;
import com.xatkit.dsl.intent.IntentContextParameterStep;
import com.xatkit.intent.Context;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.IntentFactory;
import lombok.NonNull;

public class IntentContextBuilder extends IntentDefinitionProviderImpl implements IntentContextLifespanStep {

    private Context context;

    public IntentContextBuilder(@NonNull IntentDefinition parent) {
        this.intent = parent;
        this.context = IntentFactory.eINSTANCE.createContext();
        /*
         * Add the context right now because we don't know what will be the last call to define it (we can have
         * multiple parameters in a context).
         */
        this.intent.getOutContexts().add(this.context);
    }

    public IntentContextBuilder(@NonNull IntentDefinition parent, @NonNull Context baseContext) {
        // TODO check this, we shouldn't pass the baseContext to the constructor.
        this.intent = parent;
        this.context = baseContext;
    }

    public @NonNull IntentContextBuilder name(@NonNull String name) {
        this.context.setName(name);
        return this;
    }

    @Override
    public @NonNull IntentContextParameterStep lifespan(int lifespan) {
        this.context.setLifeSpan(lifespan);
        return this;
    }

    @Override
    public @NonNull IntentContextParameterFragmentStep parameter(@NonNull String parameterName) {
        IntentContextParameterBuilder intentContextParameterBuilder = new IntentContextParameterBuilder(this.intent,
                this.context);
        intentContextParameterBuilder.name(parameterName);
        return intentContextParameterBuilder;
    }

    @Override
    public @NonNull IntentContextLifespanStep context(@NonNull String name) {
        /*
         * Create a new builder with the created context.
         */
        IntentContextBuilder intentContextBuilder = new IntentContextBuilder(this.intent);
        intentContextBuilder.name(name);
        return intentContextBuilder;
    }
}
