package com.xatkit.dsl.state.impl;

import com.xatkit.dsl.state.MoveToStep;
import com.xatkit.dsl.state.OptionalWhenStep;
import com.xatkit.dsl.state.StateProvider;
import com.xatkit.dsl.state.TransitionStep;
import com.xatkit.dsl.state.WhenStep;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.GuardedTransition;
import com.xatkit.execution.State;
import com.xatkit.execution.StateContext;
import com.xatkit.execution.Transition;
import lombok.NonNull;

import java.util.function.Predicate;

import static java.util.Objects.isNull;

public class TransitionBuilder extends StateBuilder implements
        TransitionStep,
        WhenStep,
        OptionalWhenStep,
        MoveToStep {

    private Transition transition;

    public TransitionBuilder(@NonNull State parent) {
        this.state = parent;
    }

    @Override
    public OptionalWhenStep moveTo(@NonNull StateProvider stateProvider) {
        return this.moveTo(stateProvider.getState());
    }

    @Override
    public OptionalWhenStep moveTo(@NonNull State state) {
        if (isNull(this.transition)) {
            /*
             * The delegate didn't create a transition from a previous call, we are defining an AutoTransition.
             */
            this.transition = ExecutionFactory.eINSTANCE.createAutoTransition();
        }
        this.transition.setState(state);
        /*
         * Add the transition we are building to the parent state (we know it is complete at this point).
         */
        this.state.getTransitions().add(this.transition);
        return this;
    }

    @Override
    public @NonNull MoveToStep when(@NonNull Predicate<StateContext> condition) {
        if (isNull(this.transition)) {
            GuardedTransition guardedTransition = ExecutionFactory.eINSTANCE.createGuardedTransition();
            guardedTransition.setCondition(condition);
            this.transition = guardedTransition;
            return this;
        } else {
            /*
             * The current transition already contains a "moveTo" state. This invocation of when() means that we are
             * creating another transition:
             * next()
             *   .when(...).moveTo(...)
             *   .when() // Here we are defining a new transition.
             * We need to create and return a new builder to reflect this behavior.
             */
            return new TransitionBuilder(this.state).when(condition);
        }
    }
}
