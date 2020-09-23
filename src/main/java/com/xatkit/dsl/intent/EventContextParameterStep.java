package com.xatkit.dsl.intent;

import lombok.NonNull;

public interface EventContextParameterStep extends EventDefinitionProvider {

    @NonNull EventContextParameterStep parameter(@NonNull String name);
}
