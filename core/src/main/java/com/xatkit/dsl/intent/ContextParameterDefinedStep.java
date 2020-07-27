package com.xatkit.dsl.intent;

// "Fake" state to make sure it's possible to define a new parameter/context after a parameter entity
public interface ContextParameterDefinedStep extends ContextStep, ContextParameterStep, IntentDefinitionProvider,
        ContextProvider {
}
