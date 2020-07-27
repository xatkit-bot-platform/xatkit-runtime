package com.xatkit.dsl.intent;

import com.xatkit.dsl.entity.EntityDefinitionReferenceProvider;
import com.xatkit.intent.EntityDefinitionReference;

public interface ContextParameterEntityStep extends IntentDefinitionProvider {

    ContextParameterStep entity(EntityDefinitionReferenceProvider entityReferenceProvider);

    ContextParameterStep entity(EntityDefinitionReference entityReference);
}
