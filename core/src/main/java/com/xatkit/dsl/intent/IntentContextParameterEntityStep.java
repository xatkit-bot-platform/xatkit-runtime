package com.xatkit.dsl.intent;

import com.xatkit.dsl.entity.EntityDefinitionReferenceProvider;
import com.xatkit.intent.EntityDefinitionReference;

public interface IntentContextParameterEntityStep extends IntentDefinitionProvider {

    IntentContextParameterStep entity(EntityDefinitionReferenceProvider entityReferenceProvider);

    IntentContextParameterStep entity(EntityDefinitionReference entityReference);
}
