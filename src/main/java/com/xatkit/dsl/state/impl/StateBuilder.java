package com.xatkit.dsl.state.impl;

import com.xatkit.dsl.state.BodyStep;
import com.xatkit.dsl.state.FallbackBodyStep;
import com.xatkit.dsl.state.FallbackStep;
import com.xatkit.dsl.state.NextStep;
import com.xatkit.dsl.state.StateProvider;
import com.xatkit.dsl.state.TransitionStep;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.StateContext;
import lombok.NonNull;

import java.util.function.Consumer;

public class StateBuilder extends StateProviderImpl implements
        BodyStep,
        FallbackBodyStep,
        NextStep,
        FallbackStep {

    public StateBuilder() {
        this.state = ExecutionFactory.eINSTANCE.createState();
    }

    public @NonNull StateBuilder name(@NonNull String name) {
        this.state.setName(name);
        return this;
    }

    @Override
    public @NonNull NextStep body(@NonNull Consumer<StateContext> body) {
        this.state.setBody(body);
        return this;
    }

    @Override
    public @NonNull TransitionStep next() {
        return new TransitionBuilder(this.state);
    }

    @Override
    public @NonNull StateProvider fallback(@NonNull Consumer<StateContext> fallback) {
        this.state.setFallback(fallback);
        return this;
    }




}
