package com.xatkit.dsl.intent;

/**
 * Commodity interface (= IntentMandatoryTrainingSentence) to have clear usage of the DSL:
 * IntentVar myIntent = intent("MyState")
 */
public interface IntentVar extends IntentDefinitionProvider {

    IntentOptionalTrainingSentenceStep trainingSentence(String trainingSentence);
}
