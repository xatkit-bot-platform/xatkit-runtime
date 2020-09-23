package com.xatkit.core.platform.action;

import com.xatkit.AbstractActionTest;
import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.stubs.StubRuntimePlatform;
import com.xatkit.stubs.action.StubRuntimeMessageAction;
import com.xatkit.stubs.action.StubRuntimeMessageActionIOException;
import com.xatkit.stubs.action.StubRuntimeMessageActionIOExceptionThenOk;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RuntimeMessageActionTest extends AbstractActionTest<RuntimeMessageAction<RuntimePlatform>, RuntimePlatform> {

    private static String MESSAGE = "test message";

    @Test(expected = NullPointerException.class)
    public void constructRuntimeMessageActionNullPlatform() {
        new StubRuntimeMessageAction(null, context, MESSAGE);
    }

    @Test(expected = NullPointerException.class)
    public void constructRuntimeMessageActionNullSession() {
        new StubRuntimeMessageAction(getPlatform(), null, MESSAGE);
    }

    @Test(expected = NullPointerException.class)
    public void constructRuntimeMessageActionNullMessage() {
        new StubRuntimeMessageAction(getPlatform(), context, null);
    }

    @Test
    public void constructValidRuntimeMessageAction() {
        RuntimeMessageAction action = new StubRuntimeMessageAction(getPlatform(), context, MESSAGE);
        assertThat(action.getMessage()).as("Valid message").isEqualTo(MESSAGE);
    }

    @Test
    public void initRuntimeMessageActionDifferentSessionFromGetClientSession() {
        /*
         * Test that the session are merged.
         */
        // TODO update this with proper NLP context
//        session.getRuntimeContexts().setContextValue("Test", 5, "key", "value");
//        RuntimeMessageAction runtimeMessageAction = new StubRuntimeMessageAction(getPlatform(), session, MESSAGE);
//        runtimeMessageAction.init();
//        assertThat(runtimeMessageAction.getClientStateContext()).isInstanceOf(XatkitSession.class);
//        XatkitSession clientSession = (XatkitSession) runtimeMessageAction.getClientStateContext();
//        assertThat(clientSession).as("Not null client session").isNotNull();
//        RuntimeContexts context = clientSession.getRuntimeContexts();
//        assertThat(context.getContextValue("Test", "key")).as("Session context has been merged in the client one")
//                .isEqualTo("value");
    }

    @Test
    public void callRuntimeMessageActionOk() throws Exception {
        StubRuntimeMessageAction action = new StubRuntimeMessageAction(getPlatform(), context, MESSAGE);
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
        RuntimePlatform stubPlatform = new StubRuntimePlatform();
        stubPlatform.start(mockedXatkitBot, configuration);
        StubRuntimeMessageAction action = new StubRuntimeMessageAction(stubPlatform, context, MESSAGE);
        long before = System.currentTimeMillis();
        RuntimeActionResult result = action.call();
        long after = System.currentTimeMillis();
        assertThat(after - before).as("The action waited the configured delay").isGreaterThanOrEqualTo(2000);
        assertThat(action.getAttempts()).as("Valid attempt number (1)").isEqualTo(1);
        assertThat(result).as("Not null result").isNotNull();
        assertThat(result.isError()).as("Result is not an error").isFalse();
        assertThat(result.getResult()).as("Valid result").isEqualTo(StubRuntimeMessageAction.RESULT);
        stubPlatform.shutdown();
    }

    @Test
    public void callRuntimeMessageActionIOException() throws Exception {
        StubRuntimeMessageActionIOException action = new StubRuntimeMessageActionIOException(getPlatform(),
                context, MESSAGE);
        RuntimeActionResult result = action.call();
        assertThat(action.getAttempts()).as("Valid attempt number (1 + number of retries)").isEqualTo(4);
        assertThat(result).as("Not null result").isNotNull();
        assertThat(result.isError()).as("Result is error").isTrue();
        assertThat(result.getThrowable()).as("Result threw an IOException").isOfAnyClassIn(IOException.class);
    }

    @Test
    public void callRuntimeMessageActionIOExceptionThenOk() throws Exception {
        StubRuntimeMessageActionIOExceptionThenOk action = new StubRuntimeMessageActionIOExceptionThenOk
                (getPlatform(), context, MESSAGE);
        RuntimeActionResult result = action.call();
        assertThat(action.getAttempts()).as("Valid attempt number (2)").isEqualTo(2);
        assertThat(result).as("Not null result").isNotNull();
        assertThat(result.isError()).as("Result is not an error").isFalse();
        assertThat(result.getResult()).as("Valid result").isEqualTo(StubRuntimeMessageAction.RESULT);
    }

    @Override
    protected RuntimePlatform getPlatform() {
        RuntimePlatform platform = mock(RuntimePlatform.class);
        when(platform.getConfiguration()).thenReturn(new BaseConfiguration());
        return platform;
    }
}
