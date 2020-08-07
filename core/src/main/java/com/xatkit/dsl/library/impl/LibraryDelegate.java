package com.xatkit.dsl.library.impl;

import com.xatkit.dsl.entity.CustomEntityDefinitionProvider;
import com.xatkit.dsl.intent.IntentDefinitionProvider;
import com.xatkit.dsl.library.EntityStep;
import com.xatkit.dsl.library.IntentStep;
import com.xatkit.intent.Library;
import lombok.NonNull;

public class LibraryDelegate extends LibraryProviderImpl implements EntityStep, IntentStep {

    public LibraryDelegate(@NonNull Library library) {
        super(library);
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
