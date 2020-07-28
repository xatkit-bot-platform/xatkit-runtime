package com.xatkit.dsl.intent.impl;

import com.xatkit.dsl.intent.EventDefinitionProvider;
import com.xatkit.intent.EventDefinition;

public class EventDefinitionProviderImpl implements EventDefinitionProvider {

    protected EventDefinition event;

    public EventDefinitionProviderImpl(EventDefinition event) {
        this.event = event;
    }

    @Override
    public EventDefinition getEventDefinition() {
        return this.event;
    }
}
