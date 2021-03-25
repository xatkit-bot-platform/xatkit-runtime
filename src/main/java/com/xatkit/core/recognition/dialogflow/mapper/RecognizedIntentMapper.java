package com.xatkit.core.recognition.dialogflow.mapper;

import com.google.cloud.dialogflow.v2.Context;
import com.google.cloud.dialogflow.v2.ContextName;
import com.google.cloud.dialogflow.v2.Intent;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.protobuf.Value;
import com.xatkit.core.EventDefinitionRegistry;
import com.xatkit.core.recognition.dialogflow.DialogFlowConfiguration;
import com.xatkit.intent.BaseEntityDefinition;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.ContextParameterValue;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.RecognizedIntent;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Map;

import static com.xatkit.core.recognition.IntentRecognitionProvider.DEFAULT_FALLBACK_INTENT;
import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Maps DialogFlow {@link QueryResult} to {@link RecognizedIntent}s.
 * <p>
 * This class allows to wrap {@link QueryResult} instances into generic {@link RecognizedIntent} that can be
 * manipulated by the core components.
 */
public class RecognizedIntentMapper {

    /**
     * The {@link DialogFlowConfiguration}.
     * <p>
     * This configuration is used to check whether the created {@link RecognizedIntent} has a confidence level higher
     * than the set threshold. If not the returned {@link RecognizedIntent}'s definition is set to
     * {@link com.xatkit.core.recognition.IntentRecognitionProvider#DEFAULT_FALLBACK_INTENT}.
     */
    private DialogFlowConfiguration configuration;

    /**
     * The Xatkit {@link EventDefinitionRegistry} used to retrieve the {@link RecognizedIntent}'s definitions.
     */
    private EventDefinitionRegistry eventRegistry;

    /**
     * Constructs a {@link RecognizedIntentMapper} with the provided {@code configuration} and {@code eventRegistry}.
     *
     * @param configuration the {@link DialogFlowConfiguration}
     * @param eventRegistry the {@link EventDefinitionRegistry} used to retrieve the {@link RecognizedIntent}'s
     *                      definitions
     * @throws NullPointerException if the provided {@code configuration} or {@code eventRegistry} is {@code null}
     */
    public RecognizedIntentMapper(@NonNull DialogFlowConfiguration configuration,
                                  @NonNull EventDefinitionRegistry eventRegistry) {
        this.configuration = configuration;
        this.eventRegistry = eventRegistry;
    }

    /**
     * Reifies the provided DialogFlow {@link QueryResult} into a {@link RecognizedIntent}.
     * <p>
     * This method relies on the {@link #convertDialogFlowIntentToIntentDefinition(Intent)} method to retrieve the
     * {@link IntentDefinition} associated to the {@link QueryResult}'s {@link Intent}, and the
     * {@link IntentDefinition#getParameter(String)} method to retrieve the {@link ContextParameter}s to set the
     * value of from the DialogFlow contexts.
     *
     * @param result the DialogFlow {@link QueryResult} containing the {@link Intent} to reify
     * @return the reified {@link RecognizedIntent}
     * @throws NullPointerException     if the provided {@link QueryResult} is {@code null}
     * @throws IllegalArgumentException if the provided {@link QueryResult}'s {@link Intent} is {@code null}
     * @see #convertDialogFlowIntentToIntentDefinition(Intent)
     * @see IntentDefinition#getParameter(String)
     */
    public RecognizedIntent mapQueryResult(@NonNull QueryResult result) {
        checkArgument(nonNull(result.getIntent()), "Cannot create a %s from the provided %s'%s %s", RecognizedIntent
                .class.getSimpleName(), QueryResult.class.getSimpleName(), Intent.class.getSimpleName(), result
                .getIntent());
        Intent intent = result.getIntent();
        RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
        /*
         * Retrieve the IntentDefinition corresponding to this Intent.
         */
        IntentDefinition intentDefinition = convertDialogFlowIntentToIntentDefinition(intent);
        recognizedIntent.setDefinition(intentDefinition);

        /*
         * Reuse the QueryResult values to set the recognition confidence and the matched input, DialogFlow already
         * provides confidence for each matched intent.
         */
        recognizedIntent.setRecognitionConfidence(result.getIntentDetectionConfidence());
        recognizedIntent.setMatchedInput(result.getQueryText());

        /*
         * Handle the confidence threshold: if the matched intent's confidence is lower than the defined threshold we
         *  set its definition to DEFAULT_FALLBACK_INTENT and we skip context registration.
         */
        if (!recognizedIntent.getDefinition().equals(DEFAULT_FALLBACK_INTENT)
                && recognizedIntent.getRecognitionConfidence() < this.configuration.getConfidenceThreshold()) {
            boolean containsAnyEntity =
                    recognizedIntent.getDefinition().getParameters().stream().anyMatch(
                            p -> p.getEntity().getReferredEntity() instanceof BaseEntityDefinition
                                    && ((BaseEntityDefinition) p.getEntity().getReferredEntity()).getEntityType()
                                    .equals(com.xatkit.intent.EntityType.ANY)
                    );
            /*
             * We should not reject a recognized intent if it contains an any entity, these intents typically have a
             * low confidence level.
             */
            if (!containsAnyEntity) {
                Log.debug("Confidence for matched intent {0} (input = \"{1}\", confidence = {2}) is lower than the "
                                + "configured threshold ({3}), overriding the matched intent with {4}",
                        recognizedIntent.getDefinition().getName(), recognizedIntent.getMatchedInput(),
                        recognizedIntent.getRecognitionConfidence(), this.configuration.getConfidenceThreshold(),
                        DEFAULT_FALLBACK_INTENT.getName());
                recognizedIntent.setDefinition(DEFAULT_FALLBACK_INTENT);
                return recognizedIntent;
            } else {
                Log.debug("Detected a low-confidence value for the intent {0} (inputs = \"{1}\", confidence = {2}). "
                        + "The intent has not been filtered out because it contains an any entity");
            }
        }
        /*
         * Set the output context values.
         */
        for (Context context : result.getOutputContextsList()) {
            String contextName = ContextName.parse(context.getName()).getContext();
            Log.debug("Processing context {0}", context.getName());
            Map<String, Value> parameterValues = context.getParameters().getFieldsMap();
            for (String key : parameterValues.keySet()) {
                Value value = parameterValues.get(key);

                Object parameterValue = buildParameterValue(value);

                ContextParameter contextParameter = intentDefinition.getParameter(key);
                if (nonNull(contextParameter) && !key.contains(".original")) {
                    ContextParameterValue contextParameterValue =
                            IntentFactory.eINSTANCE.createContextParameterValue();
                    contextParameterValue.setContextParameter(contextParameter);
                    contextParameterValue.setValue(parameterValue);
                    recognizedIntent.getValues().add(contextParameterValue);
                }
            }
        }
        return recognizedIntent;
    }

