package com.xatkit.core.recognition.processor;

import com.xatkit.core.recognition.IntentRecognitionProviderFactoryTest;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.RecognizedIntent;
import lombok.Getter;
import org.apache.commons.configuration2.Configuration;

/**
 * Stub class for
 * {@link IntentRecognitionProviderFactoryTest#getIntentRecognitionProviderWithPostProcessor()}.
 */
public class PostProcessorNoConfiguration implements IntentPostProcessor {

    @Override
    public RecognizedIntent process(RecognizedIntent recognizedIntent, StateContext context) {
        return null;
    }
}
