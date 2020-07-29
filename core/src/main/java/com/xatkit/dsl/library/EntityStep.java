package com.xatkit.dsl.library;

import com.xatkit.dsl.entity.CustomEntityDefinitionProvider;
import lombok.NonNull;

public interface EntityStep extends IntentStep {

    @NonNull EntityStep entity(@NonNull CustomEntityDefinitionProvider entityProvider);
}
