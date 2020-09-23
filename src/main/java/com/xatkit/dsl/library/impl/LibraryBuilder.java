package com.xatkit.dsl.library.impl;

import com.xatkit.dsl.entity.CustomEntityDefinitionProvider;
import com.xatkit.dsl.intent.IntentDefinitionProvider;
import com.xatkit.dsl.library.EntityStep;
import com.xatkit.dsl.library.IntentStep;
import com.xatkit.intent.IntentFactory;
import lombok.NonNull;

public class LibraryBuilder extends LibraryProviderImpl implements EntityStep, IntentStep {

    public LibraryBuilder() {
        this.library = IntentFactory.eINSTANCE.createLibrary();
    }

    public @NonNull LibraryBuilder name(@NonNull String name) {
        this.library.setName(name);
        return this;
    }

    @Override
    public @NonNull EntityStep entity(@NonNull CustomEntityDefinitionProvider entityProvider) {
        this.library.getCustomEntities().add(entityProvider.getEntity());
        return this;
    }

    @Override
    public @NonNull IntentStep intent(@NonNull IntentDefinitionProvider intentProvider) {
        this.library.getEventDefinitions().add(intentProvider.getIntentDefinition());
        return this;
    }
}
