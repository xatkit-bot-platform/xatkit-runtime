package com.xatkit.core;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.StateContext;
import com.xatkit.test.bot.TestBot;
import org.apache.commons.configuration2.BaseConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class ExecutionServiceTest extends AbstractXatkitTest {

    private ExecutionService executionService;

    private TestBot testBot;

    @Before
    public void setUp() {
        testBot = new TestBot();
    }

    @After
    public void tearDown() {
        if (nonNull(executionService) && !executionService.isShutdown()) {
            executionService.shutdown();
        }
    }


    @Test(expected = NullPointerException.class)
    public void constructNullExecutionModel() {
        executionService = new ExecutionService(null, new BaseConfiguration());
    }

    @Test (expected = NullPointerException.class)
    public void constructNullConfiguration() {
        executionService = new ExecutionService(testBot.getModel(), null);
    }

    @Test
    public void constructValid() {
        executionService = getValidExecutionService();
        assertThat(executionService.getExecutorService()).as("Not null ExecutorService").isNotNull();
        assertThat(executionService.isShutdown()).as("Execution service started").isFalse();
    }

    @Test(expected = NullPointerException.class)
    public void handleEventNullEvent() {
        executionService = getValidExecutionService();
        StateContext context = ExecutionFactory.eINSTANCE.createStateContext();
        context.setContextId("contextID");
        executionService.handleEventInstance(null, context);
    }

    @Test(expected = NullPointerException.class)
    public void handleEventNullSession() {
        executionService = getValidExecutionService();
        executionService.handleEventInstance(testBot.getNavigableIntent(), null);
    }

    @Test
    public void handleEventNavigableEvent() throws InterruptedException {
        executionService = getValidExecutionService();
        StateContext context = ExecutionFactory.eINSTANCE.createStateContext();
        context.setContextId("contextId");
        executionService.initContext(context);
        executionService.handleEventInstance(testBot.getNavigableIntent(), context);
        /*
         * Sleep because actions are computed asynchronously
         */
        Thread.sleep(1000);
        assertThat(testBot.isGreetingsStateBodyExecuted()).isTrue();
        /*
         * The context check transition hasn't been navigated.
         */
        assertThat(testBot.isDefaultFallbackExecuted()).isFalse();
    }

    @Test
    public void handleEventNotNavigableEvent() throws InterruptedException {
        executionService = getValidExecutionService();
        StateContext context = ExecutionFactory.eINSTANCE.createStateContext();
        context.setContextId("contextId");
        executionService.initContext(context);
        executionService.handleEventInstance(testBot.getNotNavigableIntent(), context);
        Thread.sleep(1000);
        assertThat(testBot.isGreetingsStateBodyExecuted()).isFalse();
        assertThat(testBot.isDefaultFallbackExecuted()).isTrue();
        /*
         * The context check transition hasn't been navigated.
         */
        assertThat(testBot.isSessionCheckedBodyExecuted()).isFalse();
        /*
         * We stay in Init if the event does not trigger a transition.
         */
        assertThat(context.getState().getName()).isEqualTo("Init");
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
        StateContext context = ExecutionFactory.eINSTANCE.createStateContext();
        context.setContextId("contextId");
        context.getSession().put("key", "value");
        executionService.initContext(context);
        Thread.sleep(1000);
        /*
         * The context checked state has been visited.
         */
        assertThat(testBot.isSessionCheckedBodyExecuted()).isTrue();
        /*
         * And the auto transition to Init too.
         */
        assertThat(context.getState().getName()).isEqualTo("Init");
    }

    private ExecutionService getValidExecutionService() {
        return new ExecutionService(testBot.getModel(), new BaseConfiguration());
    }
}
