package com.xatkit.dsl.model;

import com.xatkit.core.platform.io.RuntimeEventProvider;
import lombok.NonNull;

public interface ListenToStep { //extends StateStep {

    @NonNull StateStep listenTo(@NonNull RuntimeEventProvider<?> provider);
}
