package com.xatkit.dsl.intent;

public interface IntentContextStep extends IntentDefinitionProvider {

    IntentContextLifespanStep context(String name);
}
