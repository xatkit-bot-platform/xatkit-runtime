package com.xatkit.dsl.intent.impl;

import com.xatkit.dsl.intent.ContextLifespanStep;
import com.xatkit.dsl.intent.IntentMandatoryTrainingSentenceStep;
import com.xatkit.dsl.intent.IntentOptionalTrainingSentenceStep;
import com.xatkit.intent.Context;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.IntentFactory;
import lombok.NonNull;

public class IntentDefinitionDelegate extends IntentDefinitionProviderImpl implements IntentMandatoryTrainingSentenceStep,
        IntentOptionalTrainingSentenceStep {

    public IntentDefinitionDelegate(@NonNull IntentDefinition intent) {
        super(intent);
    }

    @Override
    public @NonNull ContextLifespanStep context(@NonNull String name) {
        Context context = IntentFactory.eINSTANCE.createContext();
        context.setName(name);
        this.intent.getOutContexts().add(context);
        return new ContextDelegate(this.intent, context);
    }

    @Override
    public @NonNull IntentOptionalTrainingSentenceStep trainingSentence(@NonNull String trainingSentence) {
        this.intent.getTrainingSentences().add(trainingSentence);
        return this;
    }
}