    /**
     * Reifies the provided DialogFlow {@code intent} into an Xatkit {@link IntentDefinition}.
     * <p>
     * This method looks in the {@link EventDefinitionRegistry} for an {@link IntentDefinition} associated to the
     * provided {@code intent}'s name and returns it. If there is no such {@link IntentDefinition} the
     * {@code DEFAULT_FALLBACK_INTENT} is returned.
     *
     * @param intent the DialogFlow {@link Intent} to retrieve the Xatkit {@link IntentDefinition} from
     * @return the {@link IntentDefinition} associated to the provided {@code intent}
     * @throws NullPointerException if the provided {@code intent} is {@code null}
     */
    private IntentDefinition convertDialogFlowIntentToIntentDefinition(@NonNull Intent intent) {
        IntentDefinition result = eventRegistry.getIntentDefinition(intent.getDisplayName());
        if (isNull(result)) {
            Log.warn("Cannot retrieve the {0} with the provided name {1}, returning the Default Fallback Intent",
                    IntentDefinition.class.getSimpleName(), intent.getDisplayName());
            result = DEFAULT_FALLBACK_INTENT;
        }
        return result;
    }

    /**
     * Build a context parameter value from the provided protobuf {@link Value}.
     * <p>
     * The returned value is assignable to {@link ContextParameterValue#setValue(Object)}, and is either a
     * {@link String}, or a {@link Map} for nested struct {@link Value}s.
     *
     * @param fromValue the protobuf {@link Value} to translate
     * @return the context parameter value
     * @throws NullPointerException if the provided {@code fromValue} is {@code null}
     */
    private Object buildParameterValue(@NonNull Value fromValue) {
        if (fromValue.getKindCase().equals(Value.KindCase.STRUCT_VALUE)) {
            Map<String, Object> parameterMap = new HashMap<>();
            fromValue.getStructValue().getFieldsMap().forEach((key, value) -> {
                if (!key.contains(".original")) {
                    /*
                     * Remove .original in inner structures, we don't need them
                     */
                    Object adaptedValue = buildParameterValue(value);
                    parameterMap.put(key, adaptedValue);
                }
            });
            return parameterMap;
        } else {
            return convertParameterValueToString(fromValue);
        }
    }

    /**
     * Converts the provided {@code value} into a {@link String}.
     * <p>
     * This method converts protobuf's {@link Value}s returned by DialogFlow into {@link String}s that can be
     * assigned to {@link ContextParameterValue}s.
     *
     * @param value the protobuf {@link Value} to convert
     * @return the {@link String} representation of the provided {@code value}.
     * @throws NullPointerException if the provided {@code value} is {@code null}
     */
    protected String convertParameterValueToString(@NonNull Value value) {
        switch (value.getKindCase()) {
            case STRING_VALUE:
                return value.getStringValue();
            case NUMBER_VALUE:
                DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
                decimalFormatSymbols.setDecimalSeparator('.');
                DecimalFormat decimalFormat = new DecimalFormat("0.###", decimalFormatSymbols);
                decimalFormat.setGroupingUsed(false);
                return decimalFormat.format(value.getNumberValue());
            case BOOL_VALUE:
                return Boolean.toString(value.getBoolValue());
            case NULL_VALUE:
                return "null";
            default:
                /*
                 * Includes LIST_VALUE and STRUCT_VALUE
                 */
                Log.error("Cannot convert the provided value {0}", value);
                return "";
        }
    }
}
