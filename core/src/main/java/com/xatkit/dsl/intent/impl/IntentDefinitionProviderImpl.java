package com.xatkit.dsl.intent.impl;

import com.xatkit.dsl.intent.IntentDefinitionProvider;
import com.xatkit.intent.IntentDefinition;
import lombok.NonNull;

public class IntentDefinitionProviderImpl extends EventDefinitionProviderImpl implements IntentDefinitionProvider {

    protected IntentDefinition intent;

    public IntentDefinitionProviderImpl(@NonNull IntentDefinition intent) {
        super(intent);
        this.intent = intent;
    }

    @Override
    public @NonNull IntentDefinition getIntentDefinition() {
        return this.intent;
    }
}
