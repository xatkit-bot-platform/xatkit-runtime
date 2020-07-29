package com.xatkit.dsl.intent;

import lombok.NonNull;

public interface IntentContextStep extends IntentDefinitionProvider {

    @NonNull IntentContextLifespanStep context(@NonNull String name);
}
