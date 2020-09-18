package com.xatkit.core.recognition.nlpjs.mapper;

import com.xatkit.core.recognition.nlpjs.NlpjsConfiguration;
import com.xatkit.core.recognition.nlpjs.NlpjsHelper;
import com.xatkit.core.recognition.nlpjs.model.*;
import com.xatkit.intent.*;
import com.xatkit.intent.EntityType;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;

import static java.util.Objects.nonNull;

import java.util.*;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

public class NlpjsIntentMapper {

    private NlpjsConfiguration configuration;

    private NlpjsEntityReferenceMapper nlpjsEntityReferenceMapper;

    public NlpjsIntentMapper(@NonNull NlpjsConfiguration configuration,
                             @NonNull NlpjsEntityReferenceMapper nlpjsEntityReferenceMapper) {
        this.configuration = configuration;
        this.nlpjsEntityReferenceMapper = nlpjsEntityReferenceMapper;

    }

    public Intent mapIntentDefinition(@NonNull IntentDefinition intentDefinition, List<Entity> anyEntitiesCollector) {
        checkNotNull(intentDefinition.getName(), "Cannot map the %s with the provided name %s",
                IntentDefinition.class.getSimpleName(), intentDefinition.getName());
        Intent.Builder builder = Intent.newBuilder()
                .intentName(intentDefinition.getName());
        Map<String, IntentParameter> intentParametersMap = new HashMap<>();
        Map<String, Entity> anyEntitiesMap = new HashMap<>();
        List<IntentExample> intentExamples = createIntentExamples(intentDefinition, intentParametersMap, anyEntitiesMap);
        anyEntitiesCollector.addAll(anyEntitiesMap.values());
        builder.examples(intentExamples);
        builder.parameters(new ArrayList<>(intentParametersMap.values()));
        return builder.build();
    }

    private List<IntentExample> createIntentExamples(@NonNull IntentDefinition intentDefinition, @NonNull Map<String, IntentParameter> intentParametersMap, Map<String, Entity> anyEntitiesMap) {
        List<IntentExample> intentExamples = new ArrayList<>();
        for (String trainingSentence : intentDefinition.getTrainingSentences()) {
            intentExamples.add(createIntentExample(intentDefinition, trainingSentence, intentDefinition.getOutContexts(), intentParametersMap, anyEntitiesMap));
        }
        return intentExamples;
    }

    private IntentExample createIntentExample(@NonNull IntentDefinition intentDefinition, @NonNull String trainingSentence, @NonNull List<Context> outContexts, @NonNull Map<String, IntentParameter> intentParametersMap, Map<String, Entity> anyEntitiesMap) {
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
                            String nlpEntity = null;
                            boolean isAny = false;
                            if (parameter.getEntity().getReferredEntity() instanceof BaseEntityDefinition &&
                                    ((BaseEntityDefinition) parameter.getEntity().getReferredEntity()).getEntityType().equals(EntityType.ANY)) {
                                int textFragmentIndexStart = trainingSentence.indexOf(parameter.getTextFragment());
                                int textFragmentIndexEnd = textFragmentIndexStart + parameter.getTextFragment().length();
                                String[] preParameterArray = null;
                                String[] postParameterArray = null;
                                if (textFragmentIndexStart != 0) {
                                    preParameterArray  = trainingSentence.substring(0, textFragmentIndexStart).split(" ");
                                }
                                if (textFragmentIndexEnd != trainingSentence.length()) {
                                    postParameterArray = trainingSentence.substring(textFragmentIndexEnd).split(" ");
                                }

                                String beforeLast = null;
                                String afterLast = null;

                                if (nonNull(preParameterArray) && preParameterArray.length > 0) {
                                    afterLast = preParameterArray[preParameterArray.length - 1];
                                }
                                if (nonNull(postParameterArray) && postParameterArray.length > 0) {
                                    beforeLast = postParameterArray[0];
                                }

                                if (nonNull(beforeLast) || nonNull(afterLast)) {
                                    nlpEntity = intentDefinition.getName() + parameter.getName() + "Any";
                                    isAny = true;
                                    if (anyEntitiesMap.containsKey(nlpEntity)) {
                                        if (nonNull(beforeLast) && nonNull(afterLast)) {
                                            Entity entity = anyEntitiesMap.get(beforeLast);
                                            entity.getBetween().getLeft().add(afterLast);
                                            entity.getBetween().getRight().add(beforeLast);
                                        } else if (nonNull(beforeLast)) {
                                            anyEntitiesMap.get(nlpEntity).getBeforeLast().add(beforeLast);
                                        } else {
                                            anyEntitiesMap.get(nlpEntity).getAfterLast().add(afterLast);
                                        }
                                    } else {
                                        Entity.Builder builder = Entity.newBuilder();
                                        builder.entityName(nlpEntity);
                                        builder.type(com.xatkit.core.recognition.nlpjs.model.EntityType.TRIM);
                                        if (nonNull(beforeLast) && nonNull(afterLast)) {
                                            BetweenCondition betweenCondition = new BetweenCondition();
                                            betweenCondition.getRight().add(beforeLast);
                                            betweenCondition.getLeft().add(afterLast);
                                            builder.between(betweenCondition);
                                        } else if (nonNull(beforeLast)) {
                                            builder.addBeforeLast(beforeLast);
                                        } else {
                                            builder.addAfterLast(afterLast);
                                        }
                                        anyEntitiesMap.put(nlpEntity,builder.build());
                                    }
                                } else {
                                    //TODO input Text
                                }
                            } else {
                                nlpEntity = nlpjsEntityReferenceMapper.getMappingFor(parameter.getEntity()
                                        .getReferredEntity());

                            }
                            StringBuilder nlpjsIntentParameterBuilder = new StringBuilder().append(nlpEntity);
                            if (!isAny && NlpjsHelper.getEntityCount(parameter.getEntity().getReferredEntity(), outContexts) > 1) {
                                nlpjsIntentParameterBuilder.append("_").append(NlpjsHelper.getEntityTypeIndex(parameter.getTextFragment(),
                                        parameter.getEntity().getReferredEntity(), outContexts));
                            }
                            String nlpjsIntentParameter = nlpjsIntentParameterBuilder.toString();
                            intentExampleBuilder.append("%").append(nlpjsIntentParameter).append("%");
                            if (!intentParametersMap.containsKey(parameter.getName())) {
                                IntentParameter intentParameter = new IntentParameter();
                                intentParameter.setSlot(nlpjsIntentParameter);
                                intentParametersMap.put(parameter.getName(), intentParameter);
                            }
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
