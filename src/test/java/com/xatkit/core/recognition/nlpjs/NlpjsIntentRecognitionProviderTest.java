package com.xatkit.core.recognition.nlpjs;

import com.xatkit.core.recognition.IntentRecognitionProviderException;
import com.xatkit.core.recognition.IntentRecognitionProviderTest;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.Test;

public class NlpjsIntentRecognitionProviderTest extends IntentRecognitionProviderTest<NlpjsIntentRecognitionProvider> {

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
