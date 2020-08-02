package com.xatkit.dsl.state;

import com.xatkit.execution.StateContext;

import java.util.function.Predicate;

public interface WhenStep extends StateProvider {

    MoveToStep when(Predicate<StateContext> condition);

    EventPredicateStep when();
}
