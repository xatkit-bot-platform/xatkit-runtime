package com.xatkit.dsl.intent.impl;

import com.xatkit.dsl.intent.EventDefinitionProvider;
import com.xatkit.intent.EventDefinition;
import lombok.NonNull;

public class EventDefinitionProviderImpl implements EventDefinitionProvider {

    protected EventDefinition event;

    @Override
    public @NonNull EventDefinition getEventDefinition() {
        return this.event;
    }
}
