package com.xatkit.dsl.intent;

public interface IntentContextLifespanStep extends IntentContextParameterStep, EventDefinitionProvider {

    IntentContextParameterStep lifespan(int lifespan);
}
