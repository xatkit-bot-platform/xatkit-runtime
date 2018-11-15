package fr.zelus.jarvis.core;

import fr.zelus.jarvis.AbstractJarvisTest;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.io.WebhookEventProvider;
import fr.zelus.jarvis.orchestration.*;
import fr.zelus.jarvis.platform.Action;
import fr.zelus.jarvis.platform.EventProviderDefinition;
import fr.zelus.jarvis.platform.Parameter;
import fr.zelus.jarvis.platform.PlatformFactory;
import fr.zelus.jarvis.server.JarvisServer;
import fr.zelus.jarvis.stubs.EmptyRuntimePlatform;
import fr.zelus.jarvis.stubs.StubJarvisCore;
import fr.zelus.jarvis.stubs.action.StubRuntimeActionNoParameter;
import fr.zelus.jarvis.stubs.action.StubRuntimeActionTwoConstructors;
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

public class RuntimePlatformTest extends AbstractJarvisTest {

    private RuntimePlatform runtimePlatform;

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
        runtimePlatform = new EmptyRuntimePlatform(jarvisCore);
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
        if(nonNull(runtimePlatform)) {
            runtimePlatform.shutdown();
        }
    }

    @Test
    public void getName() {
        assertThat(runtimePlatform.getName()).as("Valid runtimePlatform name").isEqualTo("EmptyRuntimePlatform");
    }

    @Test
    public void getJarvisCore() {
        assertThat(runtimePlatform.getJarvisCore()).as("Not null JarvisCore").isNotNull();
        assertThat(runtimePlatform.getJarvisCore()).as("Valid JarvisCore").isEqualTo(jarvisCore);
    }

    @Test(expected = NullPointerException.class)
    public void startEventProviderNullEventProviderDefinition() {
        runtimePlatform.startEventProvider(null);
    }

    @Test(expected = JarvisException.class)
    public void startEventProviderInvalidName() {
        EventProviderDefinition eventProviderDefinition = PlatformFactory.eINSTANCE.createEventProviderDefinition();
        eventProviderDefinition.setName("Test");
        runtimePlatform.startEventProvider(eventProviderDefinition);
    }

    @Test
    public void startValidEventProviderNotWebhook() {
        EventProviderDefinition eventProviderDefinition = PlatformFactory.eINSTANCE.createEventProviderDefinition();
        eventProviderDefinition.setName("StubInputProvider");
        runtimePlatform.startEventProvider(eventProviderDefinition);
        assertThat(runtimePlatform.getEventProviderMap()).as("Not empty EventProvider map").isNotEmpty();
        assertThat(runtimePlatform.getEventProviderMap().get(eventProviderDefinition.getName())).as("Valid EventProvider map " +
                "entry").isNotNull();
        RuntimePlatform.EventProviderThread eventProviderThread = runtimePlatform.getEventProviderMap().get
                (eventProviderDefinition.getName());
        softly.assertThat(eventProviderThread.getEventProvider()).as("EventProvider in thread is valid").isInstanceOf
                (StubInputProvider.class);
        softly.assertThat(eventProviderThread.isAlive()).as("EventProvider thread is alive").isTrue();
        assertThat(jarvisCore.getJarvisServer().getRegisteredWebhookEventProviders()).as("Empty registered webhook " +
                "providers in JarvisServer").isEmpty();
    }

    @Test
    public void startValidEventProviderNotWebhookNoConfigurationConstructor() {
        EventProviderDefinition eventProviderDefinition = PlatformFactory.eINSTANCE.createEventProviderDefinition();
        eventProviderDefinition.setName("StubInputProviderNoConfigurationConstructor");
        runtimePlatform.startEventProvider(eventProviderDefinition);
        assertThat(runtimePlatform.getEventProviderMap()).as("Not empty EventProvider map").isNotEmpty();
        assertThat(runtimePlatform.getEventProviderMap().get(eventProviderDefinition.getName())).as("Valid EventProvider map " +
                "entry").isNotNull();
        RuntimePlatform.EventProviderThread eventProviderThread = runtimePlatform.getEventProviderMap().get
                (eventProviderDefinition.getName());
        softly.assertThat(eventProviderThread.getEventProvider()).as("EventProvider in thread is valid").isInstanceOf
                (StubInputProviderNoConfigurationConstructor.class);
        softly.assertThat(eventProviderThread.isAlive()).as("EventProvider thread is alive").isTrue();
        assertThat(jarvisCore.getJarvisServer().getRegisteredWebhookEventProviders()).as("Empty registered webhook " +
                "providers in JarvisServer").isEmpty();
    }

    @Test
    public void startValidEventProviderWebhook() {
        EventProviderDefinition eventProviderDefinition = PlatformFactory.eINSTANCE.createEventProviderDefinition();
        eventProviderDefinition.setName("StubJsonWebhookEventProvider");
        runtimePlatform.startEventProvider(eventProviderDefinition);
        assertThat(runtimePlatform.getEventProviderMap()).as("Not empty EventProvider map").isNotEmpty();
        assertThat(runtimePlatform.getEventProviderMap().get(eventProviderDefinition.getName())).as("Valid EventProvider map " +
                "entry").isNotNull();
        RuntimePlatform.EventProviderThread eventProviderThread = runtimePlatform.getEventProviderMap().get
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
    public void enableActionNotPlatformAction() {
        Action action = getNotRegisteredAction();
        runtimePlatform.enableAction(action);
    }

    @Test
    public void enableActionPlatformAction() {
        Action infoAction = getNoParameterAction();
        runtimePlatform.enableAction(infoAction);
        assertThat(runtimePlatform.getActions()).as("Action map contains the enabled action").contains
                (StubRuntimeActionNoParameter.class);
    }

    @Test
    public void disableActionNotPlatformAction() {
        Action action = getNotRegisteredAction();
        runtimePlatform.disableAction(action);
    }

    @Test
    public void disableActionPlatformAction() {
        Action action = getNoParameterAction();
        runtimePlatform.enableAction(action);
        runtimePlatform.disableAction(action);
        assertThat(runtimePlatform.getActions()).as("The action map does not contain the unregistered action").doesNotContain
                (StubRuntimeActionNoParameter.class);
    }

    @Test
    public void disableAllActions() {
        Action action1 = getNoParameterAction();
        Action action2 = getParameterAction();
        runtimePlatform.enableAction(action1);
        runtimePlatform.enableAction(action2);
        runtimePlatform.disableAllActions();
        assertThat(runtimePlatform.getActions()).as("The action map is empty").isEmpty();
    }

    @Test(expected = NullPointerException.class)
    public void createRuntimeActionNullActionInstance() {
        runtimePlatform.createRuntimeAction(null, new JarvisSession("id"));
    }

    @Test(expected = JarvisException.class)
    public void createRuntimeActionNotEnabledAction() {
        Action action = getNoParameterAction();
        ActionInstance actionInstance = OrchestrationFactory.eINSTANCE.createActionInstance();
        actionInstance.setAction(action);
        runtimePlatform.createRuntimeAction(actionInstance, new JarvisSession("id"));
    }

    @Test
    public void createRuntimeActionValidActionInstanceNoParameters() {
        Action action = getNoParameterAction();
        ActionInstance actionInstance = createActionInstanceFor(action);
        RuntimeAction runtimeAction = runtimePlatform.createRuntimeAction(actionInstance, new JarvisSession("sessionID"));
        assertThat(runtimeAction).as("Created RuntimeAction type is valid").isInstanceOf(StubRuntimeActionNoParameter
                .class);
    }

    @Test
    public void createRuntimeActionValidActionInstanceWithReturnType() {
        Action action = getNoParameterAction();
        ActionInstance actionInstance = createActionInstanceFor(action);
        Variable returnVariable = OrchestrationFactory.eINSTANCE.createVariable();
        returnVariable.setName("return");
        VariableAccess returnVariableAccess = OrchestrationFactory.eINSTANCE.createVariableAccess();
        returnVariableAccess.setReferredVariable(returnVariable);
        actionInstance.setReturnVariable(returnVariableAccess);
        RuntimeAction runtimeAction = runtimePlatform.createRuntimeAction(actionInstance, new JarvisSession("session"));
        assertThat(runtimeAction.getReturnVariable()).as("Valid return variable name").isEqualTo(returnVariable
                .getName());
    }

    @Test(expected = JarvisException.class)
    public void createRuntimeActionTooManyParametersInAction() {
        Action action = getNoParameterAction();
        ActionInstance actionInstance = createActionInstanceFor(action);
        Parameter param = PlatformFactory.eINSTANCE.createParameter();
        param.setKey("myParam");
        action.getParameters().add(param);
        ParameterValue parameterValue = OrchestrationFactory.eINSTANCE.createParameterValue();
        StringLiteral value = OrchestrationFactory.eINSTANCE.createStringLiteral();
        parameterValue.setExpression(value);
//        parameterValue.setParameter(param);
        value.setValue("myValue");
        actionInstance.getValues().add(parameterValue);
        runtimePlatform.createRuntimeAction(actionInstance, new JarvisSession("sessionID"));
    }

    @Test(expected = JarvisException.class)
    public void createRuntimeActionTooManyParametersInActionInstance() {
        Action action = getNoParameterAction();
        ActionInstance actionInstance = createActionInstanceFor(action);
        Parameter param = PlatformFactory.eINSTANCE.createParameter();
        param.setKey("myParam");
        // Do not attach the Parameter to the Action
        ParameterValue parameterValue = OrchestrationFactory.eINSTANCE.createParameterValue();
        StringLiteral value = OrchestrationFactory.eINSTANCE.createStringLiteral();
        parameterValue.setExpression(value);
//        parameterValue.setParameter(param);
        value.setValue("myValue");
        actionInstance.getValues().add(parameterValue);
        runtimePlatform.createRuntimeAction(actionInstance, new JarvisSession("sessionID"));
    }

    @Test
    public void createJarvisParameterActionConstructor1ValidActionInstanceVariableAccess() {
        Action action = getParameterAction();
        ActionInstance actionInstance = createActionInstanceFor(action);
        Variable paramVariable = OrchestrationFactory.eINSTANCE.createVariable();
        paramVariable.setName("param");
        VariableAccess variableAccess = OrchestrationFactory.eINSTANCE.createVariableAccess();
        variableAccess.setReferredVariable(paramVariable);
        ParameterValue parameterValue = OrchestrationFactory.eINSTANCE.createParameterValue();
        parameterValue.setExpression(variableAccess);
        actionInstance.getValues().add(parameterValue);
        // Register the variable in the context to allow its access
        JarvisSession session = new JarvisSession("sessionID");
        session.getJarvisContext().setContextValue("variables", 5, "param", CompletableFuture.completedFuture("test"));
        RuntimeAction runtimeAction = runtimePlatform.createRuntimeAction(actionInstance, session);
        assertThat(runtimeAction).as("Created RuntimeAction type is valid").isInstanceOf
                (StubRuntimeActionTwoConstructors.class);
        StubRuntimeActionTwoConstructors runtimeActionTwoConstructors = (StubRuntimeActionTwoConstructors)
                runtimeAction;
        softly.assertThat(runtimeActionTwoConstructors.getParam()).as("Constructor1 called").isEqualTo("test");
        softly.assertThat(runtimeActionTwoConstructors.getListParam()).as("Constructor2 not called").isNull();
    }

    @Test
    public void createJarvisParameterActionConstructor2ValidActionInstanceVariableAccess() {
        Action action = getParameterAction();
        ActionInstance actionInstance = createActionInstanceFor(action);
        Variable paramVariable = OrchestrationFactory.eINSTANCE.createVariable();
        paramVariable.setName("param");
        VariableAccess variableAccess = OrchestrationFactory.eINSTANCE.createVariableAccess();
        variableAccess.setReferredVariable(paramVariable);
        ParameterValue parameterValue = OrchestrationFactory.eINSTANCE.createParameterValue();
        parameterValue.setExpression(variableAccess);
        actionInstance.getValues().add(parameterValue);
        // Register the variable in the context to allow its access
        JarvisSession session = new JarvisSession("sessionID");
        List<String> listParam = new ArrayList<>();
        listParam.add("test");
        session.getJarvisContext().setContextValue("variables", 5, "param", CompletableFuture.completedFuture
                (listParam));
        RuntimeAction runtimeAction = runtimePlatform.createRuntimeAction(actionInstance, session);
        assertThat(runtimeAction).as("Created RuntimeAction type is valid").isInstanceOf
                (StubRuntimeActionTwoConstructors.class);
        StubRuntimeActionTwoConstructors runtimeActionTwoConstructors = (StubRuntimeActionTwoConstructors) runtimeAction;
        softly.assertThat(runtimeActionTwoConstructors.getListParam()).as("Constructor2 called").isNotNull();
        softly.assertThat(runtimeActionTwoConstructors.getListParam()).as("List parameter set").contains("test");
        softly.assertThat(runtimeActionTwoConstructors.getParam()).as("Constructor1 not called").isNull();
    }

    @Test(expected = JarvisException.class)
    public void createJarvisParameterActionConstructor1ValidActionInstanceVariableNotRegistered() {
        Action action = getParameterAction();
        ActionInstance actionInstance = createActionInstanceFor(action);
        Variable paramVariable = OrchestrationFactory.eINSTANCE.createVariable();
        paramVariable.setName("param");
        VariableAccess variableAccess = OrchestrationFactory.eINSTANCE.createVariableAccess();
        variableAccess.setReferredVariable(paramVariable);
        ParameterValue parameterValue = OrchestrationFactory.eINSTANCE.createParameterValue();
        parameterValue.setExpression(variableAccess);
        actionInstance.getValues().add(parameterValue);
        List<String> listParam = new ArrayList<>();
        listParam.add("test");
        RuntimeAction runtimeAction = runtimePlatform.createRuntimeAction(actionInstance, new JarvisSession("sessionID"));
    }

    @Test(expected = JarvisException.class)
    public void createJarvisParameterActionValidActionInstanceInvalidParameterType() {
        Action action = getParameterAction();
        ActionInstance actionInstance = createActionInstanceFor(action);
        Variable paramVariable = OrchestrationFactory.eINSTANCE.createVariable();
        paramVariable.setName("param");
        VariableAccess variableAccess = OrchestrationFactory.eINSTANCE.createVariableAccess();
        variableAccess.setReferredVariable(paramVariable);
        ParameterValue parameterValue = OrchestrationFactory.eINSTANCE.createParameterValue();
        parameterValue.setExpression(variableAccess);
        actionInstance.getValues().add(parameterValue);
        // Register the variable in the context to allow its access
        JarvisSession session = new JarvisSession("sessionID");
        // Register an integer in the context, there is no constructor to handle it
        session.getJarvisContext().setContextValue("variables", 5, "param", CompletableFuture.completedFuture(1));
        runtimePlatform.createRuntimeAction(actionInstance, session);
    }

    @Test(expected = JarvisException.class)
    public void createJarvisParameterActionTooManyParametersInActionInstance() {
        Action action = getParameterAction();
        Parameter param2 = PlatformFactory.eINSTANCE.createParameter();
        param2.setKey("param2");
        action.getParameters().add(param2);
        ActionInstance actionInstance = createActionInstanceFor(action);
        Variable paramVariable = OrchestrationFactory.eINSTANCE.createVariable();
        paramVariable.setName("param");
        VariableAccess variableAccess = OrchestrationFactory.eINSTANCE.createVariableAccess();
        variableAccess.setReferredVariable(paramVariable);
        ParameterValue parameterValue1 = OrchestrationFactory.eINSTANCE.createParameterValue();
        parameterValue1.setExpression(variableAccess);
        Variable paramVariable2 = OrchestrationFactory.eINSTANCE.createVariable();
        paramVariable2.setName("param2");
        VariableAccess variableAccess2 = OrchestrationFactory.eINSTANCE.createVariableAccess();
        variableAccess2.setReferredVariable(paramVariable2);
        ParameterValue parameterValue2 = OrchestrationFactory.eINSTANCE.createParameterValue();
        parameterValue2.setExpression(variableAccess2);
        actionInstance.getValues().add(parameterValue1);
        actionInstance.getValues().add(parameterValue2);
        // Register the variable in the context to allow its access
        JarvisSession session = new JarvisSession("sessionID");
        List<String> listParam = new ArrayList<>();
        listParam.add("test");
        session.getJarvisContext().setContextValue("variables", 5, "param", CompletableFuture.completedFuture
                (listParam));
        session.getJarvisContext().setContextValue("variables", 5, "param2", CompletableFuture.completedFuture
                (listParam));
        runtimePlatform.createRuntimeAction(actionInstance, session);
    }

    @Test
    public void shutdownRegisteredEventProviderAndAction() {
        EventProviderDefinition eventProviderDefinition = PlatformFactory.eINSTANCE.createEventProviderDefinition();
        eventProviderDefinition.setName("StubInputProvider");
        runtimePlatform.startEventProvider(eventProviderDefinition);
        Action action = getNoParameterAction();
        // Enables the action in the RuntimePlatform
        ActionInstance actionInstance = createActionInstanceFor(action);
        runtimePlatform.shutdown();
        assertThat(runtimePlatform.getActions()).as("Empty Action map").isEmpty();
        assertThat(runtimePlatform.getEventProviderMap()).as("Empty EventProvider map").isEmpty();
    }

    private Action getNoParameterAction() {
        Action action = PlatformFactory.eINSTANCE.createAction();
        action.setName("StubRuntimeActionNoParameter");
        return action;
    }

    private Action getParameterAction() {
        Action action = PlatformFactory.eINSTANCE.createAction();
        action.setName("StubRuntimeActionTwoConstructors");
        Parameter param = PlatformFactory.eINSTANCE.createParameter();
        param.setKey("param");
        action.getParameters().add(param);
        return action;
    }

    private ActionInstance createActionInstanceFor(Action action) {
        ActionInstance instance = OrchestrationFactory.eINSTANCE.createActionInstance();
        instance.setAction(action);
        runtimePlatform.enableAction(action);
        return instance;
    }

    private Action getNotRegisteredAction() {
        Action action = PlatformFactory.eINSTANCE.createAction();
        action.setName("NotRegisteredAction");
        return action;
    }
}
