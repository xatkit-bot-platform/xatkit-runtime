package com.xatkit.dsl.intent.impl;

import com.xatkit.dsl.entity.EntityDefinitionReferenceProvider;
import com.xatkit.dsl.intent.IntentContextParameterEntityStep;
import com.xatkit.dsl.intent.IntentContextParameterFragmentStep;
import com.xatkit.dsl.intent.IntentContextParameterStep;
import com.xatkit.intent.Context;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.EntityDefinitionReference;
import com.xatkit.intent.IntentDefinition;
import lombok.NonNull;

public class IntentContextParameterDelegate extends IntentDefinitionProviderImpl implements IntentContextParameterFragmentStep,
        IntentContextParameterEntityStep {

    private Context context;

    private ContextParameter parameter;

    public IntentContextParameterDelegate(@NonNull IntentDefinition intent, @NonNull Context context,
                                          @NonNull ContextParameter parameter) {
        super(intent);
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
        return new IntentContextDelegate(this.intent, this.context);
    }

    @Override
    public @NonNull IntentContextParameterEntityStep fromFragment(@NonNull String fragment) {
        this.parameter.setTextFragment(fragment);
        return this;
    }
}
