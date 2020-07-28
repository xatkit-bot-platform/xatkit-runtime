package com.xatkit.dsl.intent;

import com.xatkit.intent.IntentDefinition;

public interface IntentDefinitionProvider extends EventDefinitionProvider {

    IntentDefinition getIntentDefinition();
}
