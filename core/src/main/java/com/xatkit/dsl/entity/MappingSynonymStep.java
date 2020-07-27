package com.xatkit.dsl.entity;

import lombok.NonNull;

public interface MappingSynonymStep extends MappingEntryStep, CustomEntityDefinitionProvider,
        EntityDefinitionReferenceProvider {

    @NonNull MappingSynonymStep synonym(@NonNull String synonym);
}
