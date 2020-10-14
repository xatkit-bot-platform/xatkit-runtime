package com.xatkit.dsl.intent;

import lombok.NonNull;

/**
 * Commodity interface (= IntentMandatoryTrainingSentence) to have clear usage of the DSL:
 * IntentVar myIntent = intent("MyState")
 */
public interface IntentVar extends IntentDefinitionProvider {

    @NonNull IntentOptionalTrainingSentenceStep trainingSentence(@NonNull String trainingSentence);

    @NonNull IntentOptionalTrainingSentenceStep trainingSentences(@NonNull Iterable<String> trainingSentences);
}
