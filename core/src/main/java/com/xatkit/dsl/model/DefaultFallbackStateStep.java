package com.xatkit.dsl.model;

import com.xatkit.dsl.state.StateProvider;
import com.xatkit.execution.State;

public interface DefaultFallbackStateStep {

    ExecutionModelProvider defaultFallbackState(StateProvider stateProvider);

    ExecutionModelProvider defaultFallbackState(State state);
}
