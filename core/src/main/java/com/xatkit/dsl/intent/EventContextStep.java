package com.xatkit.dsl.intent;

public interface EventContextStep extends EventDefinitionProvider {

    EventContextLifespanStep context(String name);
}
