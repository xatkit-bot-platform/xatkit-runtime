package com.xatkit.dsl.intent.impl;

import com.xatkit.dsl.intent.IntentContextParameterFragmentStep;
import com.xatkit.dsl.intent.IntentMandatoryTrainingSentenceStep;
import com.xatkit.dsl.intent.IntentOptionalTrainingSentenceStep;
import com.xatkit.intent.IntentFactory;
import lombok.NonNull;

import java.util.Arrays;

public class IntentDefinitionBuilder extends IntentDefinitionProviderImpl implements IntentMandatoryTrainingSentenceStep,
        IntentOptionalTrainingSentenceStep {

    public IntentDefinitionBuilder() {
        this.intent = IntentFactory.eINSTANCE.createIntentDefinition();
    }

    public @NonNull IntentDefinitionBuilder name(@NonNull String name) {
        this.intent.setName(name);
        return this;
    }

    @Override
    public @NonNull IntentContextParameterFragmentStep parameter(@NonNull String parameterName) {
        IntentContextParameterBuilder intentContextParameterBuilder = new IntentContextParameterBuilder(this.intent);
        intentContextParameterBuilder.name(parameterName);
        return intentContextParameterBuilder;
    }

    @Override
    public @NonNull IntentOptionalTrainingSentenceStep trainingSentence(@NonNull String trainingSentence) {
        this.intent.getTrainingSentences().add(trainingSentence);
        return this;
    }

    @Override
    public @NonNull IntentOptionalTrainingSentenceStep trainingSentences(@NonNull Iterable<String> trainingSentences) {
        trainingSentences.forEach(this::trainingSentence);
        return this;
    }

    @Override
    public @NonNull IntentOptionalTrainingSentenceStep trainingSentences(@NonNull String[] trainingSentences) {
        return this.trainingSentences(Arrays.asList(trainingSentences));
    }
}
