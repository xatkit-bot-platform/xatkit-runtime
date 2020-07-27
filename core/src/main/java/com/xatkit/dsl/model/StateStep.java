package com.xatkit.dsl.model;

import com.xatkit.dsl.state.StateProvider;

public interface StateStep extends InitStateStep {

    StateStep state(StateProvider stateProvider);
}
