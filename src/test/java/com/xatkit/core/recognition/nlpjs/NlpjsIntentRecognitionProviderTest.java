package com.xatkit.core.recognition.nlpjs;

import com.xatkit.core.recognition.IntentRecognitionProviderException;
import com.xatkit.core.recognition.IntentRecognitionProviderTest;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.library.core.CoreLibrary;
import com.xatkit.stubs.TestingStateContext;
import com.xatkit.test.util.VariableLoaderHelper;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.Test;

import static com.xatkit.dsl.DSL.countyGb;
import static com.xatkit.stubs.TestingStateContextFactory.wrap;
import static org.assertj.core.api.Assertions.assertThat;

public class NlpjsIntentRecognitionProviderTest extends IntentRecognitionProviderTest<NlpjsIntentRecognitionProvider> {

    /*
     * Check that the constructor is failing if the NLP.js server is not reachable.
     * Related to <a href="https://github.com/xatkit-bot-platform/xatkit-runtime/issues/310">#310</a>.
     */
    @Test (expected = RuntimeException.class)
    public void constructUnreachableNlpjsServer() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(NlpjsConfiguration.AGENT_ID_KEY, "default");
        configuration.addProperty(NlpjsConfiguration.NLPJS_SERVER_KEY, "invalid");
        this.intentRecognitionProvider = new NlpjsIntentRecognitionProvider(eventRegistry, configuration, null);
    }

    /*
     * This test ensures that registering a base entity that is not supported by NLP.js doesn't throw any exception.
     * It was the case before, but now Xatkit gracefully degrades to using "any" entities if a base entity is not
     * supported.
     */
    @Test
    public void registerEntityDefinitionNotSupportedBaseEntity() throws IntentRecognitionProviderException {
        this.intentRecognitionProvider = getIntentRecognitionProvider();
        this.intentRecognitionProvider.registerEntityDefinition(countyGb().getReferredEntity());
    }

    /*
     * The NLP.js connector uses "any" entities if the provided base entity is not supported. This means that the
     * intent should be more permissive and match more input. This test ensures that matching is correctly done after
     * degrading an unsupported base entity to "any".
     */
    @Test
    public void getIntentNotSupportedBaseEntity() throws IntentRecognitionProviderException {
        this.eventRegistry.registerEventDefinition(CoreLibrary.CountyGBValue);
        this.intentRecognitionProvider = getIntentRecognitionProvider();
        this.intentRecognitionProvider.registerIntentDefinition(CoreLibrary.CountyGBValue);
        this.intentRecognitionProvider.trainMLEngine();
        TestingStateContext context = wrap(this.intentRecognitionProvider.createContext("contextId"));
        context.enableIntents(CoreLibrary.CountyGBValue);
        RecognizedIntent recognizedIntent = this.intentRecognitionProvider.getIntent("London", context);
        assertThat(recognizedIntent.getDefinition().getName()).isEqualTo(CoreLibrary.CountyGBValue.getName());
        assertThat(recognizedIntent.getRecognitionConfidence()).isEqualTo(1);
        assertThat(recognizedIntent.getValue("value")).isEqualTo("London");
    }

    /*
     * The NLP.js connector uses a dedicated mapping for "any" intents containing training sentences entirely mapped
     * to an "any" entity. This test case ensures that the provider returns a correct RecognizedIntent for an input text
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
    @Test(expected = IllegalArgumentException.class)
    @Override
    public void getCompositeEntityIntent() throws IntentRecognitionProviderException {
        super.getCompositeEntityIntent();
    }

    /*
     * Composite entities are not supported by our connector for now.
     */
    @Test(expected = IllegalArgumentException.class)
    @Override
    public void registerCompositeEntityReferencedEntitiesNotRegistered() throws IntentRecognitionProviderException {
        super.registerCompositeEntityReferencedEntitiesNotRegistered();
    }

    /*
     * Composite entities are not supported by our connector for now.
     */
    @Test(expected = IllegalArgumentException.class)
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
        configuration.addProperty(NlpjsConfiguration.AGENT_ID_KEY,
                VariableLoaderHelper.getVariable(NlpjsConfiguration.AGENT_ID_KEY));
        configuration.addProperty(NlpjsConfiguration.NLPJS_SERVER_KEY,
                VariableLoaderHelper.getVariable(NlpjsConfiguration.NLPJS_SERVER_KEY));
        configuration.addProperty(NlpjsConfiguration.NLPJS_SERVER_BASICAUTH_USERNAME_KEY,
                VariableLoaderHelper.getVariable(NlpjsConfiguration.NLPJS_SERVER_BASICAUTH_USERNAME_KEY));
        configuration.addProperty(NlpjsConfiguration.NLPJS_SERVER_BASICAUTH_PASSWORD_KEY,
                VariableLoaderHelper.getVariable(NlpjsConfiguration.NLPJS_SERVER_BASICAUTH_PASSWORD_KEY));
        return new NlpjsIntentRecognitionProvider(eventRegistry, configuration, null);
    }
}
