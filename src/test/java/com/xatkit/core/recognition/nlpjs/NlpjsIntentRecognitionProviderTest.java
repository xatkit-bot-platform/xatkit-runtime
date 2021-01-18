package com.xatkit.core.recognition.nlpjs;

import com.xatkit.core.recognition.IntentRecognitionProviderException;
import com.xatkit.core.recognition.IntentRecognitionProviderTest;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.library.core.CoreLibrary;
import com.xatkit.stubs.TestingStateContext;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.Test;

import static com.xatkit.stubs.TestingStateContextFactory.wrap;
import static org.assertj.core.api.Assertions.assertThat;

public class NlpjsIntentRecognitionProviderTest extends IntentRecognitionProviderTest<NlpjsIntentRecognitionProvider> {

    /*
     * Our connector uses a dedicated mapping for "any" intents containing training sentences entirely mapped to an
     * "any" entity. This test case ensures that the provider returns a correct RecognizedIntent for an input text
     * matching such intent.
     */
    @Test
    public void getIntentWithOnlyAnyEntity() throws IntentRecognitionProviderException {
        this.eventRegistry.registerEventDefinition(CoreLibrary.AnyValue);
        this.intentRecognitionProvider = getIntentRecognitionProvider();
        this.intentRecognitionProvider.registerIntentDefinition(CoreLibrary.AnyValue);
        this.intentRecognitionProvider.trainMLEngine();
        TestingStateContext context = wrap(this.intentRecognitionProvider.createContext("contextId"));
        context.enableIntents(CoreLibrary.AnyValue);
        RecognizedIntent recognizedIntent = this.intentRecognitionProvider.getIntent("This is an example", context);
        assertThat(recognizedIntent.getDefinition().getName()).isEqualTo(CoreLibrary.AnyValue.getName());
        assertThat(recognizedIntent.getRecognitionConfidence()).isEqualTo(1);
        assertThat(recognizedIntent.getValue("value")).isEqualTo("This is an example");
    }

    /*
     * Composite entities are not supported by our connector for now.
     */
    @Test(expected = IntentRecognitionProviderException.class)
    @Override
    public void getCompositeEntityIntent() throws IntentRecognitionProviderException {
        super.getCompositeEntityIntent();
    }

    /*
     * Composite entities are not supported by our connector for now.
     */
    @Test(expected = IntentRecognitionProviderException.class)
    @Override
    public void registerCompositeEntityReferencedEntitiesNotRegistered() throws IntentRecognitionProviderException {
        super.registerCompositeEntityReferencedEntitiesNotRegistered();
    }

    /*
     * Composite entities are not supported by our connector for now.
     */
    @Test(expected = IntentRecognitionProviderException.class)
    @Override
    public void registerCompositeEntityReferencedEntitiesAlreadyRegistered() throws IntentRecognitionProviderException {
        super.registerCompositeEntityReferencedEntitiesAlreadyRegistered();
    }

    /*
     * Deleting intents is not supported by our connector for now.
     */
    @Test(expected = UnsupportedOperationException.class)
    @Override
    public void deleteExistingIntent() throws IntentRecognitionProviderException {
        super.deleteExistingIntent();
    }

    /*
     * Deleting entities is not supported by our connector for now.
     */
    @Test(expected = UnsupportedOperationException.class)
    @Override
    public void deleteEntityNotReferenced() throws IntentRecognitionProviderException {
        super.deleteEntityNotReferenced();
    }

    @Override
    protected NlpjsIntentRecognitionProvider getIntentRecognitionProvider() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(NlpjsConfiguration.AGENT_ID_KEY, "default");
        configuration.addProperty(NlpjsConfiguration.NLPJS_SERVER_KEY, "http://localhost:8080");
        return new NlpjsIntentRecognitionProvider(eventRegistry, configuration, null);
    }
}
