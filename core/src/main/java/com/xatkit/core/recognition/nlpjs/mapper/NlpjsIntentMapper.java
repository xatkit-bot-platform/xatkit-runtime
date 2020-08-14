package com.xatkit.core.recognition.nlpjs.mapper;

import com.xatkit.core.recognition.nlpjs.NlpjsConfiguration;
import com.xatkit.core.recognition.nlpjs.model.Intent;
import com.xatkit.core.recognition.nlpjs.model.IntentExample;
import com.xatkit.intent.Context;
import com.xatkit.intent.IntentDefinition;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;
import java.util.List;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

public class NlpjsIntentMapper {

    private NlpjsConfiguration configuration;

    public NlpjsIntentMapper(@NonNull NlpjsConfiguration configuration){
        this.configuration = configuration;
    }

    public Intent mapIntentDefinition(@NonNull IntentDefinition intentDefinition) {
        checkNotNull(intentDefinition.getName(), "Cannot map the %s with the provided name %s",
                IntentDefinition.class.getSimpleName(), intentDefinition.getName());
        Intent.Builder builder = Intent.newBuilder()
                .intentName(intentDefinition.getName());
        for(String trainingSentence:  intentDefinition.getTrainingSentences()){
            builder.addExample(createTrainingExample(trainingSentence,intentDefinition.getOutContexts()));
        }
        return builder.build();
    }

    private IntentExample createTrainingExample(@NonNull String trainingSentence, @NonNull List<Context> outContexts){
        if(!outContexts.isEmpty()) {
            Log.warn("outContext are not supported in NLP.js engine");
        }

            return new IntentExample(trainingSentence);
        }
}
