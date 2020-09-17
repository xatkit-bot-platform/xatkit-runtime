package com.xatkit.core.recognition.nlpjs.mapper;

import com.xatkit.core.recognition.nlpjs.NlpjsConfiguration;
import com.xatkit.core.recognition.nlpjs.NlpjsHelper;
import com.xatkit.core.recognition.nlpjs.model.Intent;
import com.xatkit.core.recognition.nlpjs.model.IntentExample;
import com.xatkit.core.recognition.nlpjs.model.IntentParameter;
import com.xatkit.intent.Context;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.IntentDefinition;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

public class NlpjsIntentMapper {

    private NlpjsConfiguration configuration;

    private NlpjsEntityReferenceMapper nlpjsEntityReferenceMapper;

    public NlpjsIntentMapper(@NonNull NlpjsConfiguration configuration,
                             @NonNull NlpjsEntityReferenceMapper nlpjsEntityReferenceMapper) {
        this.configuration = configuration;
        this.nlpjsEntityReferenceMapper = nlpjsEntityReferenceMapper;

    }

    public Intent mapIntentDefinition(@NonNull IntentDefinition intentDefinition) {
        checkNotNull(intentDefinition.getName(), "Cannot map the %s with the provided name %s",
                IntentDefinition.class.getSimpleName(), intentDefinition.getName());
        Intent.Builder builder = Intent.newBuilder()
                .intentName(intentDefinition.getName());
        Map<String, IntentParameter> intentParametersMap = new HashMap<>();
        List<IntentExample> intentExamples = createIntentExamples(intentDefinition, intentParametersMap);
        builder.examples(intentExamples);
        builder.parameters(new ArrayList<>(intentParametersMap.values()));
        return builder.build();
    }

    private List<IntentExample> createIntentExamples(@NonNull IntentDefinition intentDefinition, @NonNull Map<String, IntentParameter> intentParametersMap) {
        List<IntentExample> intentExamples = new ArrayList<>();
        for (String trainingSentence : intentDefinition.getTrainingSentences()) {
            intentExamples.add(createIntentExample(trainingSentence, intentDefinition.getOutContexts(),intentParametersMap));
        }
        return intentExamples;
    }

    private IntentExample createIntentExample(@NonNull String trainingSentence, @NonNull List<Context> outContexts, @NonNull Map<String, IntentParameter> intentParametersMap) {
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
            for (String sentencePart : splitTrainingSentence) {
                boolean isParameter = false;
                for (Context context : outContexts) {
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
                                    nlpjsEntityReferenceMapper.getMappingFor(parameter.getEntity()
                                            .getReferredEntity());
                            StringBuilder nlpjsIntentParameterBuilder = new StringBuilder().append(nlpEntity);
                            if (NlpjsHelper.getEntityCount(parameter.getEntity().getReferredEntity(), outContexts) > 1) {
                                nlpjsIntentParameterBuilder.append("_").append(NlpjsHelper.getEntityTypeIndex(parameter.getTextFragment(),
                                        parameter.getEntity().getReferredEntity(), outContexts));
                            }
                            String nlpjsIntentParameter = nlpjsIntentParameterBuilder.toString();
                            intentExampleBuilder.append("%").append(nlpjsIntentParameter).append("%");
                            if (!intentParametersMap.containsKey(parameter.getName())) {
                                IntentParameter intentParameter = new IntentParameter();
                                intentParameter.setSlot(nlpjsIntentParameter);
                                intentParametersMap.put(parameter.getName(),intentParameter);
                            }
                        }
                    }
                }
                if (!isParameter) {
                    System.out.println(sentencePart);
                    intentExampleBuilder.append(sentencePart);
                }
            }
            return new IntentExample(intentExampleBuilder.toString());

        }
    }


}
