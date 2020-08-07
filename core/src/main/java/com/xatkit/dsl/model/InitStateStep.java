package com.xatkit.dsl.model;

import com.xatkit.dsl.state.StateProvider;
import com.xatkit.execution.State;
import lombok.NonNull;

public interface InitStateStep {

    @NonNull DefaultFallbackStateStep initState(@NonNull StateProvider stateProvider);

    @NonNull DefaultFallbackStateStep initState(@NonNull State state);
}
