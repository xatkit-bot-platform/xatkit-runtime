package com.xatkit.core.recognition.nlpjs.mapper;

import com.xatkit.core.recognition.nlpjs.NlpjsConfiguration;
import com.xatkit.core.recognition.nlpjs.model.Intent;
import com.xatkit.core.recognition.nlpjs.model.IntentExample;
import com.xatkit.intent.Context;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.IntentDefinition;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

public class NlpjsIntentMapper {

    private NlpjsConfiguration configuration;

    private NlpjsSlotMapper nlpjsSlotMapper;

    public NlpjsIntentMapper(@NonNull NlpjsConfiguration configuration,
                             @NonNull NlpjsSlotMapper nlpjsSlotMapper) {
        this.configuration = configuration;
        this.nlpjsSlotMapper = nlpjsSlotMapper;

    }

    public Intent mapIntentDefinition(@NonNull IntentDefinition intentDefinition) {
        checkNotNull(intentDefinition.getName(), "Cannot map the %s with the provided name %s",
                IntentDefinition.class.getSimpleName(), intentDefinition.getName());
        Intent.Builder builder = Intent.newBuilder()
                .intentName(intentDefinition.getName());
        List<IntentExample> intentExamples = createIntentExamples(intentDefinition);
        builder.examples(intentExamples);
        return builder.build();
    }

    private List<IntentExample> createIntentExamples(@NonNull IntentDefinition intentDefinition) {
        List<IntentExample> intentExamples = new ArrayList<>();
        for(String trainingSentence:  intentDefinition.getTrainingSentences()){
            intentExamples.add(createIntentExample(trainingSentence,intentDefinition.getOutContexts()));
        }
        return intentExamples;
    }

    private IntentExample createIntentExample(@NonNull String trainingSentence, @NonNull List<Context> outContexts) {
        if (outContexts.isEmpty()) {
            return new IntentExample(trainingSentence);
        } else {
            String preparedTrainingSentence = trainingSentence;
            for (com.xatkit.intent.Context context : outContexts) {
                for (ContextParameter parameter : context.getParameters()) {
                    if (preparedTrainingSentence.contains(parameter.getTextFragment())) {
                        preparedTrainingSentence = preparedTrainingSentence.replace(parameter.getTextFragment(), "#"
                                + parameter.getTextFragment() + "#");
                    }
                }
            }
            String[] splitTrainingSentence = preparedTrainingSentence.split("#");
            StringBuilder intentExampleBuilder = new StringBuilder();
            for (int i = 0; i < splitTrainingSentence.length; i++) {
                String sentencePart = splitTrainingSentence[i];
                boolean isParameter = false;
                for (com.xatkit.intent.Context context : outContexts) {
                    for (ContextParameter parameter : context.getParameters()) {
                        if (sentencePart.equals(parameter.getTextFragment())) {
                            checkNotNull(parameter.getName(), "Cannot build the training sentence \"%s\", the " +
                                            "parameter for the fragment \"%s\" does not define a name",
                                    trainingSentence, parameter.getTextFragment());
                            checkNotNull(parameter.getEntity(), "Cannot build the training sentence \"%s\", the " +
                                            "parameter for the fragment \"%s\" does not define an entity",
                                    trainingSentence, parameter.getTextFragment());
                            isParameter = true;
                            String nlpEntity =
                                    nlpjsSlotMapper.getMappingFor(parameter.getEntity()
                                            .getReferredEntity());
                            intentExampleBuilder.append("%").append(nlpEntity).append("%");
                        }
                    }
                }
                if (!isParameter) {
                    intentExampleBuilder.append(sentencePart);
                }

            }
            return new IntentExample(intentExampleBuilder.toString());

        }
    }
}
