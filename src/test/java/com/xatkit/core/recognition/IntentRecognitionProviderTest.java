package com.xatkit.core.recognition;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.EventDefinitionRegistry;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.ContextParameterValue;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.test.bot.IntentProviderTestBot;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class IntentRecognitionProviderTest<T extends IntentRecognitionProvider> extends AbstractXatkitTest {

    protected static IntentProviderTestBot intentProviderTestBot;

    @BeforeClass
    public static void setUpBeforeClass() {
        intentProviderTestBot = new IntentProviderTestBot();
    }

    protected T intentRecognitionProvider;

    protected EventDefinitionRegistry eventRegistry;

    protected IntentDefinition registeredIntentDefinition;

    protected List<EntityDefinition> registeredEntityDefinitions = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        eventRegistry = new EventDefinitionRegistry();
        eventRegistry.registerEventDefinition(intentProviderTestBot.getSimpleIntent());
        eventRegistry.registerEventDefinition(intentProviderTestBot.getSystemEntityIntent());
        eventRegistry.registerEventDefinition(intentProviderTestBot.getMappingEntityIntent());
        eventRegistry.registerEventDefinition(intentProviderTestBot.getCompositeEntityIntent());
    }

    @After
    public void tearDown() throws Exception {
        /*
         * Default implementation that shuts down the provider. Advanced providers may need to override this method
         * to perform advanced cleaning (e.g. delete registered intents and entities).
         */
        if (nonNull(intentRecognitionProvider)) {
            if (!intentRecognitionProvider.isShutdown()) {
                intentRecognitionProvider.shutdown();
            }
        }
        this.registeredIntentDefinition = null;
        this.registeredEntityDefinitions.clear();
    }

    @Test(expected = NullPointerException.class)
    public void registerNullIntent() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        intentRecognitionProvider.registerIntentDefinition(null);
    }

    @Test
    public void registerSimpleIntent() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredIntentDefinition = intentProviderTestBot.getSimpleIntent();
        intentRecognitionProvider.registerIntentDefinition(registeredIntentDefinition);
        /*
         * Nothing to check, the method does not return anything and does not change any visible state.
         */
    }

    @Test
    public void registerSystemEntityIntent() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredIntentDefinition = intentProviderTestBot.getSystemEntityIntent();
        intentRecognitionProvider.registerIntentDefinition(registeredIntentDefinition);
        /*
         * Nothing to check, the method does not return anything and does not change any visible state.
         */
    }

    @Test
    public void registerMappingEntityIntent() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredIntentDefinition = intentProviderTestBot.getMappingEntityIntent();
        intentRecognitionProvider.registerIntentDefinition(registeredIntentDefinition);
        /*
         * Nothing to check, the method does not return anything and does not change any visible state.
         */
    }

    @Test
    public void registerCompositeEntityIntent() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredIntentDefinition = intentProviderTestBot.getCompositeEntityIntent();
        intentRecognitionProvider.registerIntentDefinition(registeredIntentDefinition);
    }

    @Test(expected = NullPointerException.class)
    public void deleteNullIntent() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        intentRecognitionProvider.deleteIntentDefinition(null);
    }

    @Test
    public void deleteExistingIntent() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredIntentDefinition = intentProviderTestBot.getSimpleIntent();
        intentRecognitionProvider.registerIntentDefinition(registeredIntentDefinition);
        intentRecognitionProvider.deleteIntentDefinition(registeredIntentDefinition);
        /*
         * Reset to null, it has been deleted.
         */
        registeredIntentDefinition = null;
    }

    @Test(expected = NullPointerException.class)
    public void registerNullEntity() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        intentRecognitionProvider.registerEntityDefinition(null);
    }

    @Test
    public void registerMappingEntity() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredEntityDefinitions.add(intentProviderTestBot.getMappingEntity());
        intentRecognitionProvider.registerEntityDefinition(intentProviderTestBot.getMappingEntity());
        /*
         * Nothing to check, the method does not return anything and does not change any visible state.
         */
    }

    @Test
    public void registerCompositeEntityReferencedEntitiesAlreadyRegistered() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredEntityDefinitions.add(intentProviderTestBot.getMappingEntity());
        intentRecognitionProvider.registerEntityDefinition(intentProviderTestBot.getMappingEntity());
        registeredEntityDefinitions.add(intentProviderTestBot.getCompositeEntity());
        intentRecognitionProvider.registerEntityDefinition(intentProviderTestBot.getCompositeEntity());
        /*
         * Nothing to check, the method does not return anything and does not change any visible state.
         */
    }

    @Test
    public void registerCompositeEntityReferencedEntitiesNotRegistered() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredEntityDefinitions.add(intentProviderTestBot.getCompositeEntity());
        /*
         * Add the mapping entity, it should be registered with the composite.
         */
        registeredEntityDefinitions.add(intentProviderTestBot.getMappingEntity());
        intentRecognitionProvider.registerEntityDefinition(intentProviderTestBot.getCompositeEntity());
        /*
         * Nothing to check, the method does not return anything and does not change any visible state.
         */
    }

    @Test(expected = NullPointerException.class)
    public void deleteNullEntity() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        intentRecognitionProvider.registerEntityDefinition(null);
    }

    @Test
    public void deleteEntityNotReferenced() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredEntityDefinitions.add(intentProviderTestBot.getMappingEntity());
        intentRecognitionProvider.registerEntityDefinition(intentProviderTestBot.getMappingEntity());
        intentRecognitionProvider.deleteEntityDefinition(intentProviderTestBot.getMappingEntity());
        /*
         * Clean the registered entities list if the entities has been successfully deleted.
         */
        registeredEntityDefinitions.clear();
    }

    @Test(expected = NullPointerException.class)
    public void createContextNullContextId() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        intentRecognitionProvider.createContext(null);
    }

    @Test
    public void createContextValidContextId() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        StateContext context = intentRecognitionProvider.createContext("TEST");
        assertThat(context).isNotNull();
    }

    @Test(expected = NullPointerException.class)
    public void getIntentNullInput() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        intentRecognitionProvider.getIntent(null, intentRecognitionProvider.createContext("TEST"));
    }

    @Test(expected = NullPointerException.class)
    public void getIntentNullContext() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        intentRecognitionProvider.getIntent("Intent", null);
    }

    @Test
    public void getIntentNotRegistered() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        StateContext context = intentRecognitionProvider.createContext("TEST");
        context.setState(intentProviderTestBot.getModel().getInitState());
        RecognizedIntent recognizedIntent = intentRecognitionProvider.getIntent("Error", context);
        assertThatRecognizedIntentHasDefinition(recognizedIntent,
                IntentRecognitionProvider.DEFAULT_FALLBACK_INTENT.getName());
    }

    @Test
    public void getSimpleIntent() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredIntentDefinition = intentProviderTestBot.getSimpleIntent();
        intentRecognitionProvider.registerIntentDefinition(registeredIntentDefinition);
        intentRecognitionProvider.trainMLEngine();
        StateContext context = intentRecognitionProvider.createContext("TEST");
        context.setState(intentProviderTestBot.getModel().getInitState());
        RecognizedIntent recognizedIntent = intentRecognitionProvider.getIntent("Greetings", context);
        assertThatRecognizedIntentHasDefinition(recognizedIntent, registeredIntentDefinition.getName());
    }

    @Test
    public void getSystemEntityIntent() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredIntentDefinition = intentProviderTestBot.getSystemEntityIntent();
        intentRecognitionProvider.registerIntentDefinition(registeredIntentDefinition);
        intentRecognitionProvider.trainMLEngine();
        StateContext context = intentRecognitionProvider.createContext("TEST");
        context.setState(intentProviderTestBot.getModel().getInitState());
        RecognizedIntent recognizedIntent = intentRecognitionProvider.getIntent("Hello Test", context);
        assertThatRecognizedIntentHasDefinition(recognizedIntent, registeredIntentDefinition.getName());
        assertThatIntentContainsParameterWithValue(recognizedIntent, "helloTo", "Test");
    }

    @Test
    public void getMappingEntityIntent() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredEntityDefinitions.add(intentProviderTestBot.getMappingEntity());
        intentRecognitionProvider.registerEntityDefinition(intentProviderTestBot.getMappingEntity());
        registeredIntentDefinition = intentProviderTestBot.getMappingEntityIntent();
        intentRecognitionProvider.registerIntentDefinition(registeredIntentDefinition);
        intentRecognitionProvider.trainMLEngine();
        StateContext context = intentRecognitionProvider.createContext("TEST");
        context.setState(intentProviderTestBot.getModel().getInitState());
        RecognizedIntent recognizedIntent = intentRecognitionProvider.getIntent("Give me some information about " +
                "Gwendal", context);
        assertThatRecognizedIntentHasDefinition(recognizedIntent, registeredIntentDefinition.getName());
        assertThatIntentContainsParameterWithValue(recognizedIntent, "name", "Gwendal");
    }

    @Test
    public void getCompositeEntityIntent() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredEntityDefinitions.add(intentProviderTestBot.getMappingEntity());
        intentRecognitionProvider.registerEntityDefinition(intentProviderTestBot.getMappingEntity());
        registeredEntityDefinitions.add(intentProviderTestBot.getCompositeEntity());
        intentRecognitionProvider.registerEntityDefinition(intentProviderTestBot.getCompositeEntity());
        registeredIntentDefinition = intentProviderTestBot.getCompositeEntityIntent();
        intentRecognitionProvider.registerIntentDefinition(registeredIntentDefinition);
        intentRecognitionProvider.trainMLEngine();
        StateContext context = intentRecognitionProvider.createContext("TEST");
        context.setState(intentProviderTestBot.getModel().getInitState());
        RecognizedIntent recognizedIntent = intentRecognitionProvider.getIntent("Does Jordi knows Barcelona?", context);
        assertThatRecognizedIntentHasDefinition(recognizedIntent, registeredIntentDefinition.getName());
        assertThatIntentContainsParameter(recognizedIntent, "founderCity");
        Object parameterValue = recognizedIntent.getValues().stream()
                .filter(p -> p.getContextParameter().getName().equals("founderCity"))
                .map(ContextParameterValue::getValue).findAny().get();
        assertThat(parameterValue).isInstanceOf(Map.class);
        Map<String, String> mapParameterValue = (Map<String, String>) parameterValue;
        assertThat(mapParameterValue).contains(new AbstractMap.SimpleEntry<>("city", "Barcelona"));
        assertThat(mapParameterValue).contains(new AbstractMap.SimpleEntry<>("XatkitFounder", "Jordi"));
    }

    @Test
    public void shutdown() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        intentRecognitionProvider.shutdown();
        assertThat(intentRecognitionProvider.isShutdown()).isTrue();
    }

    protected abstract T getIntentRecognitionProvider();

    protected void assertThatRecognizedIntentHasDefinition(RecognizedIntent recognizedIntent, String definitionName) {
        assertThat(recognizedIntent).isNotNull();
        assertThat(recognizedIntent.getDefinition()).isNotNull();
        assertThat(recognizedIntent.getDefinition().getName()).isEqualTo(definitionName);
    }

    protected void assertThatIntentContainsParameter(RecognizedIntent intent, String parameterName) {
        assertThat(intent.getValues()).anyMatch(v -> v.getContextParameter().getName().equals(parameterName));
    }

    protected void assertThatIntentContainsParameterWithValue(RecognizedIntent intent, String parameterName,
                                                              Object value) {
        assertThatIntentContainsParameter(intent, parameterName);
        /*
         * Separate the two checks to have a better log error.
         */
        assertThat(intent.getValues()).anyMatch(v -> v.getContextParameter().getName().equals(parameterName) && v.getValue().equals(value));
    }
}
