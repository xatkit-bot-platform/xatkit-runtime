package com.xatkit.dsl.state;

import com.xatkit.execution.StateContext;

import java.util.function.Consumer;

/**
 * Commodity interface (= BodyStep) to have clear usage of the DSL:
 * StateVar myState = state("MyState")
 */
public interface StateVar extends NextStep {

    NextStep body(Consumer<StateContext> body);
}
