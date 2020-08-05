package com.xatkit.core;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.dsl.DSL;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.RecognizedIntent;
import lombok.val;
import org.apache.commons.configuration2.BaseConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.xatkit.dsl.DSL.fallbackState;
import static com.xatkit.dsl.DSL.intent;
import static com.xatkit.dsl.DSL.intentIs;
import static com.xatkit.dsl.DSL.state;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class ExecutionServiceTest extends AbstractXatkitTest {

    private ExecutionService executionService;

    private RecognizedIntent navigableIntent;

    private RecognizedIntent notNavigableIntent;

    private boolean bodyExecuted;

    private boolean defaultFallbackExecuted;

    private boolean sessionChecked;

    private ExecutionModel model;

    @Before
    public void setUp() {
        this.bodyExecuted = false;
        this.defaultFallbackExecuted = false;
        this.sessionChecked = false;
        val greetings = intent("Greetings")
                .trainingSentence("Hi");

        val init = state("Init");
        val greetingsState = state("GreetingsState");
        val sessionCheckedState = state("SessionChecked");

        init
                .next()
                    .when(intentIs(greetings)).moveTo(greetingsState)
                    .when(stateContext -> stateContext.getSession().containsKey("key")).moveTo(sessionCheckedState);

        greetingsState
                .body(context -> bodyExecuted = true)
                .next()
                    .moveTo(init);

        sessionCheckedState
                .body(context -> {
                    /*
                     * Remove the key otherwise we have an infinite loop between Init and SessionChecked.
                     */
                    context.getSession().remove("key");
                    sessionChecked = true;
                })
                .next()
                    .moveTo(init);

        val fallback = fallbackState()
                .body(context -> defaultFallbackExecuted = true);

        model = DSL.model()
                .useIntent(greetings)
                .state(greetingsState)
                .initState(init)
                .defaultFallbackState(fallback)
                .getExecutionModel();

        navigableIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
        navigableIntent.setDefinition(greetings.getIntentDefinition());

        val unmatchedIntentDefinition = intent("Unmatched")
                .trainingSentence("Unmatched");
        notNavigableIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
        notNavigableIntent.setDefinition(unmatchedIntentDefinition.getIntentDefinition());
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
        executionService = new ExecutionService(model, null);
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
        executionService.handleEventInstance(null, new XatkitSession("sessionID"));
    }

    @Test(expected = NullPointerException.class)
    public void handleEventNullSession() {
        executionService = getValidExecutionService();
        executionService.handleEventInstance(navigableIntent, null);
    }

    @Test
    public void handleEventNavigableEvent() throws InterruptedException {
        executionService = getValidExecutionService();
        XatkitSession session = new XatkitSession("sessionID");
        executionService.initContext(session);
        executionService.handleEventInstance(navigableIntent, session);
        /*
         * Sleep because actions are computed asynchronously
         */
        Thread.sleep(1000);
        assertThat(this.bodyExecuted).isTrue();
        /*
         * The session check transition hasn't been navigated.
         */
        assertThat(this.sessionChecked).isFalse();
    }

    @Test
    public void handleEventNotNavigableEvent() throws InterruptedException {
        executionService = getValidExecutionService();
        XatkitSession session = new XatkitSession("sessionID");
        executionService.initContext(session);
        executionService.handleEventInstance(notNavigableIntent, session);
        Thread.sleep(1000);
        assertThat(this.bodyExecuted).isFalse();
        assertThat(this.defaultFallbackExecuted).isTrue();
        /*
         * The session check transition hasn't been navigated.
         */
        assertThat(this.sessionChecked).isFalse();
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
        session.getSession().put("key", "value");
        executionService.initContext(session);
        Thread.sleep(1000);
        /*
         * The session checked state has been visited.
         */
        assertThat(this.sessionChecked).isTrue();
        /*
         * And the auto transition to Init too.
         */
        assertThat(session.getState().getName()).isEqualTo("Init");
    }

    private ExecutionService getValidExecutionService() {
        return new ExecutionService(model, new BaseConfiguration());
    }
}
