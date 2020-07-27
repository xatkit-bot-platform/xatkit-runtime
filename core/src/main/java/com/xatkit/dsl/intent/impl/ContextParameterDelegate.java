package com.xatkit.dsl.intent.impl;

import com.xatkit.dsl.entity.EntityDefinitionReferenceProvider;
import com.xatkit.dsl.intent.ContextParameterEntityStep;
import com.xatkit.dsl.intent.ContextParameterFragmentStep;
import com.xatkit.dsl.intent.ContextParameterStep;
import com.xatkit.intent.Context;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.EntityDefinitionReference;
import com.xatkit.intent.IntentDefinition;
import lombok.NonNull;

public class ContextParameterDelegate extends IntentDefinitionProviderImpl implements ContextParameterFragmentStep,
        ContextParameterEntityStep {

    private Context context;

    private ContextParameter parameter;

    public ContextParameterDelegate(@NonNull IntentDefinition intent, @NonNull Context context,
                                    @NonNull ContextParameter parameter) {
        super(intent);
        this.context = context;
        this.parameter = parameter;
    }

    @Override
    public @NonNull ContextParameterStep entity(@NonNull EntityDefinitionReferenceProvider entityReferenceProvider) {
        return this.entity(entityReferenceProvider.getEntityReference());
    }

    @Override
    public @NonNull ContextParameterStep entity(@NonNull EntityDefinitionReference entityReference) {
        this.parameter.setEntity(entityReference);
        return new ContextDelegate(this.intent, this.context);
    }

    @Override
    public @NonNull ContextParameterEntityStep fromFragment(@NonNull String fragment) {
        this.parameter.setTextFragment(fragment);
        return this;
    }
}
