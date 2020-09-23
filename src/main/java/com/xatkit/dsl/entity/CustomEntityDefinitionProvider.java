package com.xatkit.dsl.entity;

import com.xatkit.intent.CustomEntityDefinition;
import lombok.NonNull;

public interface CustomEntityDefinitionProvider {

    @NonNull CustomEntityDefinition getEntity();
}
