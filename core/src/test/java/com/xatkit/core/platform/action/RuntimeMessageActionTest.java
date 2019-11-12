package com.xatkit.core.platform.action;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.XatkitCore;
import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.session.RuntimeContexts;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.stubs.StubRuntimePlatform;
import com.xatkit.stubs.StubXatkitCore;
import com.xatkit.stubs.action.StubRuntimeMessageAction;
import com.xatkit.stubs.action.StubRuntimeMessageActionIOException;
import com.xatkit.stubs.action.StubRuntimeMessageActionIOExceptionThenOk;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class RuntimeMessageActionTest extends AbstractXatkitTest {

    private static String MESSAGE = "test message";

    private static XatkitCore XATKIT_CORE;

    private static RuntimePlatform RUNTIME_PLATFORM;

    private XatkitSession session;

    @BeforeClass
    public static void setUpBeforeClass() {
        XATKIT_CORE = new StubXatkitCore();
        RUNTIME_PLATFORM = new StubRuntimePlatform(XATKIT_CORE, new BaseConfiguration());
    }

    @AfterClass
    public static void tearDownAfterClass() {
        RUNTIME_PLATFORM.shutdown();
        XATKIT_CORE.shutdown();
    }

    @Before
    public void setUp() {
        session = new XatkitSession(UUID.randomUUID().toString());
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
    public void initRuntimeMessageActionDifferentSessionFromGetClientSession() {
        /*
         * Test that the session are merged.
         */
        session.getRuntimeContexts().setContextValue("Test", 5, "key", "value");
        RuntimeMessageAction runtimeMessageAction = new StubRuntimeMessageAction(RUNTIME_PLATFORM, session, MESSAGE);
        runtimeMessageAction.init();
        XatkitSession clientSession = runtimeMessageAction.getClientSession();
        assertThat(clientSession).as("Not null client session").isNotNull();
        RuntimeContexts context = clientSession.getRuntimeContexts();
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
    public void callRuntimeMessageActionWithDelay() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(RuntimeArtifactAction.MESSAGE_DELAY_KEY, 2000);
        RuntimePlatform stubPlatform = new StubRuntimePlatform(XATKIT_CORE, configuration);
        StubRuntimeMessageAction action = new StubRuntimeMessageAction(stubPlatform, session, MESSAGE);
        long before = System.currentTimeMillis();
        RuntimeActionResult result = action.call();
        long after = System.currentTimeMillis();
        assertThat(after - before).as("The action waited the configured delay").isGreaterThan(2000);
        assertThat(action.getAttempts()).as("Valid attempt number (1)").isEqualTo(1);
        assertThat(result).as("Not null result").isNotNull();
        assertThat(result.isError()).as("Result is not an error").isFalse();
        assertThat(result.getResult()).as("Valid result").isEqualTo(StubRuntimeMessageAction.RESULT);
        stubPlatform.shutdown();
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
