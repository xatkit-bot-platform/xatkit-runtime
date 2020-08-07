package com.xatkit.dsl.state;

import com.xatkit.execution.State;

public interface TransitionStep extends WhenStep, StateProvider {

//    MoveToStep when(Predicate<Object> condition);
    // Return state provider here because we don't want to allow fallback
    StateProvider moveTo(StateProvider stateProvider);

    StateProvider moveTo(State state);
}
