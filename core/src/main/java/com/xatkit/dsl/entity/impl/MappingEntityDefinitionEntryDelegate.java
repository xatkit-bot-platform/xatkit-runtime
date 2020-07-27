package com.xatkit.dsl.entity.impl;

import com.xatkit.dsl.entity.CustomEntityDefinitionProvider;
import com.xatkit.dsl.entity.MappingReferenceValueStep;
import com.xatkit.dsl.entity.MappingSynonymStep;
import com.xatkit.intent.MappingEntityDefinition;
import com.xatkit.intent.MappingEntityDefinitionEntry;

// Wrapper? Delegate?
public class MappingEntityDefinitionEntryDelegate extends MappingEntityDefinitionDelegate implements
        MappingReferenceValueStep,
        MappingSynonymStep,
        CustomEntityDefinitionProvider {

    private MappingEntityDefinitionEntry entry;

    public MappingEntityDefinitionEntryDelegate(MappingEntityDefinition entity, MappingEntityDefinitionEntry entry) {
        super(entity);
        this.entry = entry;
    }

    @Override
    public MappingSynonymStep value(String referenceValue) {
        this.entry.setReferenceValue(referenceValue);
        return this;
    }

    @Override
    public MappingSynonymStep synonym(String synonym) {
        this.entry.getSynonyms().add(synonym);
        return this;
    }
}
