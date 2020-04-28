package com.xatkit.core.recognition.regex;

import com.xatkit.core.recognition.IntentRecognitionProviderException;
import com.xatkit.core.recognition.IntentRecognitionProviderTest;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.ContextInstance;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.util.ExecutionModelUtils;
import org.apache.commons.configuration2.BaseConfiguration;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RegExIntentRecognitionProviderTest extends IntentRecognitionProviderTest<RegExIntentRecognitionProvider> {

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        intentRecognitionProvider = new RegExIntentRecognitionProvider(null, null);
    }

    @Test
    public void constructValidConfiguration() {
        intentRecognitionProvider = new RegExIntentRecognitionProvider(new BaseConfiguration(), null);
        assertThat(intentRecognitionProvider.isShutdown()).as("Provider not shut down").isFalse();
    }

    @Ignore
    /*
     * Should be enabled to test #261 (https://github.com/xatkit-bot-platform/xatkit-runtime/issues/261)
     */
    @Test
    public void getIntentValidIntentDefinitionWithOutContextMappingUpperCase() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        intentRecognitionProvider.registerEntityDefinition(testBotExecutionModel.getMappingEntity());
        intentRecognitionProvider.registerIntentDefinition(testBotExecutionModel.getMappingEntityIntent());
        RecognizedIntent recognizedIntent = intentRecognitionProvider.getIntent(("Give me some information about " +
                "Gwendal").toUpperCase(), new XatkitSession("sessionID"));
        assertThatRecognizedIntentHasDefinition(recognizedIntent,
                testBotExecutionModel.getMappingEntityIntent().getName());
        ContextInstance context = recognizedIntent.getOutContextInstance("Founder");
        assertThatContextContainsParameterValue(context, "name");
        assertThatContextContainsParameterWithValue(context, "name", "Gwendal");
    }

    @Test
    public void getIntentValidIntentDefinitionNoOutContextUpperCase() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        intentRecognitionProvider.registerIntentDefinition(testBotExecutionModel.getSimpleIntent());
        XatkitSession session = new XatkitSession("sessionId");
        session.setState(ExecutionModelUtils.getInitState(testBotExecutionModel.getBaseModel()));
        RecognizedIntent recognizedIntent = intentRecognitionProvider.getIntent("Greetings".toUpperCase(),
                session);
        assertThatRecognizedIntentHasDefinition(recognizedIntent, testBotExecutionModel.getSimpleIntent().getName());
    }

    @Test
    public void getIntentValidIntentDefinitionWithReservedRegExpCharacters() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        intentDefinition.setName("TestReservedRegExpCharacters");
        intentDefinition.getTrainingSentences().add("$test");
        intentRecognitionProvider.registerIntentDefinition(intentDefinition);
        XatkitSession session = new XatkitSession("sessionID");
        /*
         * We need to set the context manually because the intent is not part of the loaded model.
         */
        session.getRuntimeContexts().setContext("Enable" + intentDefinition.getName(), 1);
        session.setState(ExecutionModelUtils.getInitState(testBotExecutionModel.getBaseModel()));
        RecognizedIntent recognizedIntent = intentRecognitionProvider.getIntent("$test", session);
        assertThatRecognizedIntentHasDefinition(recognizedIntent, intentDefinition.getName());
    }

    @Ignore
    @Test
    @Override
    public void getCompositeEntityIntent() throws IntentRecognitionProviderException {
        /*
         * Composite entities are not supported in the RegExp provider (see https://github
         * .com/xatkit-bot-platform/xatkit-runtime/issues/272)
         */
        super.getCompositeEntityIntent();
    }

    @Override
    protected RegExIntentRecognitionProvider getIntentRecognitionProvider() {
        return new RegExIntentRecognitionProvider(new BaseConfiguration(), null);
    }
}
