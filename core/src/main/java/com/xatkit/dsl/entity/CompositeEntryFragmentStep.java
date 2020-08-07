package com.xatkit.dsl.entity;

import com.xatkit.intent.EntityDefinitionReference;
import lombok.NonNull;

public interface CompositeEntryFragmentStep extends CompositeEntryStep, CustomEntityDefinitionProvider {

    @NonNull CompositeEntryFragmentStep text(@NonNull String text);

    @NonNull CompositeEntryFragmentStep entity(@NonNull EntityDefinitionReferenceProvider entityReference);

    @NonNull CompositeEntryFragmentStep entity(@NonNull EntityDefinitionReference entityReference);
}
