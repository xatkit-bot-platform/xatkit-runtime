package com.xatkit.core.recognition.processor;

import com.xatkit.core.recognition.IntentRecognitionProviderFactoryTest;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.RecognizedIntent;

/**
 * Stub class for
 * {@link IntentRecognitionProviderFactoryTest#getIntentRecognitionProviderWithPreProcessor()}.
 */
public class PreProcessorNoConfiguration implements InputPreProcessor {

    @Override
    public String process(String input, StateContext context) {
        return null;
    }
}
