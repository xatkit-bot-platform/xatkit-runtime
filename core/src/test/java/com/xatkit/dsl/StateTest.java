package com.xatkit.dsl;

import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.State;
import com.xatkit.execution.StateContext;
import com.xatkit.execution.Transition;
import lombok.val;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.xatkit.dsl.DSL.fallbackState;
import static com.xatkit.dsl.DSL.state;
import static org.assertj.core.api.Assertions.assertThat;

public class StateTest {

    private State s1;

    @Before
    public void setUp() {
        s1 = ExecutionFactory.eINSTANCE.createState();
        s1.setName("s1");
    }

    @Test
    public void stateWithAutoTransition() {
        val state = state("State")
                .next()
                    .moveTo(s1);

        State base = state.getState();
        assertThat(base.getName()).isEqualTo("State");
        assertThat(base.getTransitions()).hasSize(1);
        Transition transition = base.getTransitions().get(0);
        /*
         * Dirty way to test that the predicate returns true. It's not easy to compare lambdas.
         */
        assertThat(transition.getCondition().test(null)).isTrue();
        assertThat(transition.getState()).isEqualTo(s1);
    }

    @Test
    public void stateWithCustomTransition() {
        val state = state("State")
                .next()
                    .when(context -> context.getSession().get("key").equals("value"))
                        .moveTo(s1);

        State base = state.getState();
        assertThat(base.getName()).isEqualTo("State");
        assertThat(base.getTransitions()).hasSize(1);
        Transition transition = base.getTransitions().get(0);
        /*
         * Create a StateContext to test the lambda.
         */
        StateContext stateContext = ExecutionFactory.eINSTANCE.createStateContext();
        Map<Object, Object> sessionMap = new HashMap<>();
        sessionMap.put("key", "value");
        stateContext.setSession(sessionMap);
        assertThat(transition.getCondition().test(stateContext)).isTrue();
        /*
         * Change the session value to make sure the lambda returns the correct result.
         */
        sessionMap.put("key", "anotherValue");
        assertThat(transition.getCondition().test(stateContext)).isFalse();
        assertThat(transition.getState()).isEqualTo(s1);
    }

    @Test
    public void stateWithConsumerBody() {
        val state = state("State")
                .body(context -> context.getSession().put("key", "value"))
                .next()
                    .moveTo(s1);

        State base = state.getState();
        assertThat(base.getName()).isEqualTo("State");
        assertThat(base.getBody()).isNotNull();
        val body = base.getBody();
        Map<Object, Object> testSession = new HashMap<>();
        StateContext stateContext = ExecutionFactory.eINSTANCE.createStateContext();
        stateContext.setSession(testSession);
        body.accept(stateContext);
        assertThat(testSession).containsKey("key");
        assertThat(testSession).containsValue("value");
    }

    @Test
    public void stateWithConsumerFallback() {
        val state = state("State")
                .next()
                    .when(x -> true).moveTo(s1)
                .fallback(context -> context.getSession().put("key", "value"));

        State base = state.getState();
        assertThat(base.getName()).isEqualTo("State");
        assertThat(base.getFallback()).isNotNull();
        val fallback = base.getFallback();
        Map<Object, Object> testSession = new HashMap<>();
        StateContext stateContext = ExecutionFactory.eINSTANCE.createStateContext();
        stateContext.setSession(testSession);
        fallback.accept(stateContext);
        assertThat(testSession).containsKey("key");
        assertThat(testSession).containsValue("value");
    }

    @Test
    public void createFallbackState() {
        val fallbackState = fallbackState()
                .body(context -> context.getSession().put("key", "value"));

        State base = fallbackState.getState();
        assertThat(base.getName()).isEqualTo("Default_Fallback");
        assertThat(base.getFallback()).isNull();
        assertThat(base.getTransitions()).isEmpty();
        assertThat(base.getBody()).isNotNull();
        val body = base.getBody();
        Map<Object, Object> testSession = new HashMap<>();
        StateContext stateContext = ExecutionFactory.eINSTANCE.createStateContext();
        stateContext.setSession(testSession);
        body.accept(stateContext);
        assertThat(testSession).containsKey("key");
        assertThat(testSession).containsValue("value");
    }
}
