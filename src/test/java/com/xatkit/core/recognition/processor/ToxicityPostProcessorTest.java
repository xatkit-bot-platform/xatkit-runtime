package com.xatkit.core.recognition.processor;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.recognition.processor.toxicity.detoxify.DetoxifyConfiguration;
import com.xatkit.core.recognition.processor.toxicity.perspectiveapi.PerspectiveApiClient;
import com.xatkit.core.recognition.processor.toxicity.perspectiveapi.PerspectiveApiConfiguration;
import com.xatkit.core.recognition.processor.toxicity.perspectiveapi.PerspectiveApiLabel;
import com.xatkit.core.recognition.processor.toxicity.perspectiveapi.PerspectiveApiScore;
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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore("Tests aren't working at the moment, to be improved")
public class ToxicityPostProcessorTest extends AbstractXatkitTest {

    private ToxicityPostProcessor processor;
    private String perspectiveApiKey;
    private StateContext context;

    @Before
    public void setUp() {
        this.processor = null;
        this.perspectiveApiKey = VariableLoaderHelper.getVariable(PerspectiveApiConfiguration.API_KEY);
        this.context = ExecutionFactory.eINSTANCE.createStateContext();
        this.context.setContextId("contextId");
    }

    @Test
    public void perspectiveApiProcessIntentForEachLanguage() {
        Set<String> perspectiveApiLanguages = this.getPerspectiveApiLanguages();
        for (String language : perspectiveApiLanguages) {
            Configuration botConfiguration = new BaseConfiguration();
            botConfiguration.setProperty("xatkit.perspectiveapi", true);
            botConfiguration.setProperty(PerspectiveApiConfiguration.API_KEY, perspectiveApiKey);
            botConfiguration.setProperty(PerspectiveApiConfiguration.LANGUAGE, language);
            processor = new ToxicityPostProcessor(botConfiguration);
            RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
            recognizedIntent.setMatchedInput("test");
            RecognizedIntent processedIntent = processor.process(recognizedIntent, context);
            PerspectiveApiLabel[] languageLabels =
                    processor.getPerspectiveapiClient().getLanguageLabels().get(language);
            PerspectiveApiScore scores =
                    (PerspectiveApiScore) processedIntent.getNlpData().get(ToxicityPostProcessor.PERSPECTIVEAPI_PARAMETER_KEY);
            for (PerspectiveApiLabel label : languageLabels) {
                Double score = scores.getScore(label);
                assertThat(score).isBetween(0d, 1d);
            }
        }
    }

    @Ignore
    @Test
    public void detoxifyProcessIntent() {
        Configuration botConfiguration = new BaseConfiguration();
        botConfiguration.setProperty("xatkit.detoxify", true);
        botConfiguration.setProperty(DetoxifyConfiguration.DETOXIFY_SERVER_URL, "http://localhost:8000");
        processor = new ToxicityPostProcessor(botConfiguration);
        RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
        recognizedIntent.setMatchedInput("test");
        RecognizedIntent processedIntent = processor.process(recognizedIntent, context);
        for (PerspectiveApiLabel attribute : PerspectiveApiLabel.values()) {
            Double score = (Double) processedIntent.getNlpData()
                    .get(ToxicityPostProcessor.DETOXIFY_PARAMETER_KEY + attribute.toString());
            assertThat(score).isBetween(0d, 1d);
        }
    }

    private Set<String> getPerspectiveApiLanguages() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(PerspectiveApiConfiguration.API_KEY, perspectiveApiKey);
        PerspectiveApiClient perspectiveapiClient = new PerspectiveApiClient(configuration);
        return perspectiveapiClient.getLanguageLabels().keySet();
    }
}
