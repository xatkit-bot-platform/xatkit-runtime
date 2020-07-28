package com.xatkit.dsl.intent.impl;

import com.xatkit.dsl.entity.EntityDefinitionReferenceProvider;
import com.xatkit.dsl.intent.IntentContextParameterEntityStep;
import com.xatkit.dsl.intent.IntentContextParameterFragmentStep;
import com.xatkit.dsl.intent.IntentContextParameterStep;
import com.xatkit.intent.Context;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.EntityDefinitionReference;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.IntentDefinition;
import lombok.NonNull;

import static fr.inria.atlanmod.commons.Preconditions.checkState;

public class ContextParameterDelegate extends EventDefinitionProviderImpl implements IntentContextParameterFragmentStep,
        IntentContextParameterEntityStep {

    private Context context;

    private ContextParameter parameter;

    public ContextParameterDelegate(@NonNull EventDefinition event, @NonNull Context context,
                                    @NonNull ContextParameter parameter) {
        super(event);
        this.context = context;
        this.parameter = parameter;
    }

    @Override
    public @NonNull IntentContextParameterStep entity(@NonNull EntityDefinitionReferenceProvider entityReferenceProvider) {
        return this.entity(entityReferenceProvider.getEntityReference());
    }

    @Override
    public @NonNull IntentContextParameterStep entity(@NonNull EntityDefinitionReference entityReference) {
        this.parameter.setEntity(entityReference);
        return new ContextDelegate(this.event, this.context);
    }

    @Override
    public @NonNull IntentContextParameterEntityStep fromFragment(@NonNull String fragment) {
        this.parameter.setTextFragment(fragment);
        return this;
    }

    @Override
    public IntentDefinition getIntentDefinition() {
        checkState(this.event instanceof IntentDefinition, "Cannot cast %s to %s",
                this.event.getClass().getSimpleName(), IntentDefinition.class.getSimpleName());
        return (IntentDefinition) this.event;
    }
}
