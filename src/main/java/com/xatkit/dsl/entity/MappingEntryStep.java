package com.xatkit.dsl.entity;

import lombok.NonNull;

public interface MappingEntryStep {

    @NonNull MappingReferenceValueStep entry();
}
