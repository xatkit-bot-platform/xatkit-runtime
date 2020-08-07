package com.xatkit.dsl.intent;

import lombok.NonNull;

public interface IntentContextLifespanStep extends IntentContextParameterStep, EventDefinitionProvider {

    @NonNull IntentContextParameterStep lifespan(int lifespan);
}
