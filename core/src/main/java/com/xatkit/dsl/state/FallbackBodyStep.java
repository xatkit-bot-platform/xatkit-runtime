package com.xatkit.dsl.state;

import com.xatkit.execution.StateContext;

import java.util.function.Consumer;

public interface FallbackBodyStep extends StateProvider {

    StateProvider body(Consumer<StateContext> body);
}
