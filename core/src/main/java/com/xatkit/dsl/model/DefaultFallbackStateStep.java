package com.xatkit.dsl.model;

import com.xatkit.dsl.state.StateProvider;
import com.xatkit.execution.State;
import lombok.NonNull;

public interface DefaultFallbackStateStep {

    @NonNull ExecutionModelProvider defaultFallbackState(@NonNull StateProvider stateProvider);

    @NonNull ExecutionModelProvider defaultFallbackState(@NonNull State state);
}
