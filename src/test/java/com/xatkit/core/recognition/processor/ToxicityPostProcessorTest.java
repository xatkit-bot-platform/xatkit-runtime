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

import static org.junit.Assert.*;

public class ToxicityPostProcessorTest extends AbstractXatkitTest {

    private ToxicityPostProcessor processor;
    private String perspectiveapiapiKey;
    private StateContext context;

    @Before
    public void setUp() {
        this.processor = null;
        this.perspectiveapiapiKey = "YOUR-PERSPECTIVEAPI-KEY";
        this.context = ExecutionFactory.eINSTANCE.createStateContext();
        this.context.setContextId("contextId");
    }

    @Test
    public void perspectiveapiProcessCommentForEachLanguage() {
        Set<String> perspectiveapiLanguages = this.getPerspectiveapiLanguages();
        for (String language : perspectiveapiLanguages) {
            Configuration botConfiguration = new BaseConfiguration();
            botConfiguration.setProperty("xatkit.perspectiveapi", true);
            botConfiguration.setProperty("xatkit.perspectiveapi.apiKey", perspectiveapiapiKey);
            botConfiguration.setProperty("xatkit.perspectiveapi.language", language);
            processor = new ToxicityPostProcessor(botConfiguration);
            RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
            recognizedIntent.setMatchedInput("test");
            RecognizedIntent processedIntent = processor.process(recognizedIntent, context);
            PerspectiveapiInterface.AttributeType[] languageAttributes =
                    processor.getPerspectiveapiClient().getLanguageAttributes().get(language);
            for (PerspectiveapiInterface.AttributeType attribute : languageAttributes) {
                Double score = (Double) processedIntent.getNlpData()
                        .get(ToxicityPostProcessor.PERSPECTIVEAPI_PARAMETER_KEY + attribute.toString());
                assertTrue(score >= 0);
                assertTrue(score <= 1);
            }
        }
    }

    @Test
    public void detoxifyProcessComment() {
        Configuration botConfiguration = new BaseConfiguration();
        botConfiguration.setProperty("xatkit.detoxify", true);
        processor = new ToxicityPostProcessor(botConfiguration);
        RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
        recognizedIntent.setMatchedInput("test");
        RecognizedIntent processedIntent = processor.process(recognizedIntent, context);
        for (DetoxifyInterface.AttributeType attribute : DetoxifyInterface.AttributeType.values()) {
            Double score = (Double) processedIntent.getNlpData()
                    .get(ToxicityPostProcessor.DETOXIFY_PARAMETER_KEY + attribute.toString());
            assertTrue(score >= 0);
            assertTrue(score <= 1);
        }
    }

    private Set<String> getPerspectiveapiLanguages() {
        PerspectiveapiInterface perspectiveapiClient = new PerspectiveapiInterface(perspectiveapiapiKey, null, null,
                null, null,
                null);
        return perspectiveapiClient.getLanguageAttributes().keySet();
    }
}