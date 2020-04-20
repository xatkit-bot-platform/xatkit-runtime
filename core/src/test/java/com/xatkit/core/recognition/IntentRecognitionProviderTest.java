package com.xatkit.core.recognition;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.EventDefinitionRegistry;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.ContextInstance;
import com.xatkit.intent.ContextParameterValue;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.test.util.TestBotExecutionModel;
import com.xatkit.test.util.TestModelLoader;
import org.apache.commons.configuration2.ex.ConfigurationException;
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

    protected static TestBotExecutionModel testBotExecutionModel;

    @BeforeClass
    public static void setUpBeforeClass() throws ConfigurationException {
        testBotExecutionModel = TestModelLoader.loadTestBot();
    }

    protected T intentRecognitionProvider;

    protected EventDefinitionRegistry eventRegistry;

    protected IntentDefinition registeredIntentDefinition;

    protected List<EntityDefinition> registeredEntityDefinitions = new ArrayList<>();

    @Before
    public void setUp() {
        eventRegistry = new EventDefinitionRegistry();
        eventRegistry.registerEventDefinition(testBotExecutionModel.getSimpleIntent());
        eventRegistry.registerEventDefinition(testBotExecutionModel.getSystemEntityIntent());
        eventRegistry.registerEventDefinition(testBotExecutionModel.getMappingEntityIntent());
        eventRegistry.registerEventDefinition(testBotExecutionModel.getCompositeEntityIntent());
    }

    @After
    public void tearDown() {
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
    public void registerNullIntent() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        intentRecognitionProvider.registerIntentDefinition(null);
    }

    @Test
    public void registerSimpleIntent() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredIntentDefinition = testBotExecutionModel.getSimpleIntent();
        intentRecognitionProvider.registerIntentDefinition(registeredIntentDefinition);
        /*
         * Nothing to check, the method does not return anything and does not change any visible state.
         */
    }

    @Test
    public void registerSystemEntityIntent() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredIntentDefinition = testBotExecutionModel.getSystemEntityIntent();
        intentRecognitionProvider.registerIntentDefinition(registeredIntentDefinition);
        /*
         * Nothing to check, the method does not return anything and does not change any visible state.
         */
    }

    @Test
    public void registerMappingEntityIntent() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredIntentDefinition = testBotExecutionModel.getMappingEntityIntent();
        intentRecognitionProvider.registerIntentDefinition(registeredIntentDefinition);
        /*
         * Nothing to check, the method does not return anything and does not change any visible state.
         */
    }

    @Test
    public void registerCompositeEntityIntent() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredIntentDefinition = testBotExecutionModel.getCompositeEntityIntent();
        intentRecognitionProvider.registerIntentDefinition(registeredIntentDefinition);
    }

    @Test(expected = NullPointerException.class)
    public void deleteNullIntent() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        intentRecognitionProvider.deleteIntentDefinition(null);
    }

    @Test
    public void deleteExistingIntent() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredIntentDefinition = testBotExecutionModel.getSimpleIntent();
        intentRecognitionProvider.registerIntentDefinition(registeredIntentDefinition);
        intentRecognitionProvider.deleteIntentDefinition(registeredIntentDefinition);
        /*
         * Reset to null, it has been deleted.
         */
        registeredIntentDefinition = null;
    }

    @Test(expected = NullPointerException.class)
    public void registerNullEntity() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        intentRecognitionProvider.registerEntityDefinition(null);
    }

    @Test
    public void registerMappingEntity() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredEntityDefinitions.add(testBotExecutionModel.getMappingEntity());
        intentRecognitionProvider.registerEntityDefinition(testBotExecutionModel.getMappingEntity());
        /*
         * Nothing to check, the method does not return anything and does not change any visible state.
         */
    }

    @Test
    public void registerCompositeEntityReferencedEntitiesAlreadyRegistered() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredEntityDefinitions.add(testBotExecutionModel.getMappingEntity());
        intentRecognitionProvider.registerEntityDefinition(testBotExecutionModel.getMappingEntity());
        registeredEntityDefinitions.add(testBotExecutionModel.getCompositeEntity());
        intentRecognitionProvider.registerEntityDefinition(testBotExecutionModel.getCompositeEntity());
        /*
         * Nothing to check, the method does not return anything and does not change any visible state.
         */
    }

    @Test
    public void registerCompositeEntityReferencedEntitiesNotRegistered() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredEntityDefinitions.add(testBotExecutionModel.getCompositeEntity());
        /*
         * Add the mapping entity, it should be registered with the composite.
         */
        registeredEntityDefinitions.add(testBotExecutionModel.getMappingEntity());
        intentRecognitionProvider.registerEntityDefinition(testBotExecutionModel.getCompositeEntity());
        /*
         * Nothing to check, the method does not return anything and does not change any visible state.
         */
    }

    @Test(expected = NullPointerException.class)
    public void deleteNullEntity() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        intentRecognitionProvider.registerEntityDefinition(null);
    }

    @Test
    public void deleteEntityNotReferenced() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredEntityDefinitions.add(testBotExecutionModel.getMappingEntity());
        intentRecognitionProvider.registerEntityDefinition(testBotExecutionModel.getMappingEntity());
        intentRecognitionProvider.deleteEntityDefinition(testBotExecutionModel.getMappingEntity());
        /*
         * Clean the registered entities list if the entities has been successfully deleted.
         */
        registeredEntityDefinitions.clear();
    }

    @Test(expected = NullPointerException.class)
    public void createSessionNullSessionId() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        intentRecognitionProvider.createSession(null);
    }

    @Test
    public void createSessionValidSessionId() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        XatkitSession session = intentRecognitionProvider.createSession("TEST");
        assertThat(session).isNotNull();
    }

    @Test(expected = NullPointerException.class)
    public void getIntentNullInput() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        intentRecognitionProvider.getIntent(null, intentRecognitionProvider.createSession("TEST"));
    }

    @Test(expected = NullPointerException.class)
    public void getIntentNullSession() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        intentRecognitionProvider.getIntent("Intent", null);
    }

    @Test
    public void getIntentNotRegistered() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        RecognizedIntent recognizedIntent = intentRecognitionProvider.getIntent("Error",
                intentRecognitionProvider.createSession("TEST"));
        assertThatRecognizedIntentHasDefinition(recognizedIntent,
                IntentRecognitionProvider.DEFAULT_FALLBACK_INTENT.getName());
    }

    @Test
    public void getSimpleIntent() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredIntentDefinition = testBotExecutionModel.getSimpleIntent();
        intentRecognitionProvider.registerIntentDefinition(registeredIntentDefinition);
        intentRecognitionProvider.trainMLEngine();
        RecognizedIntent recognizedIntent = intentRecognitionProvider.getIntent("Greetings",
                intentRecognitionProvider.createSession("TEST"));
        assertThatRecognizedIntentHasDefinition(recognizedIntent, registeredIntentDefinition.getName());
    }

    @Test
    public void getSystemEntityIntent() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredIntentDefinition = testBotExecutionModel.getSystemEntityIntent();
        intentRecognitionProvider.registerIntentDefinition(registeredIntentDefinition);
        intentRecognitionProvider.trainMLEngine();
        RecognizedIntent recognizedIntent = intentRecognitionProvider.getIntent("Hello Test",
                intentRecognitionProvider.createSession("TEST"));
        assertThatRecognizedIntentHasDefinition(recognizedIntent, registeredIntentDefinition.getName());
        ContextInstance context = recognizedIntent.getOutContextInstance("Hello");
        assertThat(context).isNotNull();
        assertThatContextContainsParameterWithValue(context, "helloTo", "Test");
    }

    @Test
    public void getMappingEntityIntent() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredEntityDefinitions.add(testBotExecutionModel.getMappingEntity());
        intentRecognitionProvider.registerEntityDefinition(testBotExecutionModel.getMappingEntity());
        registeredIntentDefinition = testBotExecutionModel.getMappingEntityIntent();
        intentRecognitionProvider.registerIntentDefinition(registeredIntentDefinition);
        intentRecognitionProvider.trainMLEngine();
        RecognizedIntent recognizedIntent = intentRecognitionProvider.getIntent("Give me some information about " +
                "Gwendal", intentRecognitionProvider.createSession("TEST"));
        assertThatRecognizedIntentHasDefinition(recognizedIntent, registeredIntentDefinition.getName());
        ContextInstance context = recognizedIntent.getOutContextInstance("Founder");
        assertThat(context).isNotNull();
        assertThatContextContainsParameterWithValue(context, "name", "Gwendal");
    }

    @Test
    public void getCompositeEntityIntent() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredEntityDefinitions.add(testBotExecutionModel.getMappingEntity());
        intentRecognitionProvider.registerEntityDefinition(testBotExecutionModel.getMappingEntity());
        registeredEntityDefinitions.add(testBotExecutionModel.getCompositeEntity());
        intentRecognitionProvider.registerEntityDefinition(testBotExecutionModel.getCompositeEntity());
        registeredIntentDefinition = testBotExecutionModel.getCompositeEntityIntent();
        intentRecognitionProvider.registerIntentDefinition(registeredIntentDefinition);
        intentRecognitionProvider.trainMLEngine();
        RecognizedIntent recognizedIntent = intentRecognitionProvider.getIntent("Does Jordi knows Barcelona?",
                intentRecognitionProvider.createSession("TEST"));
        assertThatRecognizedIntentHasDefinition(recognizedIntent, registeredIntentDefinition.getName());
        ContextInstance context = recognizedIntent.getOutContextInstance("Query");
        assertThat(context).isNotNull();
        assertThatContextContainsParameterValue(context, "founderCity");
        Object parameterValue = context.getValues().stream()
                .filter(p -> p.getContextParameter().getName().equals("founderCity"))
                .map(ContextParameterValue::getValue).findAny().get();
        assertThat(parameterValue).isInstanceOf(Map.class);
        Map<String, String> mapParameterValue = (Map<String, String>) parameterValue;
        assertThat(mapParameterValue).contains(new AbstractMap.SimpleEntry<>("city", "Barcelona"));
        assertThat(mapParameterValue).contains(new AbstractMap.SimpleEntry<>("XatkitFounder", "Jordi"));
    }

    @Test
    public void shutdown() {
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

    protected void assertThatContextContainsParameterValue(ContextInstance contextInstance, String parameterName) {
        assertThat(contextInstance.getValues()).isNotEmpty();
        assertThat(contextInstance.getValues()).anyMatch(p -> p.getContextParameter().getName().equals(parameterName));
    }

    protected void assertThatContextContainsParameterWithValue(ContextInstance contextInstance, String parameterName,
                                                               Object value) {
        assertThatContextContainsParameterValue(contextInstance, parameterName);
        /*
         * Separate the two checks to have a better log error.
         */
        assertThat(contextInstance.getValues()).anyMatch(p -> p.getContextParameter().getName().equals(parameterName) && p.getValue().equals(value));
    }
}
