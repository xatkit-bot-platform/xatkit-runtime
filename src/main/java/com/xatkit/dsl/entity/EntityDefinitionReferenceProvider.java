package com.xatkit.dsl.entity;

import com.xatkit.intent.EntityDefinitionReference;
import lombok.NonNull;

public interface EntityDefinitionReferenceProvider {

    @NonNull EntityDefinitionReference getEntityReference();
}
