package com.xatkit.core.recognition.nlpjs.mapper;


import com.xatkit.core.EventDefinitionRegistry;
import com.xatkit.core.recognition.nlpjs.model.Classification;
import com.xatkit.core.recognition.nlpjs.model.ExtractedEntity;
import com.xatkit.core.recognition.nlpjs.model.RecognitionResult;
import com.xatkit.intent.BaseEntityDefinition;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.ContextParameterValue;
import com.xatkit.intent.CustomEntityDefinition;
import com.xatkit.intent.EntityType;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.RecognizedIntent;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.xatkit.core.recognition.IntentRecognitionProvider.DEFAULT_FALLBACK_INTENT;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Maps NLP.js {@link RecognitionResult} to {@link RecognizedIntent}s.
 * <p>
 * This class provides two methods:
 * <ul>
 *     <li>{@link #mapRecognitionResult(RecognitionResult)}: creates a list of {@link RecognizedIntent} from a
 *     given {@link RecognitionResult}. This method does not set the {@link RecognizedIntent}'s parameter values.
 *     This is an expensive operation that should be performed on a per-intent basis (typically the one selected by
 *     the recognition engine).
 *     </li>
 *     <li>{@link #mapParameterValues(IntentDefinition, List)}: creates the {@link ContextParameterValue} mapping the
 *     provided {@link EntityType}s for a given {@link IntentDefinition}. This method is typically used to set the
 *     parameters of the {@link RecognizedIntent} that has been selected by the recognition engine.
 *     </li>
 * </ul>
 *
 * @see #mapRecognitionResult(RecognitionResult)
 * @see #mapParameterValues(IntentDefinition, List)
 */
public class NlpjsRecognitionResultMapper {

    /**
     * The minimal matching confidence score accepted by this mapper.
     * <p>
     * {@link RecognitionResult}'s {@link Classification} instances with a confidence lower than this value are
     * discarded during the mapping. They correspond to highly improbable matches and shouldn't be evaluated by the
     * recognition engine when selecting the best candidate from a given {@link RecognitionResult}.
     */
    private static final double MIN_CONFIDENCE_SCORE = 0.1;

    /**
     * The {@link EventDefinitionRegistry} used to retrieve the bot's {@link IntentDefinition}s.
     */
    private EventDefinitionRegistry eventRegistry;

    /**
     * The {@link NlpjsEntityReferenceMapper} used to retrieve the Xatkit entities form the NLP.js ones.
     * <p>
     * This class uses the {@link NlpjsEntityReferenceMapper#getReversedEntity(String)} method to perform a reverse
     * lookup in the registered entities.
     */
    private NlpjsEntityReferenceMapper nlpjsEntityReferenceMapper;

    /**
     * Constructs a {@link NlpjsRecognitionResultMapper} from the provided {@code eventRegistry} and {@code
     * nlpjsEntityReferenceMapper}.
     *
     * @param eventRegistry              the {@link EventDefinitionRegistry} used to retrieve the bot's
     *                                   {@link IntentDefinition}s
     * @param nlpjsEntityReferenceMapper the mapper used to retrieve Xatkit entities from NLP.js ones
     * @throws NullPointerException if the provided {@code eventRegistry} or {@code nlpjsEntityReferenceMapper} is
     *                              {@code null}
     */
    public NlpjsRecognitionResultMapper(@NonNull EventDefinitionRegistry eventRegistry,
                                        @NonNull NlpjsEntityReferenceMapper nlpjsEntityReferenceMapper) {
        this.eventRegistry = eventRegistry;
        this.nlpjsEntityReferenceMapper = nlpjsEntityReferenceMapper;
    }

    /**
     * Transforms the NLP.js {@code recognitionResult} to {@link RecognizedIntent}s.
     * <p>
     * NLP.js returns a classification of the agent's intents for a given input. This method transform each
     * classification entry to a {@link RecognizedIntent}, but does not select which one to keep and use in the bot's
     * state machine.
     * <p>
     * This method does not set the {@link ContextParameterValue}s of the returned {@link RecognizedIntent}s. Setting
     * parameters is an expensive operation that should be done on an instance-basis (typically the
     * {@link RecognizedIntent} that has been selected as the best option). See
     * {@link #mapParameterValues(IntentDefinition, List)} to get the list of parameters for a given
     * {@link IntentDefinition}.
     * <p>
     * Classification entries with a confidence below {@code 0.1} are discarded.
     * <p>
     * NLP.js default intent "None" is mapped to
     * {@link com.xatkit.core.recognition.IntentRecognitionProvider#DEFAULT_FALLBACK_INTENT}.
     *
     * @param recognitionResult the NLP.js recognition result
     * @return the {@link RecognizedIntent}s corresponding to the provided {@code recognitionResult}
     * @throws NullPointerException if the provided {@code recognitionResult} is {@code null}
     */
    public List<RecognizedIntent> mapRecognitionResult(@NonNull RecognitionResult recognitionResult) {
        List<Classification> classifications = recognitionResult.getClassifications();
        classifications =
                classifications.stream().filter(c -> c.getScore() > MIN_CONFIDENCE_SCORE).collect(Collectors.toList());
        List<RecognizedIntent> recognizedIntents = new ArrayList<>();
        for (Classification classification : classifications) {
            RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
            IntentDefinition intentDefinition = convertNlpjsIntentNameToIntentDefinition(classification.getIntent());
            recognizedIntent.setDefinition(intentDefinition);
            recognizedIntent.setRecognitionConfidence(classification.getScore());
            recognizedIntent.setMatchedInput(recognitionResult.getUtterance());
            recognizedIntents.add(recognizedIntent);
        }
        return recognizedIntents;
    }

    /**
     * Converts the provided {@code extractedEntities} to {@link ContextParameterValue}.
     * <p>
     * NLP.js integrates the {@link ExtractedEntity} instances of all the intents in its classification. This
     * method discards the ones that do not correspond to a {@link ContextParameter} of the provided {@code
     * intentDefinition}.
     * <p>
     * This method is not invoked when calling {@link #mapRecognitionResult(RecognitionResult)} because it is not
     * necessary to map the {@link ContextParameterValue}s of all the {@link RecognizedIntent}s extracted from NLP.js.
     *
     * @param intentDefinition  the {@link IntentDefinition} containing the {@link ContextParameter} to instantiate
     * @param extractedEntities the {@link ExtractedEntity} instances returned by NLP.js
     * @return the created {@link ContextParameter}s
     * @throws NullPointerException if the provided {@code intentDefinition} or {@code extractedEntities} is {@code
     *                              null}
     */
    public List<ContextParameterValue> mapParameterValues(@NonNull IntentDefinition intentDefinition,
                                                          @NonNull List<ExtractedEntity> extractedEntities) {
        List<ContextParameterValue> contextParameterValues = new ArrayList<>();
        for (ExtractedEntity extractedEntity : extractedEntities) {
            String entityType = extractedEntity.getEntity();
            Optional<ContextParameter> contextParameter = this.getContextParameterFromNlpEntity(entityType,
                    intentDefinition);
            if (contextParameter.isPresent()) {
                ContextParameterValue contextParameterValue =
                        IntentFactory.eINSTANCE.createContextParameterValue();
                if (nonNull(extractedEntity.getValue())) {
                    contextParameterValue.setValue(convertParameterValueToString(extractedEntity.getValue()));
                } else if (extractedEntity.getType().equals("regex")) {
                    /*
                     * Regex in NLP.js are applied to the entire source text. This means that when a regex entity
                     * is matched its entire source text is the value of the regex.
                     * Since NLP.js doesn't set the value field of regex entities we use the utterance text to
                     * populate the parameter value.
                     */
                    contextParameterValue.setValue(convertParameterValueToString(extractedEntity.getUtteranceText()));
                } else {
                    Log.warn("Cannot retrieve the value for the context parameter {0}",
                            contextParameter.get().getName());
                }
                contextParameterValue.setContextParameter(contextParameter.get());
                contextParameterValues.add(contextParameterValue);
            }
        }
        return contextParameterValues;
    }

    /**
     * Returns the {@link ContextParameter} from {@code intentDefinition} matching the provided {@code nlpjsEntityType}.
     * <p>
     * This method handles suffixed NLP.js entity types, and match them to the corresponding parameter in the {@code
     * intentDefinition}. For example {@code getContextParameterFromNlpEntity("type_2", i)} returns the second
     * {@link ContextParameter} typed {@code type} of {@code i}.
     *
     * @param nlpjsEntityType  the NLP.js entity type to match
     * @param intentDefinition the {@link IntentDefinition} containing the {@link ContextParameter} tp retrieve
     * @return the {@link ContextParameter} matching the NLP.js entity type
     * @throws NullPointerException if {@code nlpjsEntityType} or {@code intentDefinition} is {@code null}
     */
    private Optional<ContextParameter> getContextParameterFromNlpEntity(@NonNull String nlpjsEntityType,
                                                                        @NonNull IntentDefinition intentDefinition) {
        /*
         * Suffixed types do not work with v4 of NLP.js. We have asked the NLP.js developers about it.
         */
        String[] splitEntity = nlpjsEntityType.split("_");
        String baseNlpjsEntityType = nlpjsEntityType;
        int index = -1;
        /*
         * If the provided nlpjsEntityType contains a "_" this means that we are dealing with multiple entities of
         * the same type in the corresponding IntentDefinition. We compute the index of the parameter we are looking
         * for, as well as the base name of the entity to compare it with the list of parameters in the
         * IntentDefinition.
         * If the provided nlpjsEntityType does not contain "_" we set the index to -1, effectively ignoring index
         * comparison (index == -1 || index == i).
         */
        if (splitEntity.length > 1) {
            try {
                index = Integer.parseInt(splitEntity[splitEntity.length - 1]);
            } catch (NumberFormatException e) {
                /*
                 * Ignore the exception, this means that we aren't dealing with a suffixed entity.
                 */
            }
        }
        if (index != -1) {
            /*
             * We extracted an index, this means that we need to find the substring of nlpjsEntityType that
             * corresponds to the entity name.
             */
            baseNlpjsEntityType = nlpjsEntityType.substring(0, nlpjsEntityType.lastIndexOf("_"));
        }
        /*
         * Counts the number of times we have found a parameter matching the base entity name. This value is used to
         * retrieve the nth occurrence of a parameter, for example when matching parameter_2.
         */
        int i = 0;
        for (ContextParameter parameter : intentDefinition.getParameters()) {
            if (parameter.getEntity().getReferredEntity() instanceof BaseEntityDefinition) {
                /*
                 * The parameter is a BaseEntityDefinition. We differentiate any entities from other base
                 * entities, because they do not support index suffixes, and their mapping is not recorded in
                 * NlpjsEntityReferenceMapper.getReversedEntity().
                 */
                BaseEntityDefinition baseEntityDefinition =
                        (BaseEntityDefinition) parameter.getEntity().getReferredEntity();
                if (baseEntityDefinition.getEntityType().equals(EntityType.ANY)
                        && nlpjsEntityType.equals(intentDefinition.getName() + parameter.getName() + "Any")) {
                    /*
                     * The parameter is an any entity and the current NLP.js entity name follows the any entity
                     * convention IntentName_ParameterName_Any. We know this any entity is bound to this specific
                     * parameter and return it.
                     */
                    return Optional.of(parameter);
                }
                if (nlpjsEntityReferenceMapper.getReversedEntity(baseNlpjsEntityType).stream()
                        .anyMatch(entityType -> entityType.equals(baseEntityDefinition.getName()))
                        && (index == -1 || ++i == index)) {
                    /*
                     * The NLP.js entity type corresponds to the parameter type. Note that we need to search for
                     * it in a list because NLP.js entities may represent multiple Xatkit entities (e.g. date is
                     * used for various date formats supported by Xatkit).
                     */
                    return Optional.of(parameter);
                }
            } else if (parameter.getEntity().getReferredEntity() instanceof CustomEntityDefinition) {
                CustomEntityDefinition customEntityDefinition =
                        (CustomEntityDefinition) parameter.getEntity().getReferredEntity();
                if (customEntityDefinition.getName().equals(baseNlpjsEntityType) && (index == -1 || ++i == index)) {
                    return Optional.of(parameter);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Converts the provided NLP.js {@code intentName} to a Xatkit {@link IntentDefinition}.
     * <p>
     * This method transforms NLP.js default intent "None" to
     * {@link com.xatkit.core.recognition.IntentRecognitionProvider#DEFAULT_FALLBACK_INTENT}.
     * <p>
     * This method checks that the provided {@code intentName} matches an {@link IntentDefinition} in the
     * {@link EventDefinitionRegistry}.
     *
     * @param intentName the name of the NLP.js intent to convert
     * @return the {@link IntentDefinition} matching the provided {@code intentName}
     * @throws NullPointerException if the provided {@code intentName} is {@code null}
     */
    private IntentDefinition convertNlpjsIntentNameToIntentDefinition(@NonNull String intentName) {
        if (intentName.equals("None")) {
            return DEFAULT_FALLBACK_INTENT;
        }
        IntentDefinition result = eventRegistry.getIntentDefinition(intentName);
        if (isNull(result)) {
            Log.warn("Cannot retrieve the {0} with the provided name {1}, returning the Default Fallback Intent",
                    IntentDefinition.class.getSimpleName(), intentName);
            result = DEFAULT_FALLBACK_INTENT;
        }
        return result;
    }

    /**
     * Converts the provided {@code parameterValue} into a {@link String}.
     * <p>
     * This method supports {@link Number} values, and map them with the following format: {@code DecimalFormat("0
     * .###")}.
     * <p>
     * Not supported values produce a warning and are converted with {@link Object#toString()}. {@code Null} values
     * are converted to "null".
     *
     * @param parameterValue the value to convert
     * @return the {@link String} representation of the provided value
     */
    private @NonNull String convertParameterValueToString(@Nullable Object parameterValue) {
        if (isNull(parameterValue)) {
            return "null";
        }
        if (parameterValue instanceof String) {
            return (String) parameterValue;
        }
        if (parameterValue instanceof Number) {
            DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
            decimalFormatSymbols.setDecimalSeparator('.');
            DecimalFormat decimalFormat = new DecimalFormat("0.###", decimalFormatSymbols);
            decimalFormat.setGroupingUsed(false);
            return decimalFormat.format(parameterValue);
        }
        Log.warn("Cannot convert the provided value {0}", parameterValue);
        return parameterValue.toString();
    }
}