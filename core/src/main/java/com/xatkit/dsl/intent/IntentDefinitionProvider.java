package com.xatkit.dsl.intent;

import com.xatkit.intent.IntentDefinition;
import lombok.NonNull;

public interface IntentDefinitionProvider extends EventDefinitionProvider {

    @NonNull IntentDefinition getIntentDefinition();
}
