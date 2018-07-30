package fr.zelus.jarvis.core;

import fr.zelus.jarvis.AbstractJarvisTest;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.io.WebhookEventProvider;
import fr.zelus.jarvis.module.Action;
import fr.zelus.jarvis.module.EventProviderDefinition;
import fr.zelus.jarvis.module.ModuleFactory;
import fr.zelus.jarvis.module.Parameter;
import fr.zelus.jarvis.orchestration.*;
import fr.zelus.jarvis.server.JarvisServer;
import fr.zelus.jarvis.stubs.EmptyJarvisModule;
import fr.zelus.jarvis.stubs.StubJarvisCore;
import fr.zelus.jarvis.stubs.action.StubJarvisActionNoParameter;
import fr.zelus.jarvis.stubs.action.StubJarvisActionTwoConstructors;
import fr.zelus.jarvis.stubs.io.StubInputProvider;
import fr.zelus.jarvis.stubs.io.StubInputProviderNoConfigurationConstructor;
import fr.zelus.jarvis.stubs.io.StubJsonWebhookEventProvider;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class JarvisModuleTest extends AbstractJarvisTest {

    private JarvisModule module;

    private static JarvisCore jarvisCore;

    @BeforeClass
    public static void setUpBeforeClass() {
        jarvisCore = new StubJarvisCore();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        if (nonNull(jarvisCore)) {
            jarvisCore.shutdown();
        }
    }

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Before
    public void setUp() {
        module = new EmptyJarvisModule();
        if (nonNull(jarvisCore)) {
            /*
             * Unregister the WebhookEventProviders that may have been set as side effect of startEventProvider
             */
            JarvisServer jarvisServer = jarvisCore.getJarvisServer();
            for (WebhookEventProvider eventProvider : jarvisServer.getRegisteredWebhookEventProviders()) {
                jarvisServer.unregisterWebhookEventProvider(eventProvider);
            }
        }
        if (!jarvisCore.getJarvisServer().getRegisteredWebhookEventProviders().isEmpty()) {

        }
    }

    @After
    public void tearDown() {
        if(nonNull(module)) {
            module.shutdown();
        }
    }

    @Test
    public void getName() {
        assertThat(module.getName()).as("Valid module name").isEqualTo("EmptyJarvisModule");
    }

    @Test(expected = NullPointerException.class)
    public void startEventProviderNullEventProviderDefinition() {
        module.startEventProvider(null, jarvisCore);
    }

    @Test(expected = NullPointerException.class)
    public void startEventProviderNullJarvisCore() {
        EventProviderDefinition eventProviderDefinition = ModuleFactory.eINSTANCE.createEventProviderDefinition();
        eventProviderDefinition.setName("StubInputProvider");
        module.startEventProvider(eventProviderDefinition, null);
    }

    @Test(expected = JarvisException.class)
    public void startEventProviderInvalidName() {
        EventProviderDefinition eventProviderDefinition = ModuleFactory.eINSTANCE.createEventProviderDefinition();
        eventProviderDefinition.setName("Test");
        module.startEventProvider(eventProviderDefinition, jarvisCore);
    }

    @Test
    public void startValidEventProviderNotWebhook() {
        EventProviderDefinition eventProviderDefinition = ModuleFactory.eINSTANCE.createEventProviderDefinition();
        eventProviderDefinition.setName("StubInputProvider");
        module.startEventProvider(eventProviderDefinition, jarvisCore);
        assertThat(module.getEventProviderMap()).as("Not empty EventProvider map").isNotEmpty();
        assertThat(module.getEventProviderMap().get(eventProviderDefinition.getName())).as("Valid EventProvider map " +
                "entry").isNotNull();
        JarvisModule.EventProviderThread eventProviderThread = module.getEventProviderMap().get
                (eventProviderDefinition.getName());
        softly.assertThat(eventProviderThread.getEventProvider()).as("EventProvider in thread is valid").isInstanceOf
                (StubInputProvider.class);
        softly.assertThat(eventProviderThread.isAlive()).as("EventProvider thread is alive").isTrue();
        assertThat(jarvisCore.getJarvisServer().getRegisteredWebhookEventProviders()).as("Empty registered webhook " +
                "providers in JarvisServer").isEmpty();
    }

    @Test
    public void startValidEventProviderNotWebhookNoConfigurationConstructor() {
        EventProviderDefinition eventProviderDefinition = ModuleFactory.eINSTANCE.createEventProviderDefinition();
        eventProviderDefinition.setName("StubInputProviderNoConfigurationConstructor");
        module.startEventProvider(eventProviderDefinition, jarvisCore);
        assertThat(module.getEventProviderMap()).as("Not empty EventProvider map").isNotEmpty();
        assertThat(module.getEventProviderMap().get(eventProviderDefinition.getName())).as("Valid EventProvider map " +
                "entry").isNotNull();
        JarvisModule.EventProviderThread eventProviderThread = module.getEventProviderMap().get
                (eventProviderDefinition.getName());
        softly.assertThat(eventProviderThread.getEventProvider()).as("EventProvider in thread is valid").isInstanceOf
                (StubInputProviderNoConfigurationConstructor.class);
        softly.assertThat(eventProviderThread.isAlive()).as("EventProvider thread is alive").isTrue();
        assertThat(jarvisCore.getJarvisServer().getRegisteredWebhookEventProviders()).as("Empty registered webhook " +
                "providers in JarvisServer").isEmpty();
    }

    @Test
    public void startValidEventProviderWebhook() {
        EventProviderDefinition eventProviderDefinition = ModuleFactory.eINSTANCE.createEventProviderDefinition();
        eventProviderDefinition.setName("StubJsonWebhookEventProvider");
        module.startEventProvider(eventProviderDefinition, jarvisCore);
        assertThat(module.getEventProviderMap()).as("Not empty EventProvider map").isNotEmpty();
        assertThat(module.getEventProviderMap().get(eventProviderDefinition.getName())).as("Valid EventProvider map " +
                "entry").isNotNull();
        JarvisModule.EventProviderThread eventProviderThread = module.getEventProviderMap().get
                (eventProviderDefinition.getName());
        softly.assertThat(eventProviderThread.getEventProvider()).as("EventProvider in thread is valid").isInstanceOf
                (StubJsonWebhookEventProvider.class);
        softly.assertThat(eventProviderThread.isAlive()).as("EventProvider thread is alive").isTrue();
        assertThat(jarvisCore.getJarvisServer().getRegisteredWebhookEventProviders()).as("Webhook provider " +
                "registered" + "in JarvisServer").hasSize(1);
        softly.assertThat(jarvisCore.getJarvisServer().getRegisteredWebhookEventProviders().iterator().next()).as
                ("Valid Webhook registered in JarvisServer").isInstanceOf(StubJsonWebhookEventProvider.class);
    }

    @Test(expected = JarvisException.class)
    public void enableActionNotModuleAction() {
        Action action = getNotRegisteredAction();
        module.enableAction(action);
    }

    @Test
    public void enableActionModuleAction() {
        Action infoAction = getNoParameterAction();
        module.enableAction(infoAction);
        assertThat(module.getActions()).as("Action map contains the enabled action").contains
                (StubJarvisActionNoParameter.class);
    }

    @Test
    public void disableActionNotModuleAction() {
        Action action = getNotRegisteredAction();
        module.disableAction(action);
    }

    @Test
    public void disableActionModuleAction() {
        Action action = getNoParameterAction();
        module.enableAction(action);
        module.disableAction(action);
        assertThat(module.getActions()).as("The action map does not contain the unregistered action").doesNotContain
                (StubJarvisActionNoParameter.class);
    }

    @Test
    public void disableAllActions() {
        Action action1 = getNoParameterAction();
        Action action2 = getParameterAction();
        module.enableAction(action1);
        module.enableAction(action2);
        module.disableAllActions();
        assertThat(module.getActions()).as("The action map is empty").isEmpty();
    }

    @Test(expected = NullPointerException.class)
    public void createJarvisActionNullActionInstance() {
        module.createJarvisAction(null, new JarvisSession("id"));
    }

    @Test(expected = JarvisException.class)
    public void createJarvisActionNotEnabledAction() {
        Action action = getNoParameterAction();
        ActionInstance actionInstance = OrchestrationFactory.eINSTANCE.createActionInstance();
        actionInstance.setAction(action);
        module.createJarvisAction(actionInstance, new JarvisSession("id"));
    }

    @Test
    public void createJarvisActionValidActionInstanceNoParameters() {
        Action action = getNoParameterAction();
        ActionInstance actionInstance = createActionInstanceFor(action);
        JarvisAction jarvisAction = module.createJarvisAction(actionInstance, new JarvisSession("sessionID"));
        assertThat(jarvisAction).as("Created JarvisAction type is valid").isInstanceOf(StubJarvisActionNoParameter
                .class);
    }

    @Test
    public void createJarvisActionValidActionInstanceWithReturnType() {
        Action action = getNoParameterAction();
        ActionInstance actionInstance = createActionInstanceFor(action);
        Variable returnVariable = OrchestrationFactory.eINSTANCE.createVariable();
        returnVariable.setName("return");
        VariableAccess returnVariableAccess = OrchestrationFactory.eINSTANCE.createVariableAccess();
        returnVariableAccess.setReferredVariable(returnVariable);
        actionInstance.setReturnVariable(returnVariableAccess);
        JarvisAction jarvisAction = module.createJarvisAction(actionInstance, new JarvisSession("session"));
        assertThat(jarvisAction.getReturnVariable()).as("Valid return variable name").isEqualTo(returnVariable
                .getName());
    }

    @Test(expected = JarvisException.class)
    public void createJarvisActionTooManyParametersInAction() {
        Action action = getNoParameterAction();
        ActionInstance actionInstance = createActionInstanceFor(action);
        Parameter param = ModuleFactory.eINSTANCE.createParameter();
        param.setKey("myParam");
        action.getParameters().add(param);
        StringValue value = OrchestrationFactory.eINSTANCE.createStringValue();
//        value.setParameter(param);
        value.setValue("myValue");
        actionInstance.getValues().add(value);
        module.createJarvisAction(actionInstance, new JarvisSession("sessionID"));
    }

    @Test(expected = JarvisException.class)
    public void createJarvisActionTooManyParametersInActionInstance() {
        Action action = getNoParameterAction();
        ActionInstance actionInstance = createActionInstanceFor(action);
        Parameter param = ModuleFactory.eINSTANCE.createParameter();
        param.setKey("myParam");
        // Do not attach the Parameter to the Action
        StringValue value = OrchestrationFactory.eINSTANCE.createStringValue();
//        value.setParameter(param);
        value.setValue("myValue");
        actionInstance.getValues().add(value);
        module.createJarvisAction(actionInstance, new JarvisSession("sessionID"));
    }

    @Test
    public void createJarvisParameterActionConstructor1ValidActionInstanceVariableAccess() {
        Action action = getParameterAction();
        ActionInstance actionInstance = createActionInstanceFor(action);
        Variable paramVariable = OrchestrationFactory.eINSTANCE.createVariable();
        paramVariable.setName("param");
        VariableAccess variableAccess = OrchestrationFactory.eINSTANCE.createVariableAccess();
        variableAccess.setReferredVariable(paramVariable);
        actionInstance.getValues().add(variableAccess);
        // Register the variable in the context to allow its access
        JarvisSession session = new JarvisSession("sessionID");
        session.getJarvisContext().setContextValue("variables", "param", CompletableFuture.completedFuture("test"));
        JarvisAction jarvisAction = module.createJarvisAction(actionInstance, session);
        assertThat(jarvisAction).as("Created JarvisAction type is valid").isInstanceOf
                (StubJarvisActionTwoConstructors.class);
        StubJarvisActionTwoConstructors jarvisActionTwoConstructors = (StubJarvisActionTwoConstructors) jarvisAction;
        softly.assertThat(jarvisActionTwoConstructors.getParam()).as("Constructor1 called").isEqualTo("test");
        softly.assertThat(jarvisActionTwoConstructors.getListParam()).as("Constructor2 not called").isNull();
    }

    @Test
    public void createJarvisParameterActionConstructor2ValidActionInstanceVariableAccess() {
        Action action = getParameterAction();
        ActionInstance actionInstance = createActionInstanceFor(action);
        Variable paramVariable = OrchestrationFactory.eINSTANCE.createVariable();
        paramVariable.setName("param");
        VariableAccess variableAccess = OrchestrationFactory.eINSTANCE.createVariableAccess();
        variableAccess.setReferredVariable(paramVariable);
        actionInstance.getValues().add(variableAccess);
        // Register the variable in the context to allow its access
        JarvisSession session = new JarvisSession("sessionID");
        List<String> listParam = new ArrayList<>();
        listParam.add("test");
        session.getJarvisContext().setContextValue("variables", "param", CompletableFuture.completedFuture(listParam));
        JarvisAction jarvisAction = module.createJarvisAction(actionInstance, session);
        assertThat(jarvisAction).as("Created JarvisAction type is valid").isInstanceOf
                (StubJarvisActionTwoConstructors.class);
        StubJarvisActionTwoConstructors jarvisActionTwoConstructors = (StubJarvisActionTwoConstructors) jarvisAction;
        softly.assertThat(jarvisActionTwoConstructors.getListParam()).as("Constructor2 called").isNotNull();
        softly.assertThat(jarvisActionTwoConstructors.getListParam()).as("List parameter set").contains("test");
        softly.assertThat(jarvisActionTwoConstructors.getParam()).as("Constructor1 not called").isNull();
    }

    @Test(expected = JarvisException.class)
    public void createJarvisParameterActionConstructor1ValidActionInstanceVariableNotRegistered() {
        Action action = getParameterAction();
        ActionInstance actionInstance = createActionInstanceFor(action);
        Variable paramVariable = OrchestrationFactory.eINSTANCE.createVariable();
        paramVariable.setName("param");
        VariableAccess variableAccess = OrchestrationFactory.eINSTANCE.createVariableAccess();
        variableAccess.setReferredVariable(paramVariable);
        actionInstance.getValues().add(variableAccess);
        List<String> listParam = new ArrayList<>();
        listParam.add("test");
        JarvisAction jarvisAction = module.createJarvisAction(actionInstance, new JarvisSession("sessionID"));
    }

    @Test(expected = JarvisException.class)
    public void createJarvisParameterActionValidActionInstanceInvalidParameterType() {
        Action action = getParameterAction();
        ActionInstance actionInstance = createActionInstanceFor(action);
        Variable paramVariable = OrchestrationFactory.eINSTANCE.createVariable();
        paramVariable.setName("param");
        VariableAccess variableAccess = OrchestrationFactory.eINSTANCE.createVariableAccess();
        variableAccess.setReferredVariable(paramVariable);
        actionInstance.getValues().add(variableAccess);
        // Register the variable in the context to allow its access
        JarvisSession session = new JarvisSession("sessionID");
        // Register an integer in the context, there is no constructor to handle it
        session.getJarvisContext().setContextValue("variables", "param", CompletableFuture.completedFuture(1));
        module.createJarvisAction(actionInstance, session);
    }

    @Test(expected = JarvisException.class)
    public void createJarvisParameterActionTooManyParametersInActionInstance() {
        Action action = getParameterAction();
        Parameter param2 = ModuleFactory.eINSTANCE.createParameter();
        param2.setKey("param2");
        param2.setType("List"); // not used, see #120
        action.getParameters().add(param2);
        ActionInstance actionInstance = createActionInstanceFor(action);
        Variable paramVariable = OrchestrationFactory.eINSTANCE.createVariable();
        paramVariable.setName("param");
        VariableAccess variableAccess = OrchestrationFactory.eINSTANCE.createVariableAccess();
        variableAccess.setReferredVariable(paramVariable);
        Variable paramVariable2 = OrchestrationFactory.eINSTANCE.createVariable();
        paramVariable2.setName("param2");
        VariableAccess variableAccess2 = OrchestrationFactory.eINSTANCE.createVariableAccess();
        variableAccess2.setReferredVariable(paramVariable2);
        actionInstance.getValues().add(variableAccess);
        actionInstance.getValues().add(variableAccess2);
        // Register the variable in the context to allow its access
        JarvisSession session = new JarvisSession("sessionID");
        List<String> listParam = new ArrayList<>();
        listParam.add("test");
        session.getJarvisContext().setContextValue("variables", "param", CompletableFuture.completedFuture(listParam));
        session.getJarvisContext().setContextValue("variables", "param2", CompletableFuture.completedFuture(listParam));
        module.createJarvisAction(actionInstance, session);
    }

    @Test
    public void shutdownRegisteredEventProviderAndAction() {
        EventProviderDefinition eventProviderDefinition = ModuleFactory.eINSTANCE.createEventProviderDefinition();
        eventProviderDefinition.setName("StubInputProvider");
        module.startEventProvider(eventProviderDefinition, jarvisCore);
        Action action = getNoParameterAction();
        // Enables the action in the JarvisModule
        ActionInstance actionInstance = createActionInstanceFor(action);
        module.shutdown();
        assertThat(module.getActions()).as("Empty Action map").isEmpty();
        assertThat(module.getEventProviderMap()).as("Empty EventProvider map").isEmpty();
    }

    private Action getNoParameterAction() {
        Action action = ModuleFactory.eINSTANCE.createAction();
        action.setName("StubJarvisActionNoParameter");
        return action;
    }

    private Action getParameterAction() {
        Action action = ModuleFactory.eINSTANCE.createAction();
        action.setName("StubJarvisActionTwoConstructors");
        Parameter param = ModuleFactory.eINSTANCE.createParameter();
        param.setKey("param");
        param.setType("List"); // not used, see #120
        action.getParameters().add(param);
        return action;
    }

    private ActionInstance createActionInstanceFor(Action action) {
        ActionInstance instance = OrchestrationFactory.eINSTANCE.createActionInstance();
        instance.setAction(action);
        module.enableAction(action);
        return instance;
    }

    private Action getNotRegisteredAction() {
        Action action = ModuleFactory.eINSTANCE.createAction();
        action.setName("NotRegisteredAction");
        return action;
    }
}
