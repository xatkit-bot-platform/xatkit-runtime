package com.xatkit.core.recognition.processor;

import com.xatkit.execution.StateContext;
import com.xatkit.intent.RecognizedIntent;

public class TrimParameterValuesPostProcessor implements IntentPostProcessor {

    @Override
    public RecognizedIntent process(RecognizedIntent recognizedIntent, StateContext context) {
        recognizedIntent.getValues().forEach(v -> {
            if (v.getValue() instanceof String) {
                v.setValue(((String) v.getValue()).trim());
            }
        });
        return recognizedIntent;
    }
}
