package com.xatkit.dsl.intent;

public interface IntentContextParameterStep extends IntentContextStep, IntentDefinitionProvider {

    IntentContextParameterFragmentStep parameter(String parameterName);
}
