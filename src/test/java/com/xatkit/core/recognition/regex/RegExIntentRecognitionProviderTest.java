package com.xatkit.core.recognition.regex;

import com.xatkit.core.recognition.IntentRecognitionProviderException;
import com.xatkit.core.recognition.IntentRecognitionProviderTest;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.State;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.RecognizedIntent;
import org.apache.commons.configuration2.BaseConfiguration;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        intentRecognitionProvider.registerEntityDefinition(intentProviderTestBot.getMappingEntity());
        intentRecognitionProvider.registerIntentDefinition(intentProviderTestBot.getMappingEntityIntent());
        StateContext context = ExecutionFactory.eINSTANCE.createStateContext();
        context.setContextId("contextId");
        RecognizedIntent recognizedIntent = intentRecognitionProvider.getIntent(("Give me some information about " +
                "Gwendal").toUpperCase(), context);
        assertThatRecognizedIntentHasDefinition(recognizedIntent,
                intentProviderTestBot.getMappingEntityIntent().getName());
        assertThatIntentContainsParameter(recognizedIntent, "name");
        assertThatIntentContainsParameterWithValue(recognizedIntent, "name", "Gwendal");
    }

    @Test
    public void getIntentValidIntentDefinitionNoOutContextUpperCase() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        intentRecognitionProvider.registerIntentDefinition(intentProviderTestBot.getSimpleIntent());
        StateContext context = ExecutionFactory.eINSTANCE.createStateContext();
        context.setContextId("contextId");
        context.setState(intentProviderTestBot.getModel().getInitState());
        RecognizedIntent recognizedIntent = intentRecognitionProvider.getIntent("Greetings".toUpperCase(),
                context);
        assertThatRecognizedIntentHasDefinition(recognizedIntent, intentProviderTestBot.getSimpleIntent().getName());
    }

    @Test
    public void getIntentValidIntentDefinitionWithReservedRegExpCharacters() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        intentDefinition.setName("TestReservedRegExpCharacters");
        intentDefinition.getTrainingSentences().add("$test");
        intentRecognitionProvider.registerIntentDefinition(intentDefinition);
        StateContext context = ExecutionFactory.eINSTANCE.createStateContext();
        context.setContextId("contextID");
        /*
         * We need to set the context manually because the intent is not part of the loaded model.
         */
        State mockedInitState = mock(State.class);
        context.setState(mockedInitState);
        when(mockedInitState.getAllAccessedIntents()).thenReturn(Collections.singletonList(intentDefinition));
        RecognizedIntent recognizedIntent = intentRecognitionProvider.getIntent("$test", context);
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
