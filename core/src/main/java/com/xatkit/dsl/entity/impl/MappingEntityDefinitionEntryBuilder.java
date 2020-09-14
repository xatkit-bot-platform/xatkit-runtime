package com.xatkit.dsl.entity.impl;

import com.xatkit.dsl.entity.CustomEntityDefinitionProvider;
import com.xatkit.dsl.entity.MappingReferenceValueStep;
import com.xatkit.dsl.entity.MappingSynonymStep;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.MappingEntityDefinition;
import com.xatkit.intent.MappingEntityDefinitionEntry;
import lombok.NonNull;

// Wrapper? Delegate?
public class MappingEntityDefinitionEntryBuilder extends MappingEntityDefinitionBuilder implements
        MappingReferenceValueStep,
        MappingSynonymStep,
        CustomEntityDefinitionProvider {

    private MappingEntityDefinitionEntry entry;

    public MappingEntityDefinitionEntryBuilder(MappingEntityDefinition parent) {
        this.entity = parent;
        this.entry = IntentFactory.eINSTANCE.createMappingEntityDefinitionEntry();
        /*
         * Add the entry right now because we don't know what will be the last call to define it (we can have
         * multiple synonyms in an entry).
         */
        this.entity.getEntries().add(this.entry);
    }

    @Override
    public @NonNull MappingSynonymStep value(@NonNull String referenceValue) {
        this.entry.setReferenceValue(referenceValue);
        return this;
    }

    @Override
    public @NonNull MappingSynonymStep synonym(@NonNull String synonym) {
        this.entry.getSynonyms().add(synonym);
        return this;
    }
}
