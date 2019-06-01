package com.xatkit.core.platform;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.common.CommonFactory;
import com.xatkit.common.StringLiteral;
import com.xatkit.common.VariableAccess;
import com.xatkit.common.VariableDeclaration;
import com.xatkit.core.XatkitCore;
import com.xatkit.core.XatkitException;
import com.xatkit.core.interpreter.ExecutionContext;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.platform.io.WebhookEventProvider;
import com.xatkit.core.server.XatkitServer;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.execution.ActionInstance;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.ParameterValue;
import com.xatkit.platform.ActionDefinition;
import com.xatkit.platform.EventProviderDefinition;
import com.xatkit.platform.Parameter;
import com.xatkit.platform.PlatformFactory;
import com.xatkit.stubs.EmptyRuntimePlatform;
import com.xatkit.stubs.StubXatkitCore;
import com.xatkit.stubs.action.StubRuntimeActionNoParameter;
import com.xatkit.stubs.action.StubRuntimeActionTwoConstructors;
import com.xatkit.stubs.io.StubInputProvider;
import com.xatkit.stubs.io.StubInputProviderNoConfigurationConstructor;
import com.xatkit.stubs.io.StubJsonWebhookEventProvider;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class RuntimePlatformTest extends AbstractXatkitTest {

    private static ExecutionFactory executionFactory = ExecutionFactory.eINSTANCE;

    private static CommonFactory commonFactory = CommonFactory.eINSTANCE;

    private RuntimePlatform runtimePlatform;

    private static XatkitCore xatkitCore;

    private ExecutionContext executionContext;

    @BeforeClass
    public static void setUpBeforeClass() {
        xatkitCore = new StubXatkitCore();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        if (nonNull(xatkitCore)) {
            xatkitCore.shutdown();
        }
    }

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Before
    public void setUp() {
        runtimePlatform = new EmptyRuntimePlatform(xatkitCore);
        if (nonNull(xatkitCore)) {
            /*
             * Unregister the WebhookEventProviders that may have been set as side effect of startEventProvider
             */
            XatkitServer xatkitServer = xatkitCore.getXatkitServer();
            for (WebhookEventProvider eventProvider : xatkitServer.getRegisteredWebhookEventProviders()) {
                xatkitServer.unregisterWebhookEventProvider(eventProvider);
            }
        }
        if (!xatkitCore.getXatkitServer().getRegisteredWebhookEventProviders().isEmpty()) {

        }
        /*
         * An empty execution context (not tested here)
         */
        executionContext = new ExecutionContext();
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
    public void getXatkitCore() {
        Assertions.assertThat(runtimePlatform.getXatkitCore()).as("Not null XatkitCore").isNotNull();
        Assertions.assertThat(runtimePlatform.getXatkitCore()).as("Valid XatkitCore").isEqualTo(xatkitCore);
    }

    @Test(expected = NullPointerException.class)
    public void startEventProviderNullEventProviderDefinition() {
        runtimePlatform.startEventProvider(null);
    }

    @Test(expected = XatkitException.class)
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
        assertThat(runtimePlatform.getEventProviderMap()).as("Not empty RuntimeEventProvider map").isNotEmpty();
        assertThat(runtimePlatform.getEventProviderMap().get(eventProviderDefinition.getName())).as("Valid RuntimeEventProvider map " +
                "entry").isNotNull();
        RuntimePlatform.EventProviderThread eventProviderThread = runtimePlatform.getEventProviderMap().get
                (eventProviderDefinition.getName());
        softly.assertThat(eventProviderThread.getRuntimeEventProvider()).as("RuntimeEventProvider in thread is valid").isInstanceOf
                (StubInputProvider.class);
        softly.assertThat(eventProviderThread.isAlive()).as("RuntimeEventProvider thread is alive").isTrue();
        assertThat(xatkitCore.getXatkitServer().getRegisteredWebhookEventProviders()).as("Empty registered webhook " +
                "providers in XatkitServer").isEmpty();
    }

    @Test
    public void startValidEventProviderNotWebhookNoConfigurationConstructor() {
        EventProviderDefinition eventProviderDefinition = PlatformFactory.eINSTANCE.createEventProviderDefinition();
        eventProviderDefinition.setName("StubInputProviderNoConfigurationConstructor");
        runtimePlatform.startEventProvider(eventProviderDefinition);
        assertThat(runtimePlatform.getEventProviderMap()).as("Not empty RuntimeEventProvider map").isNotEmpty();
        assertThat(runtimePlatform.getEventProviderMap().get(eventProviderDefinition.getName())).as("Valid RuntimeEventProvider map " +
                "entry").isNotNull();
        RuntimePlatform.EventProviderThread eventProviderThread = runtimePlatform.getEventProviderMap().get
                (eventProviderDefinition.getName());
        softly.assertThat(eventProviderThread.getRuntimeEventProvider()).as("RuntimeEventProvider in thread is valid").isInstanceOf
                (StubInputProviderNoConfigurationConstructor.class);
        softly.assertThat(eventProviderThread.isAlive()).as("RuntimeEventProvider thread is alive").isTrue();
        assertThat(xatkitCore.getXatkitServer().getRegisteredWebhookEventProviders()).as("Empty registered webhook " +
                "providers in XatkitServer").isEmpty();
    }

    @Test
    public void startValidEventProviderWebhook() {
        EventProviderDefinition eventProviderDefinition = PlatformFactory.eINSTANCE.createEventProviderDefinition();
        eventProviderDefinition.setName("StubJsonWebhookEventProvider");
        runtimePlatform.startEventProvider(eventProviderDefinition);
        assertThat(runtimePlatform.getEventProviderMap()).as("Not empty RuntimeEventProvider map").isNotEmpty();
        assertThat(runtimePlatform.getEventProviderMap().get(eventProviderDefinition.getName())).as("Valid RuntimeEventProvider map " +
                "entry").isNotNull();
        RuntimePlatform.EventProviderThread eventProviderThread = runtimePlatform.getEventProviderMap().get
                (eventProviderDefinition.getName());
        softly.assertThat(eventProviderThread.getRuntimeEventProvider()).as("RuntimeEventProvider in thread is valid").isInstanceOf
                (StubJsonWebhookEventProvider.class);
        softly.assertThat(eventProviderThread.isAlive()).as("RuntimeEventProvider thread is alive").isTrue();
        assertThat(xatkitCore.getXatkitServer().getRegisteredWebhookEventProviders()).as("Webhook provider " +
                "registered" + "in XatkitServer").hasSize(1);
        softly.assertThat(xatkitCore.getXatkitServer().getRegisteredWebhookEventProviders().iterator().next()).as
                ("Valid Webhook registered in XatkitServer").isInstanceOf(StubJsonWebhookEventProvider.class);
    }

    @Test(expected = XatkitException.class)
    public void enableActionNotPlatformAction() {
        ActionDefinition action = getNotRegisteredActionDefinition();
        runtimePlatform.enableAction(action);
    }

    @Test
    public void enableActionPlatformAction() {
        ActionDefinition infoActionDefinition = getNoParameterActionDefinition();
        runtimePlatform.enableAction(infoActionDefinition);
        assertThat(runtimePlatform.getActions()).as("Action map contains the enabled ActionDefinition").contains
                (StubRuntimeActionNoParameter.class);
    }

    @Test
    public void disableActionNotPlatformAction() {
        ActionDefinition actionDefinition = getNotRegisteredActionDefinition();
        runtimePlatform.disableAction(actionDefinition);
    }

    @Test
    public void disableActionPlatformAction() {
        ActionDefinition actionDefinition = getNoParameterActionDefinition();
        runtimePlatform.enableAction(actionDefinition);
        runtimePlatform.disableAction(actionDefinition);
        assertThat(runtimePlatform.getActions()).as("The actionDefinition map does not contain the unregistered " +
                "ActionDefinition").doesNotContain(StubRuntimeActionNoParameter.class);
    }

    @Test
    public void disableAllActions() {
        ActionDefinition actionDefinition1 = getNoParameterActionDefinition();
        ActionDefinition actionDefinition2 = getParameterActionDefinition();
        runtimePlatform.enableAction(actionDefinition1);
        runtimePlatform.enableAction(actionDefinition2);
        runtimePlatform.disableAllActions();
        assertThat(runtimePlatform.getActions()).as("The action map is empty").isEmpty();
    }

    @Test(expected = NullPointerException.class)
    public void createRuntimeActionNullActionInstance() {
        runtimePlatform.createRuntimeAction(null, new XatkitSession("id"), executionContext);
    }

    @Test(expected = XatkitException.class)
    public void createRuntimeActionNotEnabledAction() {
        ActionDefinition actionDefinition = getNoParameterActionDefinition();
        ActionInstance actionInstance = executionFactory.createActionInstance();
        actionInstance.setAction(actionDefinition);
        runtimePlatform.createRuntimeAction(actionInstance, new XatkitSession("id"), executionContext);
    }

    @Test
    public void createRuntimeActionValidActionInstanceNoParameters() {
        ActionDefinition actionDefinition = getNoParameterActionDefinition();
        ActionInstance actionInstance = createActionInstanceFor(actionDefinition);
        RuntimeAction runtimeAction = runtimePlatform.createRuntimeAction(actionInstance, new XatkitSession("sessionID"), executionContext);
        assertThat(runtimeAction).as("Created RuntimeAction type is valid").isInstanceOf(StubRuntimeActionNoParameter
                .class);
    }

    @Test(expected = XatkitException.class)
    public void createRuntimeActionTooManyParametersInAction() {
        ActionDefinition actionDefinition = getNoParameterActionDefinition();
        ActionInstance actionInstance = createActionInstanceFor(actionDefinition);
        Parameter param = PlatformFactory.eINSTANCE.createParameter();
        param.setKey("myParam");
        actionDefinition.getParameters().add(param);
        ParameterValue parameterValue = executionFactory.createParameterValue();
        StringLiteral value = commonFactory.createStringLiteral();
        parameterValue.setExpression(value);
//        parameterValue.setParameter(param);
        value.setValue("myValue");
        actionInstance.getValues().add(parameterValue);
        runtimePlatform.createRuntimeAction(actionInstance, new XatkitSession("sessionID"), executionContext);
    }

    @Test(expected = XatkitException.class)
    public void createRuntimeActionTooManyParametersInActionInstance() {
        ActionDefinition actionDefinition = getNoParameterActionDefinition();
        ActionInstance actionInstance = createActionInstanceFor(actionDefinition);
        Parameter param = PlatformFactory.eINSTANCE.createParameter();
        param.setKey("myParam");
        // Do not attach the Parameter to the Action
        ParameterValue parameterValue = executionFactory.createParameterValue();
        StringLiteral value = commonFactory.createStringLiteral();
        parameterValue.setExpression(value);
//        parameterValue.setParameter(param);
        value.setValue("myValue");
        actionInstance.getValues().add(parameterValue);
        runtimePlatform.createRuntimeAction(actionInstance, new XatkitSession("sessionID"), executionContext);
    }

    @Test
    public void createRuntimeParameterActionConstructor1ValidActionInstanceVariableAccess() {
        ActionDefinition actionDefinition = getParameterActionDefinition();
        ActionInstance actionInstance = createActionInstanceFor(actionDefinition);
        VariableDeclaration paramVariable = commonFactory.createVariableDeclaration();
        paramVariable.setName("param");
        StringLiteral paramValue = commonFactory.createStringLiteral();
//        paramValue.setValue("test");
        paramVariable.setValue(paramValue);
        VariableAccess variableAccess = commonFactory.createVariableAccess();
        variableAccess.setReferredVariable(paramVariable);
        ParameterValue parameterValue = executionFactory.createParameterValue();
        parameterValue.setExpression(variableAccess);
        actionInstance.getValues().add(parameterValue);
        // Register the variable in the context to allow its access
        XatkitSession session = new XatkitSession("sessionID");
//        session.getRuntimeContexts().setContextValue("variables", 5, "param", CompletableFuture.completedFuture("test"));
        executionContext.setValue(paramVariable.getName(), "test");

        RuntimeAction runtimeAction = runtimePlatform.createRuntimeAction(actionInstance, session, executionContext);
        assertThat(runtimeAction).as("Created RuntimeAction type is valid").isInstanceOf
                (StubRuntimeActionTwoConstructors.class);
        StubRuntimeActionTwoConstructors runtimeActionTwoConstructors = (StubRuntimeActionTwoConstructors)
                runtimeAction;
        softly.assertThat(runtimeActionTwoConstructors.getParam()).as("Constructor1 called").isEqualTo("test");
        softly.assertThat(runtimeActionTwoConstructors.getListParam()).as("Constructor2 not called").isNull();
    }

    @Test
    public void createRuntimeParameterActionConstructor2ValidActionInstanceVariableAccess() {
        ActionDefinition actionDefinition = getParameterActionDefinition();
        ActionInstance actionInstance = createActionInstanceFor(actionDefinition);
        VariableDeclaration paramVariable = commonFactory.createVariableDeclaration();
        paramVariable.setName("param");
        VariableAccess variableAccess = commonFactory.createVariableAccess();
        variableAccess.setReferredVariable(paramVariable);
        ParameterValue parameterValue = executionFactory.createParameterValue();
        parameterValue.setExpression(variableAccess);
        actionInstance.getValues().add(parameterValue);
        /*
         * Register manually the value in the execution context, this is handled by the ExecutionService.
         */
        executionContext.setValue("param", Collections.singletonList("test"));

        RuntimeAction runtimeAction = runtimePlatform.createRuntimeAction(actionInstance, new XatkitSession(
                "sessionID"),
                executionContext);
        assertThat(runtimeAction).as("Created RuntimeAction type is valid").isInstanceOf
                (StubRuntimeActionTwoConstructors.class);
        StubRuntimeActionTwoConstructors runtimeActionTwoConstructors = (StubRuntimeActionTwoConstructors) runtimeAction;
        softly.assertThat(runtimeActionTwoConstructors.getListParam()).as("Constructor2 called").isNotNull();
        softly.assertThat(runtimeActionTwoConstructors.getListParam()).as("List parameter set").contains("test");
        softly.assertThat(runtimeActionTwoConstructors.getParam()).as("Constructor1 not called").isNull();
    }

    @Test(expected = XatkitException.class)
    public void createRuntimeParameterActionConstructor1ValidActionInstanceVariableNotRegistered() {
        ActionDefinition actionDefinition = getParameterActionDefinition();
        ActionInstance actionInstance = createActionInstanceFor(actionDefinition);
        VariableDeclaration paramVariable = commonFactory.createVariableDeclaration();
        paramVariable.setName("param");
        VariableAccess variableAccess = commonFactory.createVariableAccess();
        variableAccess.setReferredVariable(paramVariable);
        ParameterValue parameterValue = executionFactory.createParameterValue();
        parameterValue.setExpression(variableAccess);
        actionInstance.getValues().add(parameterValue);
        RuntimeAction runtimeAction = runtimePlatform.createRuntimeAction(actionInstance, new XatkitSession("sessionID"), executionContext);
    }

    @Test(expected = XatkitException.class)
    public void createRuntimeParameterActionValidActionInstanceInvalidParameterType() {
        ActionDefinition actionDefinition = getParameterActionDefinition();
        ActionInstance actionInstance = createActionInstanceFor(actionDefinition);
        VariableDeclaration paramVariable = commonFactory.createVariableDeclaration();
        paramVariable.setName("param");
        VariableAccess variableAccess = commonFactory.createVariableAccess();
        variableAccess.setReferredVariable(paramVariable);
        ParameterValue parameterValue = executionFactory.createParameterValue();
        parameterValue.setExpression(variableAccess);
        actionInstance.getValues().add(parameterValue);
        // Register the variable in the context to allow its access
        XatkitSession session = new XatkitSession("sessionID");
        // Register an integer in the context, there is no constructor to handle it
        session.getRuntimeContexts().setContextValue("variables", 5, "param", CompletableFuture.completedFuture(1));
        runtimePlatform.createRuntimeAction(actionInstance, session, executionContext);
    }

    @Test(expected = XatkitException.class)
    public void createRuntimeParameterActionTooManyParametersInActionInstance() {
        ActionDefinition actionDefinition = getParameterActionDefinition();
        Parameter param2 = PlatformFactory.eINSTANCE.createParameter();
        param2.setKey("param2");
        actionDefinition.getParameters().add(param2);
        ActionInstance actionInstance = createActionInstanceFor(actionDefinition);
        VariableDeclaration paramVariable = commonFactory.createVariableDeclaration();
        paramVariable.setName("param");
        VariableAccess variableAccess = commonFactory.createVariableAccess();
        variableAccess.setReferredVariable(paramVariable);
        ParameterValue parameterValue1 = executionFactory.createParameterValue();
        parameterValue1.setExpression(variableAccess);
        VariableDeclaration paramVariable2 = commonFactory.createVariableDeclaration();
        paramVariable2.setName("param2");
        VariableAccess variableAccess2 = commonFactory.createVariableAccess();
        variableAccess2.setReferredVariable(paramVariable2);
        ParameterValue parameterValue2 = executionFactory.createParameterValue();
        parameterValue2.setExpression(variableAccess2);
        actionInstance.getValues().add(parameterValue1);
        actionInstance.getValues().add(parameterValue2);
        // Register the variable in the context to allow its access
        XatkitSession session = new XatkitSession("sessionID");
        List<String> listParam = new ArrayList<>();
        listParam.add("test");
        session.getRuntimeContexts().setContextValue("variables", 5, "param", CompletableFuture.completedFuture
                (listParam));
        session.getRuntimeContexts().setContextValue("variables", 5, "param2", CompletableFuture.completedFuture
                (listParam));
        runtimePlatform.createRuntimeAction(actionInstance, session, executionContext);
    }

    @Test
    public void shutdownRegisteredEventProviderAndActionDefinition() {
        EventProviderDefinition eventProviderDefinition = PlatformFactory.eINSTANCE.createEventProviderDefinition();
        eventProviderDefinition.setName("StubInputProvider");
        runtimePlatform.startEventProvider(eventProviderDefinition);
        ActionDefinition actionDefinition = getNoParameterActionDefinition();
        // Enables the actionDefinition in the RuntimePlatform
        ActionInstance actionInstance = createActionInstanceFor(actionDefinition);
        runtimePlatform.shutdown();
        assertThat(runtimePlatform.getActions()).as("Empty Action map").isEmpty();
        assertThat(runtimePlatform.getEventProviderMap()).as("Empty RuntimeEventProvider map").isEmpty();
    }

    private ActionDefinition getNoParameterActionDefinition() {
        ActionDefinition actionDefinition = PlatformFactory.eINSTANCE.createActionDefinition();
        actionDefinition.setName("StubRuntimeActionNoParameter");
        return actionDefinition;
    }

    private ActionDefinition getParameterActionDefinition() {
        ActionDefinition actionDefinition = PlatformFactory.eINSTANCE.createActionDefinition();
        actionDefinition.setName("StubRuntimeActionTwoConstructors");
        Parameter param = PlatformFactory.eINSTANCE.createParameter();
        param.setKey("param");
        actionDefinition.getParameters().add(param);
        return actionDefinition;
    }

    private ActionInstance createActionInstanceFor(ActionDefinition actionDefinition) {
        ActionInstance instance = executionFactory.createActionInstance();
        instance.setAction(actionDefinition);
        runtimePlatform.enableAction(actionDefinition);
        return instance;
    }

    private ActionDefinition getNotRegisteredActionDefinition() {
        ActionDefinition actionDefinition = PlatformFactory.eINSTANCE.createActionDefinition();
        actionDefinition.setName("NotRegisteredAction");
        return actionDefinition;
    }
}
