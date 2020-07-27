package com.xatkit.dsl.library;

import com.xatkit.dsl.entity.CustomEntityDefinitionProvider;

public interface EntityStep extends IntentStep {

    EntityStep entity(CustomEntityDefinitionProvider entityProvider);
}
