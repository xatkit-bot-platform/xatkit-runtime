package fr.zelus.jarvis.recognition.dialogflow;

import com.google.cloud.dialogflow.v2.Intent;
import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.AbstractJarvisTest;
import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.intent.*;
import fr.zelus.jarvis.module.Action;
import fr.zelus.jarvis.module.InputProviderDefinition;
import fr.zelus.jarvis.module.Module;
import fr.zelus.jarvis.module.ModuleFactory;
import fr.zelus.jarvis.orchestration.ActionInstance;
import fr.zelus.jarvis.orchestration.OrchestrationFactory;
import fr.zelus.jarvis.orchestration.OrchestrationLink;
import fr.zelus.jarvis.orchestration.OrchestrationModel;
import fr.zelus.jarvis.test.util.VariableLoaderHelper;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.*;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class DialogFlowApiTest extends AbstractJarvisTest {

    protected static String VALID_PROJECT_ID = VariableLoaderHelper.getJarvisDialogFlowProject();

    protected static String VALID_LANGUAGE_CODE = "en-US";

    protected static String SAMPLE_INPUT = "hello";

    protected static EntityDefinition VALID_ENTITY_DEFINITION;

    /*
     * EntityDefinition is contained in the context parameter, we need to create a second testing instance.
     */
    protected static EntityDefinition VALID_ENTITY_DEFINITION_2;

    protected DialogFlowApi api;

    /**
     * Stores the last {@link IntentDefinition} registered by
     * {@link DialogFlowApi#registerIntentDefinition(IntentDefinition)}.
     * <p>
     * <b>Note:</b> this variable must be set by each test case calling
     * {@link DialogFlowApi#registerIntentDefinition(IntentDefinition)}, to enable their deletion in the
     * {@link #tearDown()} method. Not setting this variable would add test-related intents in the DialogFlow project.
     *
     * @see #tearDown()
     */
    private IntentDefinition registeredIntentDefinition;

    // not tested here, only instantiated to enable IntentDefinition registration and Module retrieval
    protected static JarvisCore jarvisCore;

    private static Configuration buildConfiguration(String projectId, String languageCode) {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(DialogFlowApi.PROJECT_ID_KEY, projectId);
        configuration.addProperty(DialogFlowApi.LANGUAGE_CODE_KEY, languageCode);
        /*
         * Disable Intent loading to avoid RESOURCE_EXHAUSTED exceptions from the DialogFlow API.
         */
        configuration.addProperty(DialogFlowApi.ENABLE_INTENT_LOADING_KEY, false);
        return configuration;
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Module stubModule = ModuleFactory.eINSTANCE.createModule();
        stubModule.setName("StubJarvisModule");
        stubModule.setJarvisModulePath("fr.zelus.jarvis.stubs.StubJarvisModule");
        Action stubAction = ModuleFactory.eINSTANCE.createAction();
        stubAction.setName("StubJarvisAction");
        // No parameters, keep it simple
        stubModule.getActions().add(stubAction);
        InputProviderDefinition stubInputProvider = ModuleFactory.eINSTANCE.createInputProviderDefinition();
        stubInputProvider.setName("StubInputProvider");
        stubModule.getEventProviderDefinitions().add(stubInputProvider);
        IntentDefinition stubIntentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        stubIntentDefinition.setName("Default Welcome Intent");
        // No parameters, keep it simple
        stubModule.getIntentDefinitions().add(stubIntentDefinition);
        OrchestrationModel orchestrationModel = OrchestrationFactory.eINSTANCE.createOrchestrationModel();
        OrchestrationLink link = OrchestrationFactory.eINSTANCE.createOrchestrationLink();
        link.setEvent(stubIntentDefinition);
        ActionInstance actionInstance = OrchestrationFactory.eINSTANCE.createActionInstance();
        actionInstance.setAction(stubAction);
        link.getActions().add(actionInstance);
        orchestrationModel.getOrchestrationLinks().add(link);
        Configuration configuration = buildConfiguration(VALID_PROJECT_ID, VALID_LANGUAGE_CODE);
        configuration.addProperty(JarvisCore.ORCHESTRATION_MODEL_KEY, orchestrationModel);
        jarvisCore = new JarvisCore(configuration);
        VALID_ENTITY_DEFINITION = IntentFactory.eINSTANCE.createBaseEntityDefinition();
        ((BaseEntityDefinition) VALID_ENTITY_DEFINITION).setEntityType(EntityType.ANY);
        VALID_ENTITY_DEFINITION_2 = IntentFactory.eINSTANCE.createBaseEntityDefinition();
        ((BaseEntityDefinition) VALID_ENTITY_DEFINITION_2).setEntityType(EntityType.ANY);
    }

    @After
    public void tearDown() {
        if (nonNull(registeredIntentDefinition)) {
            api.deleteIntentDefinition(registeredIntentDefinition);
            jarvisCore.getEventDefinitionRegistry().unregisterEventDefinition(registeredIntentDefinition);
        }
        /*
         * Reset the variable value to null to avoid unnecessary deletion calls.
         */
        registeredIntentDefinition = null;
        if (nonNull(api)) {
            try {
                api.shutdown();
            } catch (DialogFlowException e) {
                /*
                 * Already shutdown, ignore
                 */
            }
        }
    }

    @AfterClass
    public static void tearDownAfterClass() {
        jarvisCore.shutdown();
    }

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private DialogFlowApi getValidDialogFlowApi() {
        api = new DialogFlowApi(jarvisCore, buildConfiguration(VALID_PROJECT_ID, VALID_LANGUAGE_CODE));
        return api;
    }

    @Test(expected = NullPointerException.class)
    public void constructNullJarvisCore() {
        api = new DialogFlowApi(null, buildConfiguration(VALID_PROJECT_ID, VALID_LANGUAGE_CODE));
    }

    @Test(expected = NullPointerException.class)
    public void constructNullProjectIdValidLanguageCode() {
        api = new DialogFlowApi(null, buildConfiguration(null, "en-US"));
    }

    @Test
    public void constructNullLanguageCode() {
        api = new DialogFlowApi(jarvisCore, buildConfiguration(VALID_PROJECT_ID, null));
        assertThat(api.getLanguageCode()).as("Default language code").isEqualTo(DialogFlowApi.DEFAULT_LANGUAGE_CODE);
    }

    @Test
    public void constructValid() {
        api = new DialogFlowApi(jarvisCore, buildConfiguration(VALID_PROJECT_ID, VALID_LANGUAGE_CODE));
        softly.assertThat(VALID_PROJECT_ID).as("Valid project ID").isEqualTo(api.getProjectId());
        softly.assertThat(VALID_LANGUAGE_CODE).as("Valid language code").isEqualTo(api.getLanguageCode());
        softly.assertThat(api.isShutdown()).as("Not shutdown").isFalse();
    }

    @Test
    public void constructDefaultLanguageCode() {
        api = new DialogFlowApi(jarvisCore, buildConfiguration(VALID_PROJECT_ID, null));
        softly.assertThat(VALID_PROJECT_ID).as("Valid project ID").isEqualTo(api.getProjectId());
        softly.assertThat(VALID_LANGUAGE_CODE).as("Valid language code").isEqualTo(api.getLanguageCode());
    }

    @Test
    public void constructCredentialsFilePath() {
        Configuration configuration = buildConfiguration(VariableLoaderHelper.getJarvisTestDialogFlowProject(), null);
        String credentialsFilePath = this.getClass().getClassLoader().getResource("jarvis-test2-secret.json").getFile();
        configuration.addProperty(DialogFlowApi.GOOGLE_CREDENTIALS_PATH_KEY, credentialsFilePath);
        api = new DialogFlowApi(jarvisCore, configuration);
        /*
         * Ensures that the underlying IntentsClient credentials are valid by listing the Agent intents.
         */
        Log.info("Listing DialogFlow intents to check permissions");
        for (Intent registeredIntent : api.getRegisteredIntents()) {
            Log.info("Found intent {0}", registeredIntent.getDisplayName());
        }
        /*
         * Ensures that the underlying AgentsClient credentials are valid by training the Agent.
         */
        api.trainMLEngine();
    }

    @Test(expected = NullPointerException.class)
    public void registerIntentDefinitionNullIntentDefinition() {
        api = getValidDialogFlowApi();
        api.registerIntentDefinition(null);
    }

    @Test(expected = NullPointerException.class)
    public void registerIntentDefinitionNullName() {
        api = getValidDialogFlowApi();
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        api.registerIntentDefinition(intentDefinition);
    }

    @Test
    public void registerIntentDefinitionValidIntentDefinition() {
        api = getValidDialogFlowApi();
        registeredIntentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        String intentName = "TestRegister";
        String trainingPhrase = "test";
        registeredIntentDefinition.setName(intentName);
        registeredIntentDefinition.getTrainingSentences().add(trainingPhrase);
        api.registerIntentDefinition(registeredIntentDefinition);
        List<Intent> registeredIntents = api.getRegisteredIntentsFullView();
        assertThat(registeredIntents).as("Registered Intent list is not null").isNotNull();
        softly.assertThat(registeredIntents).as("Registered Intent list is not empty").isNotEmpty();
        Intent foundIntent = null;
        for (Intent intent : registeredIntents) {
            if (intent.getDisplayName().equals(intentName)) {
                foundIntent = intent;
            }
        }
        assertThat(foundIntent).as("Registered Intent list contains the registered IntentDefinition")
                .isNotNull();
        softly.assertThat(foundIntent.getTrainingPhrasesList()).as("Intent's training phrase list is not empty")
                .isNotEmpty();
        boolean foundTrainingPhrase = false;
        for (Intent.TrainingPhrase intentTrainingPhrase : foundIntent.getTrainingPhrasesList()) {
            for (Intent.TrainingPhrase.Part part : intentTrainingPhrase.getPartsList()) {
                if (part.getText().equals(trainingPhrase)) {
                    foundTrainingPhrase = true;
                }
            }
        }
        softly.assertThat(foundTrainingPhrase).as("The IntentDefinition's training phrase is in the retrieved " +
                "Intent").isTrue();
    }

    @Test(expected = DialogFlowException.class)
    public void registerIntentDefinitionAlreadyRegistered() {
        api = getValidDialogFlowApi();
        registeredIntentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        String intentName = "TestAlreadyDefined";
        registeredIntentDefinition.setName(intentName);
        registeredIntentDefinition.getTrainingSentences().add("test");
        registeredIntentDefinition.getTrainingSentences().add("test jarvis");
        api.registerIntentDefinition(registeredIntentDefinition);
        api.registerIntentDefinition(registeredIntentDefinition);
    }

    @Test(expected = NullPointerException.class)
    public void deleteIntentDefinitionNullIntentDefinition() {
        api = getValidDialogFlowApi();
        api.deleteIntentDefinition(null);
    }

    @Test(expected = NullPointerException.class)
    public void deleteIntentDefinitionNullName() {
        api = getValidDialogFlowApi();
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        api.deleteIntentDefinition(intentDefinition);
    }

    @Test
    public void deleteIntentDefinitionNotRegisteredIntent() {
        api = getValidDialogFlowApi();
        String intentName = "TestDeleteNotRegistered";
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        intentDefinition.setName(intentName);
        int registeredIntentsCount = api.getRegisteredIntents().size();
        api.deleteIntentDefinition(intentDefinition);
        assertThat(api.getRegisteredIntents()).as("Registered Intents list has not changed").hasSize
                (registeredIntentsCount);
    }

    @Test
    public void deleteIntentDefinitionRegisteredIntentDefinition() {
        api = getValidDialogFlowApi();
        String intentName = "TestDeleteRegistered";
        registeredIntentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        registeredIntentDefinition.setName(intentName);
        api.registerIntentDefinition(registeredIntentDefinition);
        api.deleteIntentDefinition(registeredIntentDefinition);
        Intent foundIntent = null;
        for (Intent intent : api.getRegisteredIntents()) {
            if (intent.getDisplayName().equals(intentName)) {
                foundIntent = intent;
            }
        }
        assertThat(foundIntent).as("The Intent has been removed from the registered Intents").isNull();

    }

    @Test
    public void createSessionValidApi() {
        api = getValidDialogFlowApi();
        JarvisSession session = api.createSession("sessionID");
        checkDialogFlowSession(session, VALID_PROJECT_ID, "sessionID");
    }

    @Test
    public void createSessionWithContextProperty() {
        Configuration configuration = buildConfiguration(VALID_PROJECT_ID, VALID_LANGUAGE_CODE);
        configuration.addProperty(JarvisContext.VARIABLE_TIMEOUT_KEY, 10);
        api = new DialogFlowApi(jarvisCore, configuration);
        JarvisSession session = api.createSession("sessionID");
        checkDialogFlowSession(session, VALID_PROJECT_ID, "sessionID");
        softly.assertThat(session.getJarvisContext().getVariableTimeout()).as("Valid JarvisContext variable timeout")
                .isEqualTo(10);
    }

    @Test(expected = DialogFlowException.class)
    public void shutdownAlreadyShutdown() {
        api = getValidDialogFlowApi();
        api.shutdown();
        api.shutdown();
    }

    @Test
    public void shutdown() {
        api = getValidDialogFlowApi();
        JarvisSession session = api.createSession("sessionID");
        api.shutdown();
        softly.assertThat(api.isShutdown()).as("DialogFlow API is shutdown").isTrue();
        assertThatExceptionOfType(DialogFlowException.class).isThrownBy(() -> api.getIntent("test", session))
                .withMessage("Cannot extract an Intent from the provided input, the DialogFlow API is shutdown");
        assertThatExceptionOfType(DialogFlowException.class).isThrownBy(() -> api.createSession("sessionID"))
                .withMessage
                        ("Cannot create a new Session, the DialogFlow API is shutdown");
    }

    @Test
    public void getIntentValidSession() {
        api = getValidDialogFlowApi();
        JarvisSession session = api.createSession("sessionID");
        RecognizedIntent intent = api.getIntent(SAMPLE_INPUT, session);
        IntentDefinition intentDefinition = (IntentDefinition) intent.getDefinition();
        assertThat(intent).as("Null Intent").isNotNull();
        assertThat(intentDefinition).as("Null Intent Definition").isNotNull();
        assertThat(intentDefinition.getName()).as("Valid Intent").isEqualTo("Default Welcome Intent");
    }

    @Test(expected = DialogFlowException.class)
    public void getIntentInvalidSession() {
        api = new DialogFlowApi(jarvisCore, buildConfiguration("test", null));
        JarvisSession session = api.createSession("sessionID");
        RecognizedIntent intent = api.getIntent(SAMPLE_INPUT, session);
    }

    @Test(expected = NullPointerException.class)
    public void getIntentNullSession() {
        api = getValidDialogFlowApi();
        RecognizedIntent intent = api.getIntent(SAMPLE_INPUT, null);
    }

    @Test(expected = NullPointerException.class)
    public void getIntentNullText() {
        api = getValidDialogFlowApi();
        JarvisSession session = api.createSession("sessionID");
        RecognizedIntent intent = api.getIntent(null, session);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getIntentEmptyText() {
        api = getValidDialogFlowApi();
        JarvisSession session = api.createSession("sessionID");
        RecognizedIntent intent = api.getIntent("", session);
    }

    @Test
    public void getIntentUnknownText() {
        api = getValidDialogFlowApi();
        JarvisSession session = api.createSession("sessionID");
        RecognizedIntent intent = api.getIntent("azerty", session);
        assertThat(intent.getDefinition()).as("IntentDefinition is not null").isNotNull();
        assertThat(intent.getDefinition().getName()).as("IntentDefinition is the Default Fallback Intent").isEqualTo
                ("Default Fallback Intent");
    }

    @Test
    public void getIntentMultipleOutputContextNoParameters() {
        String trainingSentence = "I love the monkey head";
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        intentDefinition.setName("TestMonkeyHead");
        intentDefinition.getTrainingSentences().add(trainingSentence);
        Context outContext1 = IntentFactory.eINSTANCE.createContext();
        outContext1.setName("Context1");
        Context outContext2 = IntentFactory.eINSTANCE.createContext();
        outContext2.setName("Context2");
        intentDefinition.getOutContexts().add(outContext1);
        intentDefinition.getOutContexts().add(outContext2);
        api = getValidDialogFlowApi();
        try {
            api.registerIntentDefinition(intentDefinition);
        } catch (DialogFlowException e) {
            /*
             * The intent is already registered
             */
            Log.warn("The intent {0} is already registered", intentDefinition.getName());
        }
        api.trainMLEngine();
        jarvisCore.getEventDefinitionRegistry().registerEventDefinition(intentDefinition);
        /*
         * Setting this variable will delete the intent after execution. This should be done to ensure that the
         * intents are well created, but it causes intent propagation issues on the DialogFlow side (see
         * https://productforums.google.com/forum/m/#!category-topic/dialogflow/type-troubleshooting/UDokzc7mOcY)
         */
//        registeredIntentDefinition = intentDefinition;
        JarvisSession session = api.createSession(UUID.randomUUID().toString());
        RecognizedIntent recognizedIntent = api.getIntent(trainingSentence, session);
        assertThat(recognizedIntent).as("Not null recognized intent").isNotNull();
        assertThat(recognizedIntent.getDefinition()).as("Not null definition").isNotNull();
        softly.assertThat(recognizedIntent.getDefinition().getName()).as("Valid IntentDefinition").isEqualTo
                (intentDefinition.getName());
        /*
         * The ContextInstances are set, but they should not contain any value.
         */
        assertThat(recognizedIntent.getOutContextInstances()).as("Empty out context instances").hasSize(2);
        assertThat(recognizedIntent.getOutContextInstance("Context1")).as("RecognizedIntent contains Context1")
                .isNotNull();
        assertThat(recognizedIntent.getOutContextInstance("Context1").getValues()).as("ContextInstance 1 does not " +
                "contain any value").isEmpty();;
        assertThat(recognizedIntent.getOutContextInstance("Context2")).as("RecognizedIntent contains Context2")
                .isNotNull();
        assertThat(recognizedIntent.getOutContextInstance("Context2").getValues()).as("ContextInstance 2 does not " +
                "contain any value").isEmpty();
    }

    @Test
    public void getIntentMultipleOutputContextParameters() {
        /*
         * Change the training sentence to avoid deleted intent definition matching (deleted intents can take some
         * time to be completely removed from the DialogFlow Agent, see https://discuss.api
         * .ai/t/intent-mismatch-issue/12042/17)
         */
        String trainingSentence = "cheese steak jimmy's";
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        intentDefinition.setName("TestCheeseSteakJimmys");
        intentDefinition.getTrainingSentences().add(trainingSentence);
        Context outContext1 = IntentFactory.eINSTANCE.createContext();
        outContext1.setName("Context1");
        ContextParameter contextParameter1 = IntentFactory.eINSTANCE.createContextParameter();
        contextParameter1.setName("Parameter1");
        contextParameter1.setEntity(VALID_ENTITY_DEFINITION);
        contextParameter1.setTextFragment("cheese");
        outContext1.getParameters().add(contextParameter1);
        Context outContext2 = IntentFactory.eINSTANCE.createContext();
        outContext2.setName("Context2");
        ContextParameter contextParameter2 = IntentFactory.eINSTANCE.createContextParameter();
        contextParameter2.setName("Parameter2");
        contextParameter2.setEntity(VALID_ENTITY_DEFINITION_2);
        contextParameter2.setTextFragment("steak");
        outContext2.getParameters().add(contextParameter2);
        intentDefinition.getOutContexts().add(outContext1);
        intentDefinition.getOutContexts().add(outContext2);
        api = getValidDialogFlowApi();
        try {
            api.registerIntentDefinition(intentDefinition);
        } catch (DialogFlowException e) {
            Log.warn("The intent {0} is already registered", intentDefinition.getName());
        }
        api.trainMLEngine();
        jarvisCore.getEventDefinitionRegistry().registerEventDefinition(intentDefinition);
        /*
         * Setting this variable will delete the intent after execution. This should be done to ensure that the
         * intents are well created, but it causes intent propagation issues on the DialogFlow side (see
         * https://productforums.google.com/forum/m/#!category-topic/dialogflow/type-troubleshooting/UDokzc7mOcY)
         */
//        registeredIntentDefinition = intentDefinition;
        JarvisSession session = api.createSession(UUID.randomUUID().toString());
        RecognizedIntent recognizedIntent = api.getIntent(trainingSentence, session);
        assertThat(recognizedIntent).as("Not null recognized intent").isNotNull();
        assertThat(recognizedIntent.getDefinition()).as("Not null definition").isNotNull();
        softly.assertThat(recognizedIntent.getDefinition().getName()).as("Valid IntentDefinition").isEqualTo
                (intentDefinition.getName());
        assertThat(recognizedIntent.getOutContextInstances()).as("Valid out context instance list size").hasSize(2);

        /*
         * The first context is actually the second one defined in the IntentDefinition. DialogFlow does not provide
         * any specification on the order of the returned context. This test should be refactored to ensure that the
         * two contexts are defined without taking into account the order.
         */
        ContextInstance contextInstance2 = recognizedIntent.getOutContextInstances().get(0);
        assertThat(contextInstance2).as("Not null out context instance 2").isNotNull();
        assertThat(contextInstance2.getDefinition()).as("Not null out context instance 2 definition").isNotNull();
        softly.assertThat(contextInstance2.getDefinition().getName()).as("Valid out context instance 2 definition")
                .isEqualTo("Context2");
        assertThat(contextInstance2.getValues()).as("Out context instance 2 contains one value").hasSize(1);
        ContextParameterValue value2 = contextInstance2.getValues().get(0);
        assertThat(value2).as("Not null ContextParameterValue2").isNotNull();
        assertThat(value2.getContextParameter()).as("Not null ContextParameter2").isNotNull();
        softly.assertThat(value2.getContextParameter().getName()).as("Valid ContextParameter 2").isEqualTo
                (contextParameter2.getName());
        softly.assertThat(value2.getValue()).as("Valid ContextParameterValue 2").isEqualTo("steak");

        ContextInstance contextInstance1 = recognizedIntent.getOutContextInstances().get(1);
        assertThat(contextInstance1).as("Not null out context instance 1").isNotNull();
        assertThat(contextInstance1.getDefinition()).as("Not null out context instance 1 definition").isNotNull();
        softly.assertThat(contextInstance1.getDefinition().getName()).as("Valid out context instance 1 definition")
                .isEqualTo("Context1");
        assertThat(contextInstance1.getValues()).as("Out context instance 1 contains one value").hasSize(1);
        ContextParameterValue value1 = contextInstance1.getValues().get(0);
        assertThat(value1).as("Not null ContextParameterValue1").isNotNull();
        assertThat(value1.getContextParameter()).as("Not null ContextParameter1").isNotNull();
        softly.assertThat(value1.getContextParameter().getName()).as("Valid ContextParameter 1").isEqualTo
                (contextParameter1.getName());
        softly.assertThat(value1.getValue()).as("Valid ContextParameterValue 1").isEqualTo("cheese");
    }

    @Test
    public void getIntentInContextSetFromJarvisContext() {
        /*
         * Change the training sentence to avoid deleted intent definition matching (deleted intents can take some
         * time to be completely removed from the DialogFlow Agent, see https://discuss.api
         * .ai/t/intent-mismatch-issue/12042/17)
         */
        String trainingSentence = "how do you turn this on?";
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        intentDefinition.setName("TestHowDoYouTurn");
        intentDefinition.getTrainingSentences().add(trainingSentence);
        String inContextName = "testincontext";
        Context inContext = IntentFactory.eINSTANCE.createContext();
        inContext.setName(inContextName);
        intentDefinition.getInContexts().add(inContext);
        api = getValidDialogFlowApi();
        try {
            api.registerIntentDefinition(intentDefinition);
        } catch (DialogFlowException e) {
            Log.warn("The intent {0} is already registered", intentDefinition.getName());
        }
        api.trainMLEngine();
        jarvisCore.getEventDefinitionRegistry().registerEventDefinition(intentDefinition);
        /*
         * Setting this variable will delete the intent after execution. This should be done to ensure that the
         * intents are well created, but it causes intent propagation issues on the DialogFlow side (see
         * https://productforums.google.com/forum/m/#!category-topic/dialogflow/type-troubleshooting/UDokzc7mOcY)
         */
//        registeredIntentDefinition = intentDefinition;
        JarvisSession session = api.createSession(UUID.randomUUID().toString());
        /*
         * Set the input context in the JarvisSession's local context. If the intent is matched the local session has
          * been successfully merged in the Dialogflow one.
         */
        session.getJarvisContext().setContextValue(inContextName, "testKey", "testValue");
        RecognizedIntent recognizedIntent = api.getIntent(trainingSentence, session);
        assertThat(recognizedIntent).as("Not null recognized intent").isNotNull();
        assertThat(recognizedIntent.getDefinition()).as("Not null definition").isNotNull();
        softly.assertThat(recognizedIntent.getDefinition().getName()).as("Valid IntentDefinition").isEqualTo
                (intentDefinition.getName());
    }

    private void checkDialogFlowSession(JarvisSession session, String expectedProjectId, String expectedSessionId) {
        assertThat(session).as("Not null session").isNotNull();
        assertThat(session).as("The session is a DialogFlowSession instance").isInstanceOf(DialogFlowSession.class);
        DialogFlowSession dialogFlowSession = (DialogFlowSession) session;
        assertThat(dialogFlowSession.getSessionName()).as("Not null SessionName").isNotNull();
        softly.assertThat(dialogFlowSession.getSessionName().getProject()).as("Valid session project").isEqualTo
                (expectedProjectId);
        softly.assertThat(dialogFlowSession.getSessionName().getSession()).as("Valid session name").isEqualTo
                (expectedSessionId);
    }

}
