package com.xatkit.dsl.entity;

import lombok.NonNull;

public interface MappingReferenceValueStep extends MappingEntryStep {

    @NonNull MappingSynonymStep value(@NonNull String referenceValue);
}
