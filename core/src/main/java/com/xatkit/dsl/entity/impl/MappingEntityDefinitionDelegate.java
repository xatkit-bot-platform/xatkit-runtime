package com.xatkit.dsl.entity.impl;

import com.xatkit.dsl.entity.MappingEntryStep;
import com.xatkit.dsl.entity.MappingReferenceValueStep;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.MappingEntityDefinition;
import com.xatkit.intent.MappingEntityDefinitionEntry;
import lombok.NonNull;

public class MappingEntityDefinitionDelegate extends CustomEntityDefinitionProviderImpl<MappingEntityDefinition> implements
        MappingEntryStep {

    public MappingEntityDefinitionDelegate(MappingEntityDefinition mapping) {
        super(mapping);
    }

    @Override
    public @NonNull MappingReferenceValueStep entry() {
        MappingEntityDefinitionEntry entry = IntentFactory.eINSTANCE.createMappingEntityDefinitionEntry();
        this.entity.getEntries().add(entry);
        return new MappingEntityDefinitionEntryDelegate(this.entity, entry);
    }
}
