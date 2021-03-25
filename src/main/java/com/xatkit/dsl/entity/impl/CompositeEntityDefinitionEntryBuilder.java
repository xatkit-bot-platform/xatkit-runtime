package com.xatkit.dsl.entity.impl;

import com.xatkit.dsl.entity.CompositeEntryFragmentStep;
import com.xatkit.dsl.entity.CustomEntityDefinitionProvider;
import com.xatkit.dsl.entity.EntityDefinitionReferenceProvider;
import com.xatkit.intent.CompositeEntityDefinition;
import com.xatkit.intent.CompositeEntityDefinitionEntry;
import com.xatkit.intent.EntityDefinitionReference;
import com.xatkit.intent.EntityTextFragment;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.LiteralTextFragment;
import lombok.NonNull;

public class CompositeEntityDefinitionEntryBuilder extends CompositeEntityDefinitionBuilder implements
        CompositeEntryFragmentStep,
        CustomEntityDefinitionProvider {

    private CompositeEntityDefinitionEntry entry;

    public CompositeEntityDefinitionEntryBuilder(CompositeEntityDefinition parent) {
        this.entity = parent;
        this.entry = IntentFactory.eINSTANCE.createCompositeEntityDefinitionEntry();
        /*
         * Add the entry right now because we don't know what will be the last call to define it (we can have
         * multiple fragments in a composite entity entry).
         */
        this.entity.getEntries().add(this.entry);
    }

    @Override
    public @NonNull CompositeEntryFragmentStep text(@NonNull String text) {
        LiteralTextFragment fragment = IntentFactory.eINSTANCE.createLiteralTextFragment();
        fragment.setValue(text);
        this.entry.getFragments().add(fragment);
        return this;
    }

    @Override
    public @NonNull CompositeEntryFragmentStep entity(@NonNull EntityDefinitionReferenceProvider entityReference) {
        return this.entity(entityReference.getEntityReference());
    }

    @Override
    public @NonNull CompositeEntryFragmentStep entity(@NonNull EntityDefinitionReference entityReference) {
        EntityTextFragment fragment = IntentFactory.eINSTANCE.createEntityTextFragment();
        fragment.setEntityReference(entityReference);
        this.entry.getFragments().add(fragment);
        return this;
    }
}
