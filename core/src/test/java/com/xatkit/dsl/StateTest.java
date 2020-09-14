package com.xatkit.dsl;

import com.xatkit.execution.AutoTransition;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.GuardedTransition;
import com.xatkit.execution.State;
import com.xatkit.execution.StateContext;
import com.xatkit.execution.Transition;
import com.xatkit.intent.EventInstance;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.util.predicate.IsEventDefinitionPredicate;
import com.xatkit.util.predicate.IsIntentDefinitionPredicate;
import lombok.val;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.xatkit.dsl.DSL.event;
import static com.xatkit.dsl.DSL.eventIs;
import static com.xatkit.dsl.DSL.fallbackState;
import static com.xatkit.dsl.DSL.intent;
import static com.xatkit.dsl.DSL.intentIs;
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
        assertThat(transition).isInstanceOf(AutoTransition.class);
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
        assertThat(transition).isInstanceOf(GuardedTransition.class);
        GuardedTransition guardedTransition = (GuardedTransition) transition;
        /*
         * Create a StateContext to test the lambda.
         */
        StateContext stateContext = ExecutionFactory.eINSTANCE.createStateContext();
        Map<Object, Object> sessionMap = new HashMap<>();
        sessionMap.put("key", "value");
        stateContext.setSession(sessionMap);
        assertThat(guardedTransition.getCondition().test(stateContext)).isTrue();
        /*
         * Change the session value to make sure the lambda returns the correct result.
         */
        sessionMap.put("key", "anotherValue");
        assertThat(guardedTransition.getCondition().test(stateContext)).isFalse();
        assertThat(transition.getState()).isEqualTo(s1);
    }

    @Test
    public void stateWithIntentIsTransition() {
        val intent = intent("Intent")
                .trainingSentence("Hi")
                .getIntentDefinition();

        val state = state("State")
                .next()
                    .when(intentIs(intent)).moveTo(s1);

        State base = state.getState();
        assertThat(base.getName()).isEqualTo("State");
        assertThat(base.getTransitions()).hasSize(1);
        Transition transition = base.getTransitions().get(0);
        assertThat(transition).isInstanceOf(GuardedTransition.class);
        GuardedTransition guardedTransition = (GuardedTransition) transition;
        assertThat(guardedTransition.getCondition()).isInstanceOf(IsIntentDefinitionPredicate.class);
        IsIntentDefinitionPredicate predicate = (IsIntentDefinitionPredicate) guardedTransition.getCondition();
        assertThat(predicate.getIntentDefinition()).isEqualTo(intent);
        /*
         * Create a StateContext to test the lambda.
         */
        StateContext stateContext = ExecutionFactory.eINSTANCE.createStateContext();
        RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
        recognizedIntent.setDefinition(intent);
        stateContext.setEventInstance(recognizedIntent);
        assertThat(guardedTransition.getCondition().test(stateContext)).isTrue();
    }

    @Test
    public void stateWithEventIsTransition() {
        val event = event("Event")
                .getEventDefinition();

        val state = state("State")
                .next()
                    .when(eventIs(event)).moveTo(s1);

        State base = state.getState();
        assertThat(base.getName()).isEqualTo("State");
        assertThat(base.getTransitions()).hasSize(1);
        Transition transition = base.getTransitions().get(0);
        assertThat(transition).isInstanceOf(GuardedTransition.class);
        GuardedTransition guardedTransition = (GuardedTransition) transition;
        assertThat(guardedTransition.getCondition()).isInstanceOf(IsEventDefinitionPredicate.class);
        IsEventDefinitionPredicate predicate = (IsEventDefinitionPredicate) guardedTransition.getCondition();
        assertThat(predicate.getEventDefinition()).isEqualTo(event);
        /*
         * Create a StateContext to test the lambda.
         */
        StateContext stateContext = ExecutionFactory.eINSTANCE.createStateContext();
        EventInstance eventInstance = IntentFactory.eINSTANCE.createEventInstance();
        eventInstance.setDefinition(event);
        stateContext.setEventInstance(eventInstance);
        assertThat(guardedTransition.getCondition().test(stateContext)).isTrue();
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
