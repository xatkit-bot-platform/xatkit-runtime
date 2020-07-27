package com.xatkit.dsl.state;

import com.xatkit.execution.StateContext;

import java.util.function.Predicate;

public interface OptionalWhenStep extends WhenStep, FallbackStep {

    MoveToStep when(Predicate<StateContext> condition);
}
