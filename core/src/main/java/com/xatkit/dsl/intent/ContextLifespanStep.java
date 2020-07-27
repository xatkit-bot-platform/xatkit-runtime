package com.xatkit.dsl.intent;

public interface ContextLifespanStep extends ContextParameterStep, IntentDefinitionProvider {

    ContextParameterStep lifespan(int lifespan);
}
