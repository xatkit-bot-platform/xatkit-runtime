package com.xatkit.test.bot;

import com.xatkit.dsl.DSL;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.RecognizedIntent;
import lombok.Data;
import lombok.val;

import static com.xatkit.dsl.DSL.fallbackState;
import static com.xatkit.dsl.DSL.intent;
import static com.xatkit.dsl.DSL.intentIs;
import static com.xatkit.dsl.DSL.state;

@Data
public class TestBot {

    private RecognizedIntent navigableIntent;

    private RecognizedIntent notNavigableIntent;

    private boolean greetingsStateBodyExecuted;

    private boolean defaultFallbackExecuted;

    private boolean sessionCheckedBodyExecuted;

    private ExecutionModel model;

    public TestBot() {
        this.greetingsStateBodyExecuted = false;
        this.defaultFallbackExecuted = false;
        this.sessionCheckedBodyExecuted = false;
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
                .body(context -> greetingsStateBodyExecuted = true)
                .next()
                .moveTo(init);

        sessionCheckedState
                .body(context -> {
                    /*
                     * Remove the key otherwise we have an infinite loop between Init and SessionChecked.
                     */
                    context.getSession().remove("key");
                    sessionCheckedBodyExecuted = true;
                })
                .next()
                .moveTo(init);

        val fallback = fallbackState()
                .body(context -> defaultFallbackExecuted = true);

        model = DSL.model()
                .useIntent(greetings)
                .useState(greetingsState)
                .useState(sessionCheckedState)
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

    public void reset() {
        this.greetingsStateBodyExecuted = false;
        this.defaultFallbackExecuted = false;
        this.sessionCheckedBodyExecuted = false;
    }
}
