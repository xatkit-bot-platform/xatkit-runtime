package com.xatkit.dsl.entity.impl;

import com.xatkit.dsl.entity.MappingEntryStep;
import com.xatkit.dsl.entity.MappingReferenceValueStep;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.MappingEntityDefinition;
import lombok.NonNull;

public class MappingEntityDefinitionBuilder extends CustomEntityDefinitionProviderImpl<MappingEntityDefinition> implements
        MappingEntryStep {

    public MappingEntityDefinitionBuilder() {
        this.entity = IntentFactory.eINSTANCE.createMappingEntityDefinition();
    }

    @Override
    public @NonNull MappingReferenceValueStep entry() {
        return new MappingEntityDefinitionEntryBuilder(this.entity);
    }
}
