package com.xatkit.core.recognition.processor;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.test.util.VariableLoaderHelper;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class LanguageDetectionPostProcessorTest extends AbstractXatkitTest {

    private LanguageDetectionPostProcessor processor;
    private String modelPath;
    private StateContext context;

    @Before
    public void setUp() {
        this.processor = null;
        this.modelPath =
                VariableLoaderHelper.getVariable(LanguageDetectionPostProcessor.OPENNLP_MODEL_PATH_PARAMETER_KEY);
        this.context = ExecutionFactory.eINSTANCE.createStateContext();
        this.context.setContextId("contextId");
    }

    @Ignore
    @Test
    public void processText() {
        Configuration botConfiguration = new BaseConfiguration();
        botConfiguration.setProperty(LanguageDetectionPostProcessor.MAX_NUM_LANGUAGES_IN_SCORE, 999);
        botConfiguration.setProperty(LanguageDetectionPostProcessor.OPENNLP_MODEL_PATH_PARAMETER_KEY, modelPath);
        processor = new LanguageDetectionPostProcessor(botConfiguration);
        int supportedLanguages = processor.getLanguageDetector().getSupportedLanguages().length;
        RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
        recognizedIntent.setMatchedInput("test");
        RecognizedIntent processedIntent = processor.process(recognizedIntent, context);

        LanguageDetectionScore score1 =
                (LanguageDetectionScore) processedIntent.getNlpData()
                        .get(LanguageDetectionPostProcessor.OPENNLP_LAST_INPUT_SCORE_PARAMETER_KEY);
        List<String> languages1 = score1.getLanguageNames();
        List<Double> confidences1 = score1.getLanguageConfidences();

        assertThat(languages1.size()).isEqualTo(supportedLanguages);
        assertThat(confidences1.size()).isEqualTo(supportedLanguages);

        for (int i = 0; i < confidences1.size(); i++) {
            Double score = score1.getConfidence(languages1.get(i));
            assertThat(score).isBetween(0d, 1d);
        }
        LanguageDetectionScore scoreN = (LanguageDetectionScore) context.getSession()
                .get(LanguageDetectionPostProcessor.OPENNLP_LAST_N_INPUTS_SCORE_PARAMETER_KEY);
        List<String> languagesN = scoreN.getLanguageNames();
        List<Double> confidencesN = scoreN.getLanguageConfidences();

        assertThat(languagesN.size()).isEqualTo(supportedLanguages);
        assertThat(confidencesN.size()).isEqualTo(supportedLanguages);

        for (int i = 0; i < confidencesN.size(); i++) {
            Double score = score1.getConfidence(languagesN.get(i));
            assertThat(score).isBetween(0d, 1d);
        }

    }
}
