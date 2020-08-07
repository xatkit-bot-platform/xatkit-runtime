package com.xatkit.dsl.state;

import com.xatkit.execution.State;

public interface MoveToStep extends StateProvider {

    OptionalWhenStep moveTo(StateProvider stateProvider);

    OptionalWhenStep moveTo(State state);
}
