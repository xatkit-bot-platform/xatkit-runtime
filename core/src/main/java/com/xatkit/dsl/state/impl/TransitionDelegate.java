package com.xatkit.dsl.state.impl;

import com.xatkit.dsl.state.MoveToStep;
import com.xatkit.dsl.state.OptionalWhenStep;
import com.xatkit.dsl.state.StateProvider;
import com.xatkit.dsl.state.TransitionStep;
import com.xatkit.dsl.state.WhenStep;
import com.xatkit.execution.State;
import com.xatkit.execution.StateContext;
import com.xatkit.execution.Transition;
import lombok.NonNull;

import java.util.function.Predicate;

import static java.util.Objects.isNull;

public class TransitionDelegate extends StateDelegate implements
        TransitionStep,
        WhenStep,
        OptionalWhenStep,
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
        if(isNull(transition.getCondition())) {
            transition.setCondition(x -> true);
        }
        transition.setState(state);
        return this;
    }

    @Override
    public @NonNull MoveToStep when(@NonNull Predicate<StateContext> condition) {
        this.transition.setCondition(condition);
        return this;
    }

}
