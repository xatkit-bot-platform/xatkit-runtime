package com.xatkit.dsl.intent;

import lombok.NonNull;

public interface IntentContextParameterFragmentStep extends IntentDefinitionProvider {

    @NonNull IntentContextParameterEntityStep fromFragment(@NonNull String fragment);

    @NonNull IntentContextParameterEntityStep fromFragment(@NonNull String... fragment);
}
