package com.xatkit.core.platform;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.common.CommonFactory;
import com.xatkit.core.XatkitCore;
import com.xatkit.core.XatkitException;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.server.XatkitServer;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.platform.ActionDefinition;
import com.xatkit.platform.EventProviderDefinition;
import com.xatkit.platform.Parameter;
import com.xatkit.platform.PlatformDefinition;
import com.xatkit.platform.PlatformFactory;
import com.xatkit.stubs.EmptyRuntimePlatform;
import com.xatkit.stubs.StubXatkitCore;
import com.xatkit.stubs.action.StubRuntimeActionNoParameter;
import com.xatkit.stubs.io.StubInputProvider;
import com.xatkit.stubs.io.StubInputProviderNoConfigurationConstructor;
import com.xatkit.stubs.io.StubJsonWebhookEventProvider;
import com.xatkit.test.util.ElementFactory;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.core.util.Arrays;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class RuntimePlatformTest extends AbstractXatkitTest {

    private static ExecutionFactory executionFactory = ExecutionFactory.eINSTANCE;

    private static CommonFactory commonFactory = CommonFactory.eINSTANCE;

    private RuntimePlatform runtimePlatform;

    private static XatkitCore xatkitCore;

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
            xatkitServer.clearRegisteredRestHandlers();
        }
    }

    @After
    public void tearDown() {
        if (nonNull(runtimePlatform)) {
            runtimePlatform.shutdown();
        }
    }

    @Test
    public void getName() {
        assertThat(runtimePlatform.getName()).as("Valid runtimePlatform name").isEqualTo("EmptyRuntimePlatform");
    }

    @Test
    public void getConfiguration() {
        assertThat(runtimePlatform.getConfiguration()).as("Not null Configuration").isNotNull();
        assertThat(runtimePlatform.getConfiguration().containsKey(XatkitCore.EXECUTION_MODEL_KEY)).as("Configuration contains the " +
                "execution model key").isTrue();
        /*
         * We don't check the instance of the Configuration because we can't get it from XatkitCore.
         * TODO test the constructor and make sure the Configuration is the right one.
         */
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
        assertThat(runtimePlatform.getEventProviderMap().get(eventProviderDefinition.getName())).as("Valid " +
                "RuntimeEventProvider map " +
                "entry").isNotNull();
        RuntimePlatform.EventProviderThread eventProviderThread = runtimePlatform.getEventProviderMap().get
                (eventProviderDefinition.getName());
        softly.assertThat(eventProviderThread.getRuntimeEventProvider()).as("RuntimeEventProvider in thread is valid").isInstanceOf
                (StubInputProvider.class);
        softly.assertThat(eventProviderThread.isAlive()).as("RuntimeEventProvider thread is alive").isTrue();
        assertThat(xatkitCore.getXatkitServer().getRegisteredRestHandlers()).as("Empty registered handlers " +
                "in XatkitServer").isEmpty();
    }

    @Test
    public void startValidEventProviderNotWebhookNoConfigurationConstructor() {
        EventProviderDefinition eventProviderDefinition = PlatformFactory.eINSTANCE.createEventProviderDefinition();
        eventProviderDefinition.setName("StubInputProviderNoConfigurationConstructor");
        runtimePlatform.startEventProvider(eventProviderDefinition);
        assertThat(runtimePlatform.getEventProviderMap()).as("Not empty RuntimeEventProvider map").isNotEmpty();
        assertThat(runtimePlatform.getEventProviderMap().get(eventProviderDefinition.getName())).as("Valid " +
                "RuntimeEventProvider map " +
                "entry").isNotNull();
        RuntimePlatform.EventProviderThread eventProviderThread = runtimePlatform.getEventProviderMap().get
                (eventProviderDefinition.getName());
        softly.assertThat(eventProviderThread.getRuntimeEventProvider()).as("RuntimeEventProvider in thread is valid").isInstanceOf
                (StubInputProviderNoConfigurationConstructor.class);
        softly.assertThat(eventProviderThread.isAlive()).as("RuntimeEventProvider thread is alive").isTrue();
        assertThat(xatkitCore.getXatkitServer().getRegisteredRestHandlers()).as("Empty registered handlers " +
                "in XatkitServer").isEmpty();
    }

    @Test
    public void startValidEventProviderWebhook() {
        EventProviderDefinition eventProviderDefinition = PlatformFactory.eINSTANCE.createEventProviderDefinition();
        eventProviderDefinition.setName("StubJsonWebhookEventProvider");
        runtimePlatform.startEventProvider(eventProviderDefinition);
        assertThat(runtimePlatform.getEventProviderMap()).as("Not empty RuntimeEventProvider map").isNotEmpty();
        assertThat(runtimePlatform.getEventProviderMap().get(eventProviderDefinition.getName())).as("Valid " +
                "RuntimeEventProvider map " +
                "entry").isNotNull();
        RuntimePlatform.EventProviderThread eventProviderThread = runtimePlatform.getEventProviderMap().get
                (eventProviderDefinition.getName());
        softly.assertThat(eventProviderThread.getRuntimeEventProvider()).as("RuntimeEventProvider in thread is valid").isInstanceOf
                (StubJsonWebhookEventProvider.class);
        softly.assertThat(eventProviderThread.isAlive()).as("RuntimeEventProvider thread is alive").isTrue();
        assertThat(xatkitCore.getXatkitServer().getRegisteredRestHandlers()).as("RestHandler provider " +
                "registered" + "in XatkitServer").hasSize(1);
        softly.assertThat(xatkitCore.getXatkitServer().getRegisteredRestHandlers().iterator().next()).as
                ("Valid RestHandler registered in XatkitServer").isEqualTo(StubJsonWebhookEventProvider.handler);
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
        runtimePlatform.createRuntimeAction(null, Collections.emptyList(), new XatkitSession("id"));
    }

    @Test(expected = XatkitException.class)
    public void createRuntimeActionNotEnabledAction() {
        ActionDefinition actionDefinition = getNoParameterActionDefinition();
        XMemberFeatureCall featureCall = ElementFactory.createXMemberFeatureCall(actionDefinition);
        runtimePlatform.createRuntimeAction(featureCall, Collections.emptyList(), new XatkitSession("id"));
    }

    @Test
    public void createRuntimeActionValidActionInstanceNoParameters() {
        ActionDefinition actionDefinition = getNoParameterActionDefinition();
        runtimePlatform.enableAction(actionDefinition);
        XMemberFeatureCall featureCall = ElementFactory.createXMemberFeatureCall(actionDefinition);
        RuntimeAction runtimeAction = runtimePlatform.createRuntimeAction(featureCall, Collections.emptyList(),
                new XatkitSession("sessionID"));
        assertThat(runtimeAction).as("Created RuntimeAction type is valid").isInstanceOf(StubRuntimeActionNoParameter
                .class);
    }

    @Test(expected = XatkitException.class)
    public void createRuntimeActionTooManyArguments() {
        ActionDefinition actionDefinition = getNoParameterActionDefinition();
        runtimePlatform.enableAction(actionDefinition);
        XMemberFeatureCall featureCall = ElementFactory.createXMemberFeatureCall(actionDefinition);
        runtimePlatform.createRuntimeAction(featureCall, Arrays.asList(new String[]{"a", "b", "c"}), new XatkitSession(
                "sessionID"));
    }

    @Test(expected = XatkitException.class)
    public void createRuntimeActionInvalidParameterType() {
        ActionDefinition actionDefinition = getParameterActionDefinition();
        XMemberFeatureCall featureCall = ElementFactory.createXMemberFeatureCall(actionDefinition);
        runtimePlatform.createRuntimeAction(featureCall, Arrays.asList(new Integer[]{1}), new XatkitSession(
                "sessionId"));
    }

    @Test
    public void shutdownRegisteredEventProviderAndActionDefinition() {
        EventProviderDefinition eventProviderDefinition = PlatformFactory.eINSTANCE.createEventProviderDefinition();
        eventProviderDefinition.setName("StubInputProvider");
        runtimePlatform.startEventProvider(eventProviderDefinition);
        ActionDefinition actionDefinition = getNoParameterActionDefinition();
        // Enables the actionDefinition in the RuntimePlatform
        runtimePlatform.shutdown();
        assertThat(runtimePlatform.getActions()).as("Empty Action map").isEmpty();
        assertThat(runtimePlatform.getEventProviderMap()).as("Empty RuntimeEventProvider map").isEmpty();
    }

    private ActionDefinition getNoParameterActionDefinition() {
        ActionDefinition actionDefinition = PlatformFactory.eINSTANCE.createActionDefinition();
        actionDefinition.setName("StubRuntimeActionNoParameter");
        PlatformDefinition platformDefinition = PlatformFactory.eINSTANCE.createPlatformDefinition();
        platformDefinition.setName("StubRuntimePlatform");
        platformDefinition.getActions().add(actionDefinition);
        return actionDefinition;
    }

    private ActionDefinition getParameterActionDefinition() {
        ActionDefinition actionDefinition = PlatformFactory.eINSTANCE.createActionDefinition();
        actionDefinition.setName("StubRuntimeActionTwoConstructors");
        Parameter param = PlatformFactory.eINSTANCE.createParameter();
        param.setKey("param");
        actionDefinition.getParameters().add(param);
        PlatformDefinition platformDefinition = PlatformFactory.eINSTANCE.createPlatformDefinition();
        platformDefinition.setName("StubRuntimePlatform");
        platformDefinition.getActions().add(actionDefinition);
        return actionDefinition;
    }

    private ActionDefinition getNotRegisteredActionDefinition() {
        ActionDefinition actionDefinition = PlatformFactory.eINSTANCE.createActionDefinition();
        actionDefinition.setName("NotRegisteredAction");
        PlatformDefinition platformDefinition = PlatformFactory.eINSTANCE.createPlatformDefinition();
        platformDefinition.setName("StubRuntimePlatform");
        platformDefinition.getActions().add(actionDefinition);
        return actionDefinition;
    }
}
