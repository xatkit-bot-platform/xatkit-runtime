package com.xatkit.dsl.model;

import com.xatkit.dsl.state.StateProvider;
import lombok.NonNull;

public interface StateStep extends InitStateStep {

    @Deprecated
    @NonNull StateStep useState(@NonNull StateProvider stateProvider);
}
