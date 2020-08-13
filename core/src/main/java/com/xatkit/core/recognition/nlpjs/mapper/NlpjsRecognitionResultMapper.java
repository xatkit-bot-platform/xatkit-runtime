package com.xatkit.core.recognition.nlpjs.mapper;


import com.xatkit.core.EventDefinitionRegistry;
import com.xatkit.core.recognition.nlpjs.NlpjsConfiguration;
import com.xatkit.core.recognition.nlpjs.model.Classification;
import com.xatkit.core.recognition.nlpjs.model.RecognitionResult;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.RecognizedIntent;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

import static com.xatkit.core.recognition.IntentRecognitionProvider.DEFAULT_FALLBACK_INTENT;
import static java.util.Objects.isNull;

public class NlpjsRecognitionResultMapper {

    private NlpjsConfiguration configuration;

    private EventDefinitionRegistry eventRegistry;

    public NlpjsRecognitionResultMapper(@NonNull NlpjsConfiguration configuration,
                                  @NonNull EventDefinitionRegistry eventRegistry) {
        this.configuration = configuration;
        this.eventRegistry = eventRegistry;
    }

    public List<RecognizedIntent> mapResult(@NonNull RecognitionResult recognitionResult){
        List<Classification> classifications = recognitionResult.getClassifications();
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

    private IntentDefinition convertNlpjsIntentNameToIntentDefinition(@NonNull String intentName) {
        if(intentName.equals("None")) {
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


}
