package com.xatkit.dsl.intent.impl;

import com.xatkit.dsl.entity.EntityDefinitionReferenceProvider;
import com.xatkit.dsl.intent.IntentContextParameterEntityStep;
import com.xatkit.dsl.intent.IntentContextParameterFragmentStep;
import com.xatkit.dsl.intent.IntentContextParameterStep;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.EntityDefinitionReference;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.IntentFactory;
import lombok.NonNull;

public class IntentContextParameterBuilder extends IntentDefinitionProviderImpl implements IntentContextParameterFragmentStep,
        IntentContextParameterEntityStep,
        IntentContextParameterStep {

    private ContextParameter parameter;

    public IntentContextParameterBuilder(@NonNull IntentDefinition parentIntent) {
        this.intent = parentIntent;
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
        this.intent.getParameters().add(this.parameter);
        return this;
    }

    @Override
    public @NonNull IntentContextParameterEntityStep fromFragment(@NonNull String fragment) {
        this.parameter.setTextFragment(fragment);
        return this;
    }

    @Override
    public @NonNull IntentContextParameterFragmentStep parameter(@NonNull String parameterName) {
        // Second parameter
        // TODO update this comment or delete it
        IntentContextParameterBuilder intentContextParameterBuilder = new IntentContextParameterBuilder(this.intent);
        intentContextParameterBuilder.name(parameterName);
        return intentContextParameterBuilder;
    }
}

