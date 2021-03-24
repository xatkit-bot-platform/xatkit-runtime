package com.xatkit.core.recognition.processor;

import com.xatkit.core.recognition.processor.toxicity.perspectiveapi.PerspectiveApiConfiguration;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.test.util.VariableLoaderHelper;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LanguageDetectionPostProcessorTest {

    private LanguageDetectionPostProcessor processor;
    private String modelPath;
    private StateContext context;

    @Before
    public void setUp() throws Exception {
        this.processor = null;
        this.modelPath = VariableLoaderHelper.getVariable(LanguageDetectionPostProcessor.OPENNLP_MODEL_PATH_PARAMETER_KEY);
        this.context = ExecutionFactory.eINSTANCE.createStateContext();
        this.context.setContextId("contextId");
    }

    /**
     * .
     */
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
        String[] languages1 = score1.getLanguageNames();
        Double[] confidences1 = score1.getLanguageConfidences();

        assertThat(languages1.length).isEqualTo(supportedLanguages);
        assertThat(confidences1.length).isEqualTo(supportedLanguages);

        for (int i = 0; i < confidences1.length; i++) {
            Double score = score1.getScore(languages1[i]);
            assertThat(score).isBetween(0d, 1d);
        }
        LanguageDetectionScore scoreN = (LanguageDetectionScore) context.getSession()
                .get(LanguageDetectionPostProcessor.OPENNLP_LAST_N_INPUTS_SCORE_PARAMETER_KEY);
        String[] languagesN = scoreN.getLanguageNames();
        Double[] confidencesN = scoreN.getLanguageConfidences();

        assertThat(languagesN.length).isEqualTo(supportedLanguages);
        assertThat(confidencesN.length).isEqualTo(supportedLanguages);

        for (int i = 0; i < confidencesN.length; i++) {
            Double score = score1.getScore(languagesN[i]);
            assertThat(score).isBetween(0d, 1d);
        }

    }
}
