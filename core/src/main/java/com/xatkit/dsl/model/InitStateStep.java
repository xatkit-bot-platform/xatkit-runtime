package com.xatkit.dsl.model;

import com.xatkit.dsl.state.StateProvider;
import com.xatkit.execution.State;

public interface InitStateStep {

    DefaultFallbackStateStep initState(StateProvider stateProvider);

    DefaultFallbackStateStep initState(State state);
}
