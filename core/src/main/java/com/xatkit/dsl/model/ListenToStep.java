package com.xatkit.dsl.model;

import com.xatkit.core.platform.io.RuntimeEventProvider;

public interface ListenToStep extends StateStep {

    ListenToStep listenTo(RuntimeEventProvider<?> provider);
}
