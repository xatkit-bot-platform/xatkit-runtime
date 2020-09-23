package com.xatkit.dsl.intent;

import com.xatkit.dsl.entity.EntityDefinitionReferenceProvider;
import com.xatkit.intent.EntityDefinitionReference;
import lombok.NonNull;

public interface IntentContextParameterEntityStep extends IntentDefinitionProvider {

    @NonNull IntentContextParameterStep entity(@NonNull EntityDefinitionReferenceProvider entityReferenceProvider);

    @NonNull IntentContextParameterStep entity(@NonNull EntityDefinitionReference entityReference);
}
