package com.xatkit.dsl.intent.impl;

import com.xatkit.dsl.entity.EntityDefinitionReferenceProvider;
import com.xatkit.dsl.intent.IntentContextParameterEntityStep;
import com.xatkit.dsl.intent.IntentContextParameterFragmentStep;
import com.xatkit.dsl.intent.IntentContextParameterStep;
import com.xatkit.intent.Context;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.EntityDefinitionReference;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.IntentFactory;
import lombok.NonNull;

public class IntentContextParameterBuilder extends IntentDefinitionProviderImpl implements IntentContextParameterFragmentStep,
        IntentContextParameterEntityStep {

    private Context context;

    private ContextParameter parameter;

    public IntentContextParameterBuilder(@NonNull IntentDefinition parentIntent, @NonNull Context parentContext) {
        this.intent = parentIntent;
        this.context = parentContext;
        this.parameter = IntentFactory.eINSTANCE.createContextParameter();
    }

    public @NonNull IntentContextParameterBuilder name(@NonNull String name) {
        this.parameter.setName(name);
        return this;
    }

    @Override
    public @NonNull IntentContextParameterStep entity(@NonNull EntityDefinitionReferenceProvider entityReferenceProvider) {
        return this.entity(entityReferenceProvider.getEntityReference());
    }

    @Override
    public @NonNull IntentContextParameterStep entity(@NonNull EntityDefinitionReference entityReference) {
        this.parameter.setEntity(entityReference);
        this.context.getParameters().add(this.parameter);
        return new IntentContextBuilder(this.intent, this.context);
    }

    @Override
    public @NonNull IntentContextParameterEntityStep fromFragment(@NonNull String fragment) {
        this.parameter.setTextFragment(fragment);
        return this;
    }
}
