package fr.zelus.jarvis.core.platform.action;

import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.core.platform.RuntimePlatform;
import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.stubs.StubJarvisCore;
import fr.zelus.jarvis.stubs.StubRuntimePlatform;
import fr.zelus.jarvis.stubs.action.StubRuntimeMessageAction;
import fr.zelus.jarvis.stubs.action.StubRuntimeMessageActionIOException;
import fr.zelus.jarvis.stubs.action.StubRuntimeMessageActionIOExceptionThenOk;
import org.apache.commons.configuration2.BaseConfiguration;
import org.junit.*;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class RuntimeMessageActionTest {

    private static String MESSAGE = "test message";

    private static String MESSAGE_WITH_VARIABLE = "test {$Test.key}";

    private static JarvisCore JARVIS_CORE;

    private static RuntimePlatform RUNTIME_PLATFORM;

    private JarvisSession session;

    @BeforeClass
    public static void setUpBeforeClass() {
        JARVIS_CORE = new StubJarvisCore();
        RUNTIME_PLATFORM = new StubRuntimePlatform(JARVIS_CORE, new BaseConfiguration());
    }

    @AfterClass
    public static void tearDownAfterClass() {
        RUNTIME_PLATFORM.shutdown();
        JARVIS_CORE.shutdown();
    }

    @Before
    public void setUp() {
        session = new JarvisSession(UUID.randomUUID().toString());
    }

    @After
    public void tearDown() {
        session = null;
    }

    @Test(expected = NullPointerException.class)
    public void constructRuntimeMessageActionNullPlatform() {
        new StubRuntimeMessageAction(null, session, MESSAGE);
    }

    @Test(expected = NullPointerException.class)
    public void constructRuntimeMessageActionNullSession() {
        new StubRuntimeMessageAction(RUNTIME_PLATFORM, null, MESSAGE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructRuntimeMessageActionNullMessage() {
        new StubRuntimeMessageAction(RUNTIME_PLATFORM, session, null);
    }

    @Test
    public void constructValidRuntimeMessageAction() {
        RuntimeMessageAction action = new StubRuntimeMessageAction(RUNTIME_PLATFORM, session, MESSAGE);
        assertThat(action.getMessage()).as("Valid message").isEqualTo(MESSAGE);
    }

    @Test
    public void constructValidRuntimeMessageActionMessageWithVariable() {
        session.getJarvisContext().setContextValue("Test", 5, "key", "value");
        RuntimeMessageAction action = new StubRuntimeMessageAction(RUNTIME_PLATFORM, session, MESSAGE_WITH_VARIABLE);
        assertThat(action.getMessage()).as("Action message variable has been replaced").isEqualTo("test value");
    }

    @Test
    public void initRuntimeMessageActionDifferentSessionFromGetClientSession() {
        /*
         * Test that the session are merged.
         */
        session.getJarvisContext().setContextValue("Test", 5, "key", "value");
        RuntimeMessageAction runtimeMessageAction = new StubRuntimeMessageAction(RUNTIME_PLATFORM, session, MESSAGE);
        runtimeMessageAction.init();
        JarvisSession clientSession = runtimeMessageAction.getClientSession();
        assertThat(clientSession).as("Not null client session").isNotNull();
        JarvisContext context = clientSession.getJarvisContext();
        assertThat(context.getContextValue("Test", "key")).as("Session context has been merged in the client one")
                .isEqualTo("value");
    }

    @Test
    public void callRuntimeMessageActionOk() throws Exception {
        StubRuntimeMessageAction action = new StubRuntimeMessageAction(RUNTIME_PLATFORM, session, MESSAGE);
        RuntimeActionResult result = action.call();
        assertThat(action.getAttempts()).as("Valid attempt number (1)").isEqualTo(1);
        assertThat(result).as("Not null result").isNotNull();
        assertThat(result.isError()).as("Result is not an error").isFalse();
        assertThat(result.getResult()).as("Valid result").isEqualTo(StubRuntimeMessageAction.RESULT);
    }

    @Test
    public void callRuntimeMessageActionIOException() throws Exception {
        StubRuntimeMessageActionIOException action = new StubRuntimeMessageActionIOException(RUNTIME_PLATFORM, session, MESSAGE);
        RuntimeActionResult result = action.call();
        assertThat(action.getAttempts()).as("Valid attempt number (1 + number of retries)").isEqualTo(4);
        assertThat(result).as("Not null result").isNotNull();
        assertThat(result.isError()).as("Result is error").isTrue();
        assertThat(result.getThrownException()).as("Result threw an IOException").isOfAnyClassIn(IOException.class);
    }

    @Test
    public void callRuntimeMessageActionIOExceptionThenOk() throws Exception {
        StubRuntimeMessageActionIOExceptionThenOk action = new StubRuntimeMessageActionIOExceptionThenOk
                (RUNTIME_PLATFORM, session, MESSAGE);
        RuntimeActionResult result = action.call();
        assertThat(action.getAttempts()).as("Valid attempt number (2)").isEqualTo(2);
        assertThat(result).as("Not null result").isNotNull();
        assertThat(result.isError()).as("Result is not an error").isFalse();
        assertThat(result.getResult()).as("Valid result").isEqualTo(StubRuntimeMessageAction.RESULT);
    }
}
