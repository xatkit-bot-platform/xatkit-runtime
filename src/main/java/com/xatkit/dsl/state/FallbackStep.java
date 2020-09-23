package com.xatkit.dsl.state;

import com.xatkit.execution.StateContext;

import java.util.function.Consumer;

public interface FallbackStep extends StateProvider {

    StateProvider fallback(Consumer<StateContext> fallback);
}
