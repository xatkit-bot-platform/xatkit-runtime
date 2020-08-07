package com.xatkit.dsl.intent;

import lombok.NonNull;

public interface EventContextParameterStep extends EventContextStep {

    @NonNull EventContextParameterStep parameter(@NonNull String name);
}
