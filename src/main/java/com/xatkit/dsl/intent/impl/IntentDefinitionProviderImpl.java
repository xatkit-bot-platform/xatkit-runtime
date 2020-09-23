package com.xatkit.dsl.intent.impl;

import com.xatkit.dsl.intent.IntentDefinitionProvider;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.IntentDefinition;
import lombok.NonNull;

public class IntentDefinitionProviderImpl extends EventDefinitionProviderImpl implements IntentDefinitionProvider {

    protected IntentDefinition intent;

    @Override
    public @NonNull IntentDefinition getIntentDefinition() {
        return this.intent;
    }

    @Override
    public @NonNull EventDefinition getEventDefinition() {
        return this.getIntentDefinition();
    }
}
