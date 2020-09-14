package com.xatkit.dsl.state.impl;

import com.xatkit.dsl.state.BodyStep;
import com.xatkit.dsl.state.FallbackBodyStep;
import com.xatkit.dsl.state.FallbackStep;
import com.xatkit.dsl.state.NextStep;
import com.xatkit.dsl.state.StateProvider;
import com.xatkit.dsl.state.TransitionStep;
import com.xatkit.execution.State;
import com.xatkit.execution.StateContext;
import lombok.NonNull;

import java.util.function.Consumer;

public class StateDelegate extends StateProviderImpl implements
        BodyStep,
        FallbackBodyStep,
        NextStep,
        FallbackStep {

    public StateDelegate(@NonNull State state) {
        super(state);
    }

    @Override
    public @NonNull NextStep body(@NonNull Consumer<StateContext> body) {
        this.state.setBody(body);
        return this;
    }

//    @Override
//    public NextStep body(Runnable body) {
//        this.state.setBody(x -> body.run());
//        return this;
//    }


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
