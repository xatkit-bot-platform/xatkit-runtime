package com.xatkit.dsl.intent;

import lombok.NonNull;

public interface EventContextStep extends EventDefinitionProvider {

    @NonNull EventContextLifespanStep context(@NonNull String name);
}
