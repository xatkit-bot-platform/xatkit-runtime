package com.xatkit.dsl.state.impl;

import com.xatkit.dsl.intent.EventDefinitionProvider;
import com.xatkit.dsl.intent.IntentDefinitionProvider;
import com.xatkit.dsl.state.EventPredicateStep;
import com.xatkit.dsl.state.MoveToStep;
import com.xatkit.dsl.state.OptionalWhenStep;
import com.xatkit.dsl.state.StateProvider;
import com.xatkit.dsl.state.TransitionStep;
import com.xatkit.dsl.state.WhenStep;
import com.xatkit.execution.State;
import com.xatkit.execution.StateContext;
import com.xatkit.execution.Transition;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.util.IsEventDefinitionPredicate;
import com.xatkit.util.IsIntentDefinitionPredicate;
import lombok.NonNull;

import java.util.function.Predicate;

public class TransitionDelegate extends StateDelegate implements
        TransitionStep,
        WhenStep,
        OptionalWhenStep,
        EventPredicateStep,
        MoveToStep {

    private Transition transition;

    public TransitionDelegate(@NonNull State state, @NonNull Transition transition) {
        super(state);
        this.transition = transition;
    }

    @Override
    public OptionalWhenStep moveTo(@NonNull StateProvider stateProvider) {
        return this.moveTo(stateProvider.getState());
    }

    @Override
    public OptionalWhenStep moveTo(@NonNull State state) {
        /*
         * Do not set a x -> true predicate here, otherwise there is no way to differenciate auto-transitions from
         * custom transitions. We need to handle null values in the execution engine.
         */
        transition.setState(state);
        return this;
    }

    @Override
    public @NonNull MoveToStep when(@NonNull Predicate<StateContext> condition) {
        this.transition.setCondition(condition);
        return this;
    }

    @Override
    public EventPredicateStep when() {
        return this;
    }

    @Override
    public MoveToStep intentIs(IntentDefinitionProvider intentDefinitionProvider) {
        return this.intentIs(intentDefinitionProvider.getIntentDefinition());
    }

    @Override
    public MoveToStep intentIs(IntentDefinition intentDefinition) {
        this.transition.setCondition(new IsIntentDefinitionPredicate(intentDefinition));
        return this;
    }

    @Override
    public MoveToStep eventIs(EventDefinitionProvider eventDefinitionProvider) {
        return this.eventIs(eventDefinitionProvider.getEventDefinition());
    }

    @Override
    public MoveToStep eventIs(EventDefinition eventDefinition) {
        this.transition.setCondition(new IsEventDefinitionPredicate(eventDefinition));
        return this;
    }
}
