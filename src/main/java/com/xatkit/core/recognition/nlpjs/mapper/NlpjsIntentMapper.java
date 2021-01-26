package com.xatkit.core.recognition.nlpjs.mapper;

import com.xatkit.core.recognition.nlpjs.model.Intent;
import com.xatkit.core.recognition.nlpjs.model.IntentExample;
import com.xatkit.core.recognition.nlpjs.model.IntentParameter;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.util.IntentUtils;
import lombok.NonNull;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * Maps {@link IntentDefinition} instances to NLP.js {@link Intent}s.
 * <p>
 * This class is used to translate generic {@link IntentDefinition}s to platform-specific construct representing NLP
 * .js intents.
 * <p>
 * Calling {@link #mapIntentDefinition(IntentDefinition)} produces a complete NLP.js {@link Intent} containing the
 * training sentences as well as the parameters specified in the abstract {@link IntentDefinition}. Note that this
 * method does not create the {@link com.xatkit.core.recognition.nlpjs.model.Entity} instances used in the intent
 * training sentences. See {@link NlpjsEntityMapper} for Xatkit to NLP.js entity mapping.
 * <p>
 * This class uses the provided {@link NlpjsEntityReferenceMapper} to replace parameter references (defined in
 * {@link ContextParameter#getTextFragments()}) with a string literal that can be properly interpreted by NLP.js.
 */
public class NlpjsIntentMapper {

    /**
     * The {@link NlpjsEntityReferenceMapper} used to map parameter references.
     * <p>
     * Parameter references are specified in {@link ContextParameter#getTextFragments()}. This mapper transforms this
     * abstract representation into a string literal that can be included in NLP.js intent examples and
     * properly interpreted by the NLP.js server.
     */
    private NlpjsEntityReferenceMapper nlpjsEntityReferenceMapper;

    /**
     * Construct an instance of the mapper with the provided {@code nlpjsEntityReferenceMapper}.
     *
     * @param nlpjsEntityReferenceMapper the mapper used to map parameter references
     * @throws NullPointerException if the provided {@code nlpjsEntityReferenceMapper} is {@code null}
     * @see #nlpjsEntityReferenceMapper
     */
    public NlpjsIntentMapper(@NonNull NlpjsEntityReferenceMapper nlpjsEntityReferenceMapper) {
        this.nlpjsEntityReferenceMapper = nlpjsEntityReferenceMapper;
    }

    /**
     * Maps the provided {@link IntentDefinition} to a NLP.js {@link Intent}.
     * <p>
     * This method sets the name of the created intent, its training sentences, and the parameters used to extract
     * information from user input. Note that this method does not check if the entities referred in the
     * intent parameters are registered in the NLP.js agent.
     *
     * @param intentDefinition the {@link IntentDefinition} to map
     * @return the created {@link Intent}
     * @throws NullPointerException if the provided {@code intentDefinition} is {@code null}
     */
    public Intent mapIntentDefinition(@NonNull IntentDefinition intentDefinition) {
        checkNotNull(intentDefinition.getName(), "Cannot map the %s with the provided name %s",
                IntentDefinition.class.getSimpleName(), intentDefinition.getName());
        Intent.IntentBuilder builder = Intent.builder()
                .intentName(intentDefinition.getName());
        List<IntentExample> intentExamples = createIntentExamples(intentDefinition);
        builder.examples(intentExamples);
        builder.parameters(createIntentParameters(intentDefinition));
        return builder.build();
    }

    /**
     * Creates the {@link IntentExample}s representing the training sentences of the provided {@code intentDefinition}.
     * <p>
     * This method replaces the parameter references (specified in {@link ContextParameter#getTextFragments()}) with
     * NLP.js-compatible references retrieved from the {@link NlpjsEntityReferenceMapper}. Note that this method does
     * not register the referred entities, nor check if these entities have been registered.
     *
     * @param intentDefinition the {@link IntentDefinition} to map the training sentences of
     * @return the {@link IntentExample}s representing the training sentences of the provided {@code intentDefinition}
     * @throws NullPointerException if the provided {@code intentDefinition} is {@code null}
     */
    private List<IntentExample> createIntentExamples(@NonNull IntentDefinition intentDefinition) {
        List<IntentExample> result = new ArrayList<>();
        for (String trainingSentence : intentDefinition.getTrainingSentences()) {
            if (IntentUtils.isPureAnyTrainingSentence(intentDefinition, trainingSentence)) {
                /*
                 * Pure any training sentences are not considered when creating entities (NLP.js does not support
                 * them well). To avoid any inconsistency in the matching we also need to ignore them when creating the
                 * examples of the NLP.js intent.
                 */
                continue;
            }
            if (intentDefinition.getParameters().isEmpty()) {
                /*
                 * There is no parameter in the IntentDefinition, we don't need to do any processing and we can
                 * create the IntentExample with the raw training sentence.
                 */
                result.add(new IntentExample(trainingSentence));
            } else {
                /*
                 * First mark all the context parameter literals with #<literal>#. This pre-processing allows to easily
                 * split the training sentence into TrainingPhrase parts, that are bound to their concrete entity when
                 * needed, and sent to the DialogFlow API.
                 * We use this two-step process for simplicity. If the performance of TrainingPhrase creation become an
                 * issue we can reshape this method to avoid this pre-processing phase.
                 * TODO this code is duplicated from DialogFlowIntentMapper
                 */
                String preparedTrainingSentence = trainingSentence;
                for (ContextParameter parameter : intentDefinition.getParameters()) {
                    if (preparedTrainingSentence.contains(parameter.getTextFragments().get(0))) {
                        preparedTrainingSentence =
                                preparedTrainingSentence.replace(parameter.getTextFragments().get(0), "#"
                                        + parameter.getTextFragments().get(0) + "#");
                    }
                }
                String[] splitTrainingSentence = preparedTrainingSentence.split("#");
                StringBuilder intentExampleBuilder = new StringBuilder();
                for (String sentencePart : splitTrainingSentence) {
                    boolean isParameter = false;
                    for (ContextParameter parameter : intentDefinition.getParameters()) {
                        if (sentencePart.equals(parameter.getTextFragments().get(0))) {
                            checkNotNull(parameter.getName(), "Cannot build the training sentence \"%s\", the "
                                            + "parameter for the fragment \"%s\" does not define a name",
                                    trainingSentence, parameter.getTextFragments().get(0));
                            checkNotNull(parameter.getEntity(), "Cannot build the training sentence \"%s\", the "
                                            + "parameter for the fragment \"%s\" does not define an entity",
                                    trainingSentence, parameter.getTextFragments().get(0));
                            isParameter = true;
                            String entityReference = nlpjsEntityReferenceMapper.getMappingFor(parameter).get();
                            /*
                             * NLP.js uses the following format for entity reference in IntentExamples %entity_name%.
                             */
                            intentExampleBuilder.append("%").append(entityReference).append("%");
                        }
                    }
                    if (!isParameter) {
                        /*
                         * The sentence part is not a fragment of a context parameter. There is no processing to do
                         * for this specific part and we can add it as is in the builder.
                         */
                        intentExampleBuilder.append(sentencePart);
                    }
                }
                result.add(new IntentExample(intentExampleBuilder.toString()));
            }
        }
        return result;
    }

    /**
     * Creates the {@link IntentParameter}s representing the parameters of the provided {@code intentDefinition}.
     * <p>
     * This method sets the {@code slot} references of the created {@link IntentParameter}s. Each slot matches an
     * {@link IntentExample} fragment, and is used in the intent recognition process to extract parameter values.
     * <p>
     * Note: NLP.js {@link IntentParameter}s do not define a name.
     *
     * @param intentDefinition the {@link IntentDefinition} to create the parameters from
     * @return the {@link IntentParameter}s representing the parameters of the provided {@code intentDefinition}
     * @throws NullPointerException  if the provided {@code intentDefinition} is {@code null}
     * @throws IllegalStateException if the {@link NlpjsEntityReferenceMapper} cannot find the mapping for a parameter
     */
    private Collection<IntentParameter> createIntentParameters(@NonNull IntentDefinition intentDefinition)
            throws NoSuchElementException {
        Collection<IntentParameter> result = new ArrayList<>();
        for (ContextParameter parameter : intentDefinition.getParameters()) {
            String entityReference =
                    nlpjsEntityReferenceMapper.getMappingFor(parameter).orElseThrow(() -> new IllegalStateException(
                            MessageFormat.format("Cannot find the mapping for parameter {0}", parameter.getName())));
            IntentParameter intentParameter = new IntentParameter();
            intentParameter.setSlot(entityReference);
            result.add(intentParameter);
        }
        return result;
    }
}
