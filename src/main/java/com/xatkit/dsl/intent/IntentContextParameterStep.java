package com.xatkit.dsl.intent;

import lombok.NonNull;

public interface IntentContextParameterStep extends IntentDefinitionProvider {

    @NonNull IntentContextParameterFragmentStep parameter(@NonNull String parameterName);
}
