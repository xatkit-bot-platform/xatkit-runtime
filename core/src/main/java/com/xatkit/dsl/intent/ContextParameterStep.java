package com.xatkit.dsl.intent;

public interface ContextParameterStep extends ContextStep, IntentDefinitionProvider {

    ContextParameterFragmentStep parameter(String parameterName);
}
