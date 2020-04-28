package com.xatkit.core;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.EventInstance;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.language.execution.ExecutionRuntimeModule;
import com.xatkit.platform.PlatformDefinition;
import com.xatkit.stubs.StubRuntimePlatform;
import com.xatkit.test.util.TestBotExecutionModel;
import com.xatkit.test.util.TestModelLoader;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

//import com.xatkit.execution.ActionInstance;

public class ExecutionServiceTest extends AbstractXatkitTest {

    private static TestBotExecutionModel testBotExecutionModel;

    private static Injector injector;

    private static EventInstance VALID_EVENT_INSTANCE;

    private static EventInstance INVALID_EVENT_INSTANCE;

    @BeforeClass
    public static void setUpBeforeClass() throws ConfigurationException {
        testBotExecutionModel = TestModelLoader.loadTestBot();
        injector = Guice.createInjector(new ExecutionRuntimeModule());
        VALID_EVENT_INSTANCE = IntentFactory.eINSTANCE.createRecognizedIntent();
        VALID_EVENT_INSTANCE.setDefinition(testBotExecutionModel.getSimpleIntent());
        INVALID_EVENT_INSTANCE = IntentFactory.eINSTANCE.createEventInstance();
        EventDefinition eventDefinition = IntentFactory.eINSTANCE.createEventDefinition();
        eventDefinition.setName("Invalid");
        INVALID_EVENT_INSTANCE.setDefinition(eventDefinition);
    }

    private RuntimePlatformRegistry platformRegistry;

    private StubRuntimePlatform stubRuntimePlatform;

    private ExecutionService executionService;

    @Before
    public void setUp() {
        platformRegistry = new RuntimePlatformRegistry();
        stubRuntimePlatform = new StubRuntimePlatform(mock(XatkitCore.class), new BaseConfiguration());
        platformRegistry.registerRuntimePlatform("StubRuntimePlatform", stubRuntimePlatform);
        PlatformDefinition mockedPlatformDefinition = mock(PlatformDefinition.class);
        when(mockedPlatformDefinition.getName()).thenReturn("StubRuntimePlatform");
        platformRegistry.registerLoadedPlatformDefinition(mockedPlatformDefinition);
        stubRuntimePlatform.init();
//        RuntimePlatform runtimePlatform = VALID_XATKIT_CORE.getRuntimePlatformRegistry().getRuntimePlatform
//                ("StubRuntimePlatform");
//        if(nonNull(runtimePlatform)) {
//            /*
//             * Call init() to reset the platform actions and avoid comparison issues when checking their state.
//             */
//            ((StubRuntimePlatform) runtimePlatform).init();
//        }
    }

    @After
    public void tearDown() {
        if (nonNull(executionService) && !executionService.isShutdown()) {
            executionService.shutdown();
        }
    }


    @Test(expected = NullPointerException.class)
    public void constructNullExecutionModel() {
        executionService = new ExecutionService(null, platformRegistry, new BaseConfiguration());
    }

    @Test(expected = NullPointerException.class)
    public void constructNullRuntimePlatformRegistry() {
        executionService = new ExecutionService(testBotExecutionModel.getBaseModel(), null, new BaseConfiguration());
    }

    @Test (expected = NullPointerException.class)
    public void constructNullConfiguration() {
        executionService = new ExecutionService(testBotExecutionModel.getBaseModel(), platformRegistry, null);
    }

    @Test
    public void constructValid() {
        executionService = getValidExecutionService();
        assertThat(executionService.getExecutionModel()).as("Valid execution model").isEqualTo
                (testBotExecutionModel.getBaseModel());
        assertThat(executionService.getRuntimePlatformRegistry()).as("Valid Xatkit runtimePlatform registry").isEqualTo
                (platformRegistry);
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
        XatkitSession session = new XatkitSession("sessionID");
        executionService.initSession(session);
        executionService.handleEventInstance(VALID_EVENT_INSTANCE, session);
        /*
         * Sleep because actions are computed asynchronously
         */
        Thread.sleep(1000);
        assertThat(stubRuntimePlatform.getAction().isActionProcessed()).isTrue();
    }

    @Test
    public void handleEventNotHandledEvent() throws InterruptedException {
        executionService = getValidExecutionService();
        XatkitSession session = new XatkitSession("sessionID");
        executionService.initSession(session);
        executionService.handleEventInstance(INVALID_EVENT_INSTANCE, session);
        Thread.sleep(1000);
        assertThat(stubRuntimePlatform.getAction().isActionProcessed()).isFalse();
        /*
         * We stay in Init if the event does not trigger a transition.
         */
        assertThat(session.getState().getName()).isEqualTo("Init");
    }

    /*
     * Test for issue #295: https://github.com/xatkit-bot-platform/xatkit-runtime/issues/295.
     * This test sends an event that make the engine enter a state that only checks for context value before moving
     * back to the Init state (not that the value itself is not important here, the transition checks if the value
     * exists or not, but in both cases points to the Init state).
     */
    @Test
    public void handleEventToContextCheckingState() throws InterruptedException {
        executionService = getValidExecutionService();
        XatkitSession session =  new XatkitSession("sessionId");
        executionService.initSession(session);
        RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
        recognizedIntent.setDefinition(testBotExecutionModel.getContextCheckingIntent());
        executionService.handleEventInstance(recognizedIntent, session);
        Thread.sleep(1000);
        assertThat(session.getState().getName()).isEqualTo("Init");
    }

    private ExecutionService getValidExecutionService() {
        executionService = new ExecutionService(testBotExecutionModel.getBaseModel(), platformRegistry, new BaseConfiguration());
        injector.injectMembers(executionService);
        return executionService;
    }
}
