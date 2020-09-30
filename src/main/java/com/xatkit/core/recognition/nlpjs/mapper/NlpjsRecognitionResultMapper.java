package com.xatkit.core.recognition.nlpjs.mapper;


import com.xatkit.core.EventDefinitionRegistry;
import com.xatkit.core.recognition.nlpjs.NlpjsConfiguration;
import com.xatkit.core.recognition.nlpjs.NlpjsHelper;
import com.xatkit.core.recognition.nlpjs.model.Classification;
import com.xatkit.core.recognition.nlpjs.model.ExtractedEntity;
import com.xatkit.core.recognition.nlpjs.model.RecognitionResult;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.ContextParameterValue;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.RecognizedIntent;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.xatkit.core.recognition.IntentRecognitionProvider.DEFAULT_FALLBACK_INTENT;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class NlpjsRecognitionResultMapper {

    private NlpjsConfiguration configuration;

    private EventDefinitionRegistry eventRegistry;

    private NlpjsEntityReferenceMapper nlpjsEntityReferenceMapper;

    public NlpjsRecognitionResultMapper(@NonNull NlpjsConfiguration configuration,
                                  @NonNull EventDefinitionRegistry eventRegistry,
                                        @NonNull NlpjsEntityReferenceMapper nlpjsEntityReferenceMapper) {
        this.configuration = configuration;
        this.eventRegistry = eventRegistry;
        this.nlpjsEntityReferenceMapper = nlpjsEntityReferenceMapper;
    }

    public List<RecognizedIntent> mapRecognitionResult(@NonNull RecognitionResult recognitionResult){
        List<Classification> classifications = recognitionResult.getClassifications();
        classifications = classifications.stream().filter(c -> c.getScore() > 0.1).collect(Collectors.toList());
        List<RecognizedIntent> recognizedIntents = new ArrayList<>();
        for(Classification classification: classifications){
            RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
            IntentDefinition intentDefinition = convertNlpjsIntentNameToIntentDefinition(classification.getIntent());
            recognizedIntent.setDefinition(intentDefinition);
            recognizedIntent.setRecognitionConfidence(classification.getScore());
            recognizedIntent.setMatchedInput(recognitionResult.getUtterance());
            recognizedIntents.add(recognizedIntent);
        }

        return  recognizedIntents;
    }

    public List<ContextParameterValue> mapParameterValues(RecognizedIntent recognizedIntent,
                                                          List<ExtractedEntity> extractedEntities) {
        List<ContextParameterValue> contextParameterValues = new ArrayList<>();
        for (ExtractedEntity extractedEntity: extractedEntities) {
            String entityType = extractedEntity.getEntity();
                ContextParameter contextParameter = NlpjsHelper.getContextParameterFromNlpEntity(entityType,
                        (IntentDefinition) recognizedIntent.getDefinition(), nlpjsEntityReferenceMapper);
                if (nonNull(contextParameter)) {
                    ContextParameterValue contextParameterValue =
                            IntentFactory.eINSTANCE.createContextParameterValue();
                    if (nonNull(extractedEntity.getValue())) {
                        contextParameterValue.setValue(convertParameterValueToString(extractedEntity.getValue()));
                    } else {
                        Log.warn("Cannot retrieve the value for the context parameter {0}", contextParameter.getName());
                    }
                    contextParameterValue.setContextParameter(contextParameter);
                    contextParameterValues.add(contextParameterValue);
                }
        }
        /*
         * Warning: do not add the context parameters to the recognized intent, otherwise they will be added twice
         * (they are already added in NlpjsIntentRecognitionProvider#getIntentInternal).
         */
        return contextParameterValues;
    }

    private IntentDefinition convertNlpjsIntentNameToIntentDefinition(@NonNull String intentName) {
        if (intentName.equals("None")) {
            return DEFAULT_FALLBACK_INTENT;
        }
        IntentDefinition result = eventRegistry.getIntentDefinition(intentName);
        if (isNull(result)) {
            Log.warn("Cannot retrieve the {0} with the provided name {1}, returning the Default Fallback Intent",
                    IntentDefinition.class.getSimpleName(),intentName);
            result = DEFAULT_FALLBACK_INTENT;
        }
        return result;
    }

    private String convertParameterValueToString(@NonNull Object parameterValue) {
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
        Log.error("Cannot convert the provided value {0}", parameterValue);
        return "";
    }
}