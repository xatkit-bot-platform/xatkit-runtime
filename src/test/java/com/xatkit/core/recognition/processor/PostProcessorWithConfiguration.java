package com.xatkit.core.recognition.processor;

import com.xatkit.execution.StateContext;
import com.xatkit.intent.RecognizedIntent;
import lombok.Getter;
import org.apache.commons.configuration2.Configuration;

/**
 * Stub class for
 * {@link com.xatkit.core.recognition.IntentRecognitionProviderFactoryTest#getIntentRecognitionProviderWithPostProcessorConstructedWithConfiguration()}.
 */
public class PostProcessorWithConfiguration implements IntentPostProcessor {

    @Getter
    private Configuration configuration;

    public PostProcessorWithConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public RecognizedIntent process(RecognizedIntent recognizedIntent, StateContext context) {
        return null;
    }
}
