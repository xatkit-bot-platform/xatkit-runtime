package com.xatkit.dsl.state;

import com.xatkit.dsl.intent.EventDefinitionProvider;
import com.xatkit.dsl.intent.IntentDefinitionProvider;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.IntentDefinition;

public interface EventPredicateStep {

    MoveToStep intentIs(IntentDefinitionProvider intentDefinitionProvider);

    MoveToStep intentIs(IntentDefinition intentDefinition);

    MoveToStep eventIs(EventDefinitionProvider eventDefinitionProvider);

    MoveToStep eventIs(EventDefinition eventDefinition);
}
