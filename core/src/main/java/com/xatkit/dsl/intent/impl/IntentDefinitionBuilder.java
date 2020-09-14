package com.xatkit.dsl.intent.impl;

import com.xatkit.dsl.intent.IntentContextLifespanStep;
import com.xatkit.dsl.intent.IntentMandatoryTrainingSentenceStep;
import com.xatkit.dsl.intent.IntentOptionalTrainingSentenceStep;
import com.xatkit.intent.Context;
import com.xatkit.intent.IntentFactory;
import lombok.NonNull;

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
    public @NonNull IntentContextLifespanStep context(@NonNull String name) {
        Context context = IntentFactory.eINSTANCE.createContext();
        context.setName(name);
        this.intent.getOutContexts().add(context);
        return new IntentContextBuilder(this.intent, context);
    }

    @Override
    public @NonNull IntentOptionalTrainingSentenceStep trainingSentence(@NonNull String trainingSentence) {
        this.intent.getTrainingSentences().add(trainingSentence);
        return this;
    }
}
