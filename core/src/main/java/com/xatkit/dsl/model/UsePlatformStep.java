package com.xatkit.dsl.model;

import com.xatkit.core.platform.RuntimePlatform;

public interface UsePlatformStep extends ListenToStep {

    UsePlatformStep usePlatform(RuntimePlatform platform);
}
