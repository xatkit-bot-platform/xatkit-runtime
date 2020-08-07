package com.xatkit.dsl.intent;

import lombok.NonNull;

public interface IntentContextParameterStep extends IntentContextStep, IntentDefinitionProvider {

    @NonNull IntentContextParameterFragmentStep parameter(@NonNull String parameterName);
}
