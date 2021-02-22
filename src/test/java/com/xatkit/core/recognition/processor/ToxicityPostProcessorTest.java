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
    private String apiKey;
    private StateContext context;
    private Set<String> languages;

    @Before
    public void setUp() {
        this.apiKey = System.getenv("PERSPECTIVEAPI_KEY");
        Configuration botConfiguration = new BaseConfiguration();
        botConfiguration.setProperty("xatkit.perspectiveapi.apiKey", apiKey);
        this.processor = new ToxicityPostProcessor(botConfiguration);
        this.languages = processor.getClient().getLanguageAttributes().keySet();
        this.context = ExecutionFactory.eINSTANCE.createStateContext();
        this.context.setContextId("contextId");
    }

    @Test
    public void processCommentForEachLanguage() {
        for (String language : languages) {
            Configuration botConfiguration = new BaseConfiguration();
            botConfiguration.setProperty("xatkit.perspectiveapi.apiKey", apiKey);
            botConfiguration.setProperty("xatkit.perspectiveapi.language", language);
            processor = new ToxicityPostProcessor(botConfiguration);
            RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
            recognizedIntent.setMatchedInput("test");
            RecognizedIntent processedIntent = processor.process(recognizedIntent, context);
            PerspectiveapiInterface.AttributeType[] languageAttributes =
                    processor.getClient().getLanguageAttributes().get(language);
            for (PerspectiveapiInterface.AttributeType attribute : languageAttributes) {
                Double score = (Double) processedIntent.getNlpData()
                        .get(ToxicityPostProcessor.TOXICITY_PARAMETER_KEY + attribute.toString());
                assertTrue(score >= 0);
                assertTrue(score <= 1);
            }
        }

    }
}