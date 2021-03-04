package com.xatkit.core.recognition.processor;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.RecognizedIntent;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ToxicityPostProcessorTest extends AbstractXatkitTest {

    private ToxicityPostProcessor processor;
    private String perspectiveapiapiKey;
    private StateContext context;

    @Before
    public void setUp() {
        this.processor = null;
        this.perspectiveapiapiKey = "YOUR PERSPECTIVE API KEY";
        this.context = ExecutionFactory.eINSTANCE.createStateContext();
        this.context.setContextId("contextId");
    }

    @Test
    public void perspectiveapiProcessCommentForEachLanguage() {
        Set<String> perspectiveapiLanguages = this.getPerspectiveapiLanguages();
        for (String language : perspectiveapiLanguages) {
            Configuration botConfiguration = new BaseConfiguration();
            botConfiguration.setProperty("xatkit.perspectiveapi", true);
            botConfiguration.setProperty(PerspectiveapiConfiguration.API_KEY, perspectiveapiapiKey);
            botConfiguration.setProperty(PerspectiveapiConfiguration.LANGUAGE, language);
            processor = new ToxicityPostProcessor(botConfiguration);
            RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
            recognizedIntent.setMatchedInput("test");
            RecognizedIntent processedIntent = processor.process(recognizedIntent, context);
            ToxicityLabel[] languageAttributes =
                    processor.getPerspectiveapiClient().getLanguageAttributes().get(language);
            for (ToxicityLabel attribute : languageAttributes) {
                Double score = (Double) processedIntent.getNlpData()
                        .get(ToxicityPostProcessor.PERSPECTIVEAPI_PARAMETER_KEY + attribute.toString());
                assertThat(score).isBetween(0d, 1d);
            }
        }
    }

    @Test
    public void detoxifyProcessComment() {
        Configuration botConfiguration = new BaseConfiguration();
        botConfiguration.setProperty("xatkit.detoxify", true);
        botConfiguration.setProperty(DetoxifyInterfaceConfiguration.DETOXIFY_SERVER_URL, "http://localhost:8000");
        processor = new ToxicityPostProcessor(botConfiguration);
        RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
        recognizedIntent.setMatchedInput("test");
        RecognizedIntent processedIntent = processor.process(recognizedIntent, context);
        for (ToxicityLabel attribute : ToxicityLabel.values()) {
            Double score = (Double) processedIntent.getNlpData()
                    .get(ToxicityPostProcessor.DETOXIFY_PARAMETER_KEY + attribute.toString());
            assertThat(score).isBetween(0d, 1d);
        }
    }

    private Set<String> getPerspectiveapiLanguages() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(PerspectiveapiConfiguration.API_KEY, perspectiveapiapiKey);
        PerspectiveapiInterface perspectiveapiClient = new PerspectiveapiInterface(configuration);
        return perspectiveapiClient.getLanguageAttributes().keySet();
    }
}