package com.xatkit.dsl.state.impl;

import com.xatkit.dsl.state.StateProvider;
import com.xatkit.execution.State;
import lombok.NonNull;

public class StateProviderImpl implements StateProvider {

    protected State state;

    public StateProviderImpl(@NonNull State state) {
        this.state = state;
    }

    @Override
    public @NonNull State getState() {
        return state;
    }
}
