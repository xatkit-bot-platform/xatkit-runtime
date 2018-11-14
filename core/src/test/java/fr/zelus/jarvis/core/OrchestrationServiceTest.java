package fr.zelus.jarvis.core;

import fr.zelus.jarvis.AbstractJarvisTest;
import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.intent.*;
import fr.zelus.jarvis.orchestration.ActionInstance;
import fr.zelus.jarvis.orchestration.OrchestrationFactory;
import fr.zelus.jarvis.orchestration.OrchestrationLink;
import fr.zelus.jarvis.orchestration.OrchestrationModel;
import fr.zelus.jarvis.platform.Action;
import fr.zelus.jarvis.platform.InputProviderDefinition;
import fr.zelus.jarvis.platform.Platform;
import fr.zelus.jarvis.platform.PlatformFactory;
import fr.zelus.jarvis.recognition.dialogflow.DialogFlowApi;
import fr.zelus.jarvis.stubs.StubJarvisModule;
import fr.zelus.jarvis.test.util.VariableLoaderHelper;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class OrchestrationServiceTest extends AbstractJarvisTest {

    protected static String VALID_PROJECT_ID = VariableLoaderHelper.getJarvisDialogFlowProject();

    protected static String VALID_LANGUAGE_CODE = "en-US";

    protected static OrchestrationModel VALID_ORCHESTRATION_MODEL;

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
        stubPlatform.setName("StubJarvisModule");
        stubPlatform.setRuntimePath("fr.zelus.jarvis.stubs.StubJarvisModule");
        Action stubAction = PlatformFactory.eINSTANCE.createAction();
        stubAction.setName("StubJarvisAction");
        Action errorAction = PlatformFactory.eINSTANCE.createAction();
        errorAction.setName("ErroringStubJarvisAction");
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
        stubPlatform.getIntentDefinitions().add(stubIntentDefinition);
        VALID_ORCHESTRATION_MODEL = OrchestrationFactory.eINSTANCE.createOrchestrationModel();
        OrchestrationLink link = OrchestrationFactory.eINSTANCE.createOrchestrationLink();
        link.setEvent(stubIntentDefinition);
        ActionInstance actionInstance = OrchestrationFactory.eINSTANCE.createActionInstance();
        actionInstance.setAction(stubAction);
        link.getActions().add(actionInstance);
        OrchestrationLink notMatchedLink = OrchestrationFactory.eINSTANCE.createOrchestrationLink();
        notMatchedLink.setEvent(onErrorIntentDefinition);
        ActionInstance errorActionInstance = OrchestrationFactory.eINSTANCE.createActionInstance();
        errorActionInstance.setAction(errorAction);
        /*
         * The ActionInstance that is executed when the errorActionInstance throws an exception.
         */
        ActionInstance onErrorActionInstance = OrchestrationFactory.eINSTANCE.createActionInstance();
        onErrorActionInstance.setAction(stubAction);
        errorActionInstance.getOnError().add(onErrorActionInstance);
        notMatchedLink.getActions().add(errorActionInstance);
        VALID_ORCHESTRATION_MODEL.getOrchestrationLinks().add(link);
        VALID_ORCHESTRATION_MODEL.getOrchestrationLinks().add(notMatchedLink);
        VALID_ENTITY_DEFINITION = IntentFactory.eINSTANCE.createBaseEntityDefinition();
        ((BaseEntityDefinition) VALID_ENTITY_DEFINITION).setEntityType(EntityType.ANY);


        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(DialogFlowApi.PROJECT_ID_KEY, VALID_PROJECT_ID);
        configuration.addProperty(DialogFlowApi.LANGUAGE_CODE_KEY, VALID_LANGUAGE_CODE);
        configuration.addProperty(JarvisCore.ORCHESTRATION_MODEL_KEY, VALID_ORCHESTRATION_MODEL);
        VALID_JARVIS_CORE = new JarvisCore(configuration);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        if (nonNull(VALID_JARVIS_CORE) && !VALID_JARVIS_CORE.isShutdown()) {
            VALID_JARVIS_CORE.shutdown();
        }
    }

    @After
    public void tearDown() {
        if (nonNull(orchestrationService) && !orchestrationService.isShutdown()) {
            orchestrationService.shutdown();
        }
    }

    private OrchestrationService orchestrationService;

    private OrchestrationService getValidOrchestrationService() {
        orchestrationService = new OrchestrationService(VALID_ORCHESTRATION_MODEL, VALID_JARVIS_CORE
                .getJarvisModuleRegistry());
        return orchestrationService;
    }

    @Test(expected = NullPointerException.class)
    public void constructNullOrchestrationModel() {
        orchestrationService = new OrchestrationService(null, VALID_JARVIS_CORE.getJarvisModuleRegistry());
    }

    @Test(expected = NullPointerException.class)
    public void constructNullJarvisModuleRegistry() {
        orchestrationService = new OrchestrationService(VALID_ORCHESTRATION_MODEL, null);
    }

    @Test
    public void constructValid() {
        orchestrationService = getValidOrchestrationService();
        assertThat(orchestrationService.getOrchestrationModel()).as("Valid orchestration model").isEqualTo
                (VALID_ORCHESTRATION_MODEL);
        assertThat(orchestrationService.getJarvisModuleRegistry()).as("Valid Jarvis module registry").isEqualTo
                (VALID_JARVIS_CORE.getJarvisModuleRegistry());
        assertThat(orchestrationService.getExecutorService()).as("Not null ExecutorService").isNotNull();
        assertThat(orchestrationService.isShutdown()).as("Orchestration service started").isFalse();
    }

    @Test(expected = NullPointerException.class)
    public void handleEventNullEvent() {
        orchestrationService = getValidOrchestrationService();
        orchestrationService.handleEventInstance(null, new JarvisSession("sessionID"));
    }

    @Test(expected = NullPointerException.class)
    public void handleEventNullSession() {
        orchestrationService = getValidOrchestrationService();
        orchestrationService.handleEventInstance(VALID_EVENT_INSTANCE, null);
    }

    @Test
    public void handleEventValidEvent() throws InterruptedException {
        orchestrationService = getValidOrchestrationService();
        /*
         * Retrieve the stub JarvisModule to check that its action has been processed.
         */
        StubJarvisModule stubJarvisModule = (StubJarvisModule) VALID_JARVIS_CORE.getJarvisModuleRegistry()
                .getJarvisModule
                        ("StubJarvisModule");
        orchestrationService.handleEventInstance(VALID_EVENT_INSTANCE, VALID_JARVIS_CORE.getOrCreateJarvisSession
                ("sessionID"));
        /*
         * Sleep to ensure that the Action has been processed.
         */
        Thread.sleep(1000);
        assertThat(stubJarvisModule.getAction().isActionProcessed()).as("Action processed").isTrue();
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
        orchestrationService = getValidOrchestrationService();
        JarvisSession session = new JarvisSession(UUID.randomUUID().toString());
        /*
         * Should not trigger any action, the EventDefinition is not registered in the orchestration model.
         */
        orchestrationService.handleEventInstance(instance, session);
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
        orchestrationService = getValidOrchestrationService();
        /*
         * Retrieve the stub JarvisModule to check that its action has been processed.
         */
        StubJarvisModule stubJarvisModule = (StubJarvisModule) VALID_JARVIS_CORE.getJarvisModuleRegistry()
                .getJarvisModule("StubJarvisModule");
        EventDefinition notHandledDefinition = IntentFactory.eINSTANCE.createEventDefinition();
        notHandledDefinition.setName("NotHandled");
        EventInstance notHandledEventInstance = IntentFactory.eINSTANCE.createEventInstance();
        notHandledEventInstance.setDefinition(notHandledDefinition);
        orchestrationService.handleEventInstance(notHandledEventInstance, VALID_JARVIS_CORE.getOrCreateJarvisSession
                ("sessionID"));
        /*
         * Sleep to ensure that the Action has been processed.
         */
        Thread.sleep(1000);
        assertThat(stubJarvisModule.getAction().isActionProcessed()).as("Action not processed").isFalse();
    }

    @Test
    public void executedJarvisActionErroringAction() throws InterruptedException {
        orchestrationService = getValidOrchestrationService();
        StubJarvisModule stubJarvisModule = (StubJarvisModule) VALID_JARVIS_CORE.getJarvisModuleRegistry()
                .getJarvisModule("StubJarvisModule");
        orchestrationService.handleEventInstance(ON_ERROR_EVENT_INSTANCE, new JarvisSession(UUID.randomUUID()
                .toString()));
        Thread.sleep(1000);
        assertThat(stubJarvisModule.getErroringAction().isActionProcessed()).as("Erroring action processed").isTrue();
        assertThat(stubJarvisModule.getAction().isActionProcessed()).as("Fallback action processed").isTrue();
    }
}
