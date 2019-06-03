package com.xatkit.core;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.recognition.dialogflow.DialogFlowApiTest;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.core.session.RuntimeContexts;
import com.xatkit.execution.ActionInstance;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.execution.ExecutionRule;
import com.xatkit.intent.*;
import com.xatkit.platform.ActionDefinition;
import com.xatkit.platform.PlatformDefinition;
import com.xatkit.platform.PlatformFactory;
import com.xatkit.stubs.StubRuntimePlatform;
import com.xatkit.test.util.ElementFactory;
import com.xatkit.test.util.models.TestExecutionModel;
import com.xatkit.test.util.models.TestIntentModel;
import com.xatkit.test.util.models.TestPlatformModel;
import org.apache.commons.configuration2.Configuration;
import org.assertj.core.api.Assertions;
import org.junit.*;

import java.util.UUID;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class ExecutionServiceTest extends AbstractXatkitTest {

    protected static ExecutionModel VALID_EXECUTION_MODEL;

    protected static EventInstance VALID_EVENT_INSTANCE;

    /*
     * The EventInstance used to trigger onError ActionInstance computations.
     */
    protected static EventInstance ON_ERROR_EVENT_INSTANCE;

    protected static EntityDefinitionReference VALID_ENTITY_DEFINITION_REFERENCE;

    protected static XatkitCore VALID_XATKIT_CORE;

    @BeforeClass
    public static void setUpBeforeClass() {
        /*
         * Customize the TestExecutionModel to take into account error handling.
         */
        TestExecutionModel testExecutionModel = new TestExecutionModel();
        TestIntentModel testIntentModel = testExecutionModel.getTestIntentModel();
        TestPlatformModel testPlatformModel = testExecutionModel.getTestPlatformModel();

        VALID_EXECUTION_MODEL = testExecutionModel.getExecutionModel();
        Library intentLibrary = testIntentModel.getIntentLibrary();
        PlatformDefinition platformDefinition = testPlatformModel.getPlatformDefinition();

        ActionDefinition errorAction = PlatformFactory.eINSTANCE.createActionDefinition();
        errorAction.setName("ErroringStubRuntimeAction");
        platformDefinition.getActions().add(errorAction);
        IntentDefinition onErrorIntentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        onErrorIntentDefinition.setName("On error");
        intentLibrary.getEventDefinitions().add(onErrorIntentDefinition);

        ExecutionRule notMatchedRule = ExecutionFactory.eINSTANCE.createExecutionRule();
        notMatchedRule.setEvent(onErrorIntentDefinition);
        ActionInstance errorActionInstance = ExecutionFactory.eINSTANCE.createActionInstance();
        errorActionInstance.setAction(errorAction);
        /*
         * The ActionInstance that is executed when the errorActionInstance throws an exception.
         */
        ActionInstance onErrorActionInstance = ExecutionFactory.eINSTANCE.createActionInstance();
        onErrorActionInstance.setAction(testPlatformModel.getActionDefinition());
        errorActionInstance.getOnError().add(onErrorActionInstance);
        notMatchedRule.getInstructions().add(errorActionInstance);

        VALID_EXECUTION_MODEL.getExecutionRules().add(notMatchedRule);

        /*
         * Create the valid EventInstance used in handleEvent tests
         */
        VALID_EVENT_INSTANCE = IntentFactory.eINSTANCE.createEventInstance();
        VALID_EVENT_INSTANCE.setDefinition(testIntentModel.getIntentDefinition());
        ON_ERROR_EVENT_INSTANCE = IntentFactory.eINSTANCE.createEventInstance();
        ON_ERROR_EVENT_INSTANCE.setDefinition(onErrorIntentDefinition);
        VALID_ENTITY_DEFINITION_REFERENCE = ElementFactory.createBaseEntityDefinitionReference(EntityType.ANY);

        Configuration configuration = DialogFlowApiTest.buildConfiguration();
        configuration.addProperty(XatkitCore.EXECUTION_MODEL_KEY, VALID_EXECUTION_MODEL);
        VALID_XATKIT_CORE = new XatkitCore(configuration);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        if (nonNull(VALID_XATKIT_CORE) && !VALID_XATKIT_CORE.isShutdown()) {
            VALID_XATKIT_CORE.shutdown();
        }
    }

    @Before
    public void setUp() {
        RuntimePlatform runtimePlatform = VALID_XATKIT_CORE.getRuntimePlatformRegistry().getRuntimePlatform
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
        executionService = new ExecutionService(VALID_EXECUTION_MODEL, VALID_XATKIT_CORE
                .getRuntimePlatformRegistry());
        return executionService;
    }

    @Test(expected = NullPointerException.class)
    public void constructNullExecutionModel() {
        executionService = new ExecutionService(null, VALID_XATKIT_CORE.getRuntimePlatformRegistry());
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
        assertThat(executionService.getRuntimePlatformRegistry()).as("Valid Xatkit runtimePlatform registry").isEqualTo
                (VALID_XATKIT_CORE.getRuntimePlatformRegistry());
        assertThat(executionService.getExecutorService()).as("Not null ExecutorService").isNotNull();
        assertThat(executionService.isShutdown()).as("Execution service started").isFalse();
    }

    @Test(expected = NullPointerException.class)
    public void handleEventNullEvent() {
        executionService = getValidExecutionService();
        executionService.handleEventInstance(null, new XatkitSession("sessionID"));
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
        StubRuntimePlatform stubRuntimePlatform = (StubRuntimePlatform) VALID_XATKIT_CORE.getRuntimePlatformRegistry()
                .getRuntimePlatform("StubRuntimePlatform");
        executionService.handleEventInstance(VALID_EVENT_INSTANCE, VALID_XATKIT_CORE.getOrCreateXatkitSession
                ("sessionID"));
        /*
         * Sleep to ensure that the Action has been processed.
         */
        Thread.sleep(1000);
        Assertions.assertThat(stubRuntimePlatform.getAction().isActionProcessed()).as("Action processed").isTrue();
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
        contextParameter1.setEntity(VALID_ENTITY_DEFINITION_REFERENCE);
        contextParameter1.setTextFragment("love");
        outContext1.getParameters().add(contextParameter1);
        Context outContext2 = IntentFactory.eINSTANCE.createContext();
        outContext2.setName("Context2");
        ContextParameter contextParameter2 = IntentFactory.eINSTANCE.createContextParameter();
        contextParameter2.setName("Parameter2");
        contextParameter2.setEntity(VALID_ENTITY_DEFINITION_REFERENCE);
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
        XatkitSession session = new XatkitSession(UUID.randomUUID().toString());
        /*
         * Should not trigger any action, the EventDefinition is not registered in the execution model.
         */
        executionService.handleEventInstance(instance, session);
        /*
         * Sleep to ensure that the Action has been processed.
         */
        Thread.sleep(1000);
        RuntimeContexts context = session.getRuntimeContexts();
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
        StubRuntimePlatform stubRuntimePlatform = (StubRuntimePlatform) VALID_XATKIT_CORE.getRuntimePlatformRegistry()
                .getRuntimePlatform("StubRuntimePlatform");
        EventDefinition notHandledDefinition = IntentFactory.eINSTANCE.createEventDefinition();
        notHandledDefinition.setName("NotHandled");
        EventInstance notHandledEventInstance = IntentFactory.eINSTANCE.createEventInstance();
        notHandledEventInstance.setDefinition(notHandledDefinition);
        executionService.handleEventInstance(notHandledEventInstance, VALID_XATKIT_CORE.getOrCreateXatkitSession
                ("sessionID"));
        /*
         * Sleep to ensure that the Action has been processed.
         */
        Thread.sleep(1000);
        Assertions.assertThat(stubRuntimePlatform.getAction().isActionProcessed()).as("Action not processed").isFalse();
    }

    @Test
    public void executedRuntimeActionErroringAction() throws InterruptedException {
        executionService = getValidExecutionService();
        StubRuntimePlatform stubRuntimePlatform = (StubRuntimePlatform) VALID_XATKIT_CORE.getRuntimePlatformRegistry()
                .getRuntimePlatform("StubRuntimePlatform");
        executionService.handleEventInstance(ON_ERROR_EVENT_INSTANCE, new XatkitSession(UUID.randomUUID()
                .toString()));
        Thread.sleep(1000);
        Assertions.assertThat(stubRuntimePlatform.getErroringAction().isActionProcessed()).as("Erroring action processed").isTrue();
        Assertions.assertThat(stubRuntimePlatform.getAction().isActionProcessed()).as("Fallback action processed").isTrue();
    }
}
