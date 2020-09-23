package com.xatkit.dsl.intent;

import com.xatkit.intent.EventDefinition;
import lombok.NonNull;

public interface EventDefinitionProvider {

    @NonNull EventDefinition getEventDefinition();
}
