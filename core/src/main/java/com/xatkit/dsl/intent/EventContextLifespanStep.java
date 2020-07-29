package com.xatkit.dsl.intent;

import lombok.NonNull;

public interface EventContextLifespanStep extends EventContextParameterStep {

    @NonNull EventContextParameterStep lifespan(int lifespan);
}
