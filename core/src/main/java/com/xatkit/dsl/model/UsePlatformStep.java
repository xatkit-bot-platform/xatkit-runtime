package com.xatkit.dsl.model;

import com.xatkit.core.platform.RuntimePlatform;
import lombok.NonNull;

public interface UsePlatformStep extends ListenToStep {

    @NonNull UsePlatformStep usePlatform(@NonNull RuntimePlatform platform);
}
