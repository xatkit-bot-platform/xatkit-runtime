package fr.zelus.jarvis.core;

import fr.zelus.jarvis.AbstractJarvisTest;
import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.execution.ActionInstance;
import fr.zelus.jarvis.execution.ExecutionFactory;
import fr.zelus.jarvis.execution.ExecutionModel;
import fr.zelus.jarvis.execution.ExecutionRule;
import fr.zelus.jarvis.intent.*;
import fr.zelus.jarvis.platform.Action;
import fr.zelus.jarvis.platform.InputProviderDefinition;
import fr.zelus.jarvis.platform.Platform;
import fr.zelus.jarvis.platform.PlatformFactory;
import fr.zelus.jarvis.recognition.dialogflow.DialogFlowApi;
import fr.zelus.jarvis.stubs.StubRuntimePlatform;
import fr.zelus.jarvis.test.util.VariableLoaderHelper;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.*;

import java.util.UUID;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class ExecutionServiceTest extends AbstractJarvisTest {

    protected static String VALID_PROJECT_ID = VariableLoaderHelper.getJarvisDialogFlowProject();

    protected static String VALID_LANGUAGE_CODE = "en-US";

    protected static ExecutionModel VALID_EXECUTION_MODEL;

    protected static EventInstance VALID_EVENT_INSTANCE;

    /*
     * The EventInstance used to trigger onError ActionInstance computations.
     */
    protected static EventInstance ON_ERROR_EVENT_INSTANCE;

    protected static EntityDefinition VALID_ENTITY_DEFINITION;

    protected static JarvisCore VALID_JARVIS_CORE;

    @BeforeClass
    public static void setUpBeforeClass() {
        Platform stubPlatform = PlatformFactory.eINSTANCE.createPlatform();
        stubPlatform.setName("StubRuntimePlatform");
        stubPlatform.setRuntimePath("fr.zelus.jarvis.stubs.StubRuntimePlatform");
        Action stubAction = PlatformFactory.eINSTANCE.createAction();
        stubAction.setName("StubRuntimeAction");
        Action errorAction = PlatformFactory.eINSTANCE.createAction();
        errorAction.setName("ErroringStubRuntimeAction");
        // No parameters, keep it simple
        stubPlatform.getActions().add(stubAction);
        stubPlatform.getActions().add(errorAction);
        InputProviderDefinition stubInputProvider = PlatformFactory.eINSTANCE.createInputProviderDefinition();
        stubInputProvider.setName("StubInputProvider");
        stubPlatform.getEventProviderDefinitions().add(stubInputProvider);
        IntentDefinition stubIntentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        stubIntentDefinition.setName("Default Welcome Intent");
        IntentDefinition onErrorIntentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        onErrorIntentDefinition.setName("On error");

        /*
         * Create the valid EventInstance used in handleEvent tests
         */
        VALID_EVENT_INSTANCE = IntentFactory.eINSTANCE.createEventInstance();
        VALID_EVENT_INSTANCE.setDefinition(stubIntentDefinition);
        ON_ERROR_EVENT_INSTANCE = IntentFactory.eINSTANCE.createEventInstance();
        ON_ERROR_EVENT_INSTANCE.setDefinition(onErrorIntentDefinition);
        // No parameters, keep it simple
        VALID_EXECUTION_MODEL = ExecutionFactory.eINSTANCE.createExecutionModel();
        ExecutionRule rule = ExecutionFactory.eINSTANCE.createExecutionRule();
        rule.setEvent(stubIntentDefinition);
        ActionInstance actionInstance = ExecutionFactory.eINSTANCE.createActionInstance();
        actionInstance.setAction(stubAction);
        rule.getActions().add(actionInstance);
        ExecutionRule notMatchedRule = ExecutionFactory.eINSTANCE.createExecutionRule();
        notMatchedRule.setEvent(onErrorIntentDefinition);
        ActionInstance errorActionInstance = ExecutionFactory.eINSTANCE.createActionInstance();
        errorActionInstance.setAction(errorAction);
        /*
         * The ActionInstance that is executed when the errorActionInstance throws an exception.
         */
        ActionInstance onErrorActionInstance = ExecutionFactory.eINSTANCE.createActionInstance();
        onErrorActionInstance.setAction(stubAction);
        errorActionInstance.getOnError().add(onErrorActionInstance);
        notMatchedRule.getActions().add(errorActionInstance);
        VALID_EXECUTION_MODEL.getExecutionRules().add(rule);
        VALID_EXECUTION_MODEL.getExecutionRules().add(notMatchedRule);
        VALID_ENTITY_DEFINITION = IntentFactory.eINSTANCE.createBaseEntityDefinition();
        ((BaseEntityDefinition) VALID_ENTITY_DEFINITION).setEntityType(EntityType.ANY);


        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(DialogFlowApi.PROJECT_ID_KEY, VALID_PROJECT_ID);
        configuration.addProperty(DialogFlowApi.LANGUAGE_CODE_KEY, VALID_LANGUAGE_CODE);
        configuration.addProperty(JarvisCore.EXECUTION_MODEL_KEY, VALID_EXECUTION_MODEL);
        VALID_JARVIS_CORE = new JarvisCore(configuration);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        if (nonNull(VALID_JARVIS_CORE) && !VALID_JARVIS_CORE.isShutdown()) {
            VALID_JARVIS_CORE.shutdown();
        }
    }

    @Before
    public void setUp() {
        RuntimePlatform runtimePlatform = VALID_JARVIS_CORE.getRuntimePlatformRegistry().getRuntimePlatform
                ("StubRuntimePlatform");
        if(nonNull(runtimePlatform)) {
            /*
             * Call init() to reset the platform actions and avoid comparison issues when checking their state.
             */
            ((StubRuntimePlatform) runtimePlatform).init();
        }
    }

    @After
    public void tearDown() {
        if (nonNull(executionService) && !executionService.isShutdown()) {
            executionService.shutdown();
        }
    }

    private ExecutionService executionService;

    private ExecutionService getValidExecutionService() {
        executionService = new ExecutionService(VALID_EXECUTION_MODEL, VALID_JARVIS_CORE
                .getRuntimePlatformRegistry());
        return executionService;
    }

    @Test(expected = NullPointerException.class)
    public void constructNullExecutionModel() {
        executionService = new ExecutionService(null, VALID_JARVIS_CORE.getRuntimePlatformRegistry());
    }

    @Test(expected = NullPointerException.class)
    public void constructNullRuntimePlatformRegistry() {
        executionService = new ExecutionService(VALID_EXECUTION_MODEL, null);
    }

    @Test
    public void constructValid() {
        executionService = getValidExecutionService();
        assertThat(executionService.getExecutionModel()).as("Valid execution model").isEqualTo
                (VALID_EXECUTION_MODEL);
        assertThat(executionService.getRuntimePlatformRegistry()).as("Valid Jarvis runtimePlatform registry").isEqualTo
                (VALID_JARVIS_CORE.getRuntimePlatformRegistry());
        assertThat(executionService.getExecutorService()).as("Not null ExecutorService").isNotNull();
        assertThat(executionService.isShutdown()).as("Execution service started").isFalse();
    }

    @Test(expected = NullPointerException.class)
    public void handleEventNullEvent() {
        executionService = getValidExecutionService();
        executionService.handleEventInstance(null, new JarvisSession("sessionID"));
    }

    @Test(expected = NullPointerException.class)
    public void handleEventNullSession() {
        executionService = getValidExecutionService();
        executionService.handleEventInstance(VALID_EVENT_INSTANCE, null);
    }

    @Test
    public void handleEventValidEvent() throws InterruptedException {
        executionService = getValidExecutionService();
        /*
         * Retrieve the stub RuntimePlatform to check that its action has been processed.
         */
        StubRuntimePlatform stubRuntimePlatform = (StubRuntimePlatform) VALID_JARVIS_CORE.getRuntimePlatformRegistry()
                .getRuntimePlatform("StubRuntimePlatform");
        executionService.handleEventInstance(VALID_EVENT_INSTANCE, VALID_JARVIS_CORE.getOrCreateJarvisSession
                ("sessionID"));
        /*
         * Sleep to ensure that the Action has been processed.
         */
        Thread.sleep(1000);
        assertThat(stubRuntimePlatform.getAction().isActionProcessed()).as("Action processed").isTrue();
    }

    @Test
    public void handleEventMultiOutputContext() throws InterruptedException {
        /*
         * Create the EventDefinition
         */
        String trainingSentence = "I love the monkey head";
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        intentDefinition.setName("TestMonkeyHead");
        intentDefinition.getTrainingSentences().add(trainingSentence);
        Context outContext1 = IntentFactory.eINSTANCE.createContext();
        outContext1.setName("Context1");
        ContextParameter contextParameter1 = IntentFactory.eINSTANCE.createContextParameter();
        contextParameter1.setName("Parameter1");
        contextParameter1.setEntity(VALID_ENTITY_DEFINITION);
        contextParameter1.setTextFragment("love");
        outContext1.getParameters().add(contextParameter1);
        Context outContext2 = IntentFactory.eINSTANCE.createContext();
        outContext2.setName("Context2");
        ContextParameter contextParameter2 = IntentFactory.eINSTANCE.createContextParameter();
        contextParameter2.setName("Parameter2");
        contextParameter2.setEntity(VALID_ENTITY_DEFINITION);
        contextParameter2.setTextFragment("monkey");
        outContext2.getParameters().add(contextParameter2);
        intentDefinition.getOutContexts().add(outContext1);
        intentDefinition.getOutContexts().add(outContext2);
        /*
         * Create the EventInstance
         */
        EventInstance instance = IntentFactory.eINSTANCE.createEventInstance();
        instance.setDefinition(intentDefinition);
        ContextParameterValue value1 = IntentFactory.eINSTANCE.createContextParameterValue();
        value1.setContextParameter(contextParameter1);
        value1.setValue("love");
        ContextInstance contextInstance1 = IntentFactory.eINSTANCE.createContextInstance();
        contextInstance1.setDefinition(outContext1);
        contextInstance1.setLifespanCount(outContext1.getLifeSpan());
        contextInstance1.getValues().add(value1);
        instance.getOutContextInstances().add(contextInstance1);
        ContextParameterValue value2 = IntentFactory.eINSTANCE.createContextParameterValue();
        value2.setContextParameter(contextParameter2);
        value2.setValue("monkey");
        ContextInstance contextInstance2 = IntentFactory.eINSTANCE.createContextInstance();
        contextInstance2.setDefinition(outContext2);
        contextInstance2.setLifespanCount(outContext2.getLifeSpan());
        contextInstance2.getValues().add(value2);
        instance.getOutContextInstances().add(contextInstance2);
        executionService = getValidExecutionService();
        JarvisSession session = new JarvisSession(UUID.randomUUID().toString());
        /*
         * Should not trigger any action, the EventDefinition is not registered in the execution model.
         */
        executionService.handleEventInstance(instance, session);
        /*
         * Sleep to ensure that the Action has been processed.
         */
        Thread.sleep(1000);
        JarvisContext context = session.getJarvisContext();
        assertThat(context.getContextVariables("Context1")).as("Not null Context1 variable map").isNotNull();
        assertThat(context.getContextVariables("Context1").keySet()).as("Context1 variable map contains a single " +
                "variable").hasSize(1);
        assertThat(context.getContextValue("Context1", "Parameter1")).as("Not null Context1.Parameter1 value")
                .isNotNull();
        assertThat(context.getContextValue("Context1", "Parameter1")).as("Valid Context1.Parameter1 value").isEqualTo
                ("love");
        assertThat(context.getContextVariables("Context2")).as("Not null Context2 variable map").isNotNull();
        assertThat(context.getContextVariables("Context2").keySet()).as("Context2 variable map contains a single " +
                "variable")
                .hasSize(1);
        assertThat(context.getContextValue("Context2", "Parameter2")).as("Not null Context2.Parameter2 value")
                .isNotNull();
        assertThat(context.getContextValue("Context2", "Parameter2")).as("Valid Context2.Parameter2 value").isEqualTo
                ("monkey");
    }

    @Test
    public void handleEventNotHandledEvent() throws InterruptedException {
        executionService = getValidExecutionService();
        /*
         * Retrieve the stub RuntimePlatform to check that its action has been processed.
         */
        StubRuntimePlatform stubRuntimePlatform = (StubRuntimePlatform) VALID_JARVIS_CORE.getRuntimePlatformRegistry()
                .getRuntimePlatform("StubRuntimePlatform");
        EventDefinition notHandledDefinition = IntentFactory.eINSTANCE.createEventDefinition();
        notHandledDefinition.setName("NotHandled");
        EventInstance notHandledEventInstance = IntentFactory.eINSTANCE.createEventInstance();
        notHandledEventInstance.setDefinition(notHandledDefinition);
        executionService.handleEventInstance(notHandledEventInstance, VALID_JARVIS_CORE.getOrCreateJarvisSession
                ("sessionID"));
        /*
         * Sleep to ensure that the Action has been processed.
         */
        Thread.sleep(1000);
        assertThat(stubRuntimePlatform.getAction().isActionProcessed()).as("Action not processed").isFalse();
    }

    @Test
    public void executedRuntimeActionErroringAction() throws InterruptedException {
        executionService = getValidExecutionService();
        StubRuntimePlatform stubRuntimePlatform = (StubRuntimePlatform) VALID_JARVIS_CORE.getRuntimePlatformRegistry()
                .getRuntimePlatform("StubRuntimePlatform");
        executionService.handleEventInstance(ON_ERROR_EVENT_INSTANCE, new JarvisSession(UUID.randomUUID()
                .toString()));
        Thread.sleep(1000);
        assertThat(stubRuntimePlatform.getErroringAction().isActionProcessed()).as("Erroring action processed").isTrue();
        assertThat(stubRuntimePlatform.getAction().isActionProcessed()).as("Fallback action processed").isTrue();
    }
}
