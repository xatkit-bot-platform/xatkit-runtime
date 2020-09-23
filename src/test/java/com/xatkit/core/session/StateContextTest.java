package com.xatkit.core.session;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.State;
import com.xatkit.execution.StateContext;
import lombok.val;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import static com.xatkit.dsl.DSL.intent;
import static com.xatkit.dsl.DSL.intentIs;
import static com.xatkit.dsl.DSL.state;
import static org.assertj.core.api.Assertions.assertThat;

public class StateContextTest extends AbstractXatkitTest {

    private static State TEST_STATE = ExecutionFactory.eINSTANCE.createState();

    static {
        TEST_STATE.setName("Test_State");
    }

    private StateContext context;

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test
    public void getStateNewSession() {
        context = ExecutionFactory.eINSTANCE.createStateContext();
        context.setContextId("contextId");
        assertThat(context.getState()).as("Session state is null").isNull();
    }

    @Test
    public void setStateNewSession() {
        context = ExecutionFactory.eINSTANCE.createStateContext();
        context.setContextId("contextId");
        context.setState(TEST_STATE);
        assertThat(context.getState()).as("State has been set").isEqualTo(TEST_STATE);
    }

    @Test
    public void setStateErasePreviousState() {
        context = ExecutionFactory.eINSTANCE.createStateContext();
        context.setContextId("contextId");
        context.setState(TEST_STATE);
        State testState2 = ExecutionFactory.eINSTANCE.createState();
        testState2.setName("Test_State2");
        context.setState(testState2);
        assertThat(context.getState()).as("State correctly erased").isEqualTo(testState2);
    }

    @Test
    public void setStateWithComposedTransitionCondition() {
        context = ExecutionFactory.eINSTANCE.createStateContext();
        context.setContextId("contextId");
        val intent = intent("Intent")
                .trainingSentence("Hi");
        val state = state("State")
                .next()
                /*
                 * The actual and condition doesn't matter here.
                 */
                .when(intentIs(intent).and(context -> context.getSession().containsKey("test"))).moveTo(TEST_STATE)
                .getState();
        context.setState(state);
        assertThat(context.getState()).isEqualTo(state);
    }
}
