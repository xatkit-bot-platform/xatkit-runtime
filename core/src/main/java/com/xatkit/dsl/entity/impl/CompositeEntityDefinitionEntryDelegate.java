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

public class CompositeEntityDefinitionEntryDelegate extends CompositeEntityDefinitionDelegate implements
        CompositeEntryFragmentStep,
        CustomEntityDefinitionProvider {

    private CompositeEntityDefinitionEntry entry;

    public CompositeEntityDefinitionEntryDelegate(CompositeEntityDefinition entity,
                                                  CompositeEntityDefinitionEntry entry) {
        super(entity);
        this.entry = entry;
    }

    @Override
    public CompositeEntryFragmentStep text(String text) {
        LiteralTextFragment fragment = IntentFactory.eINSTANCE.createLiteralTextFragment();
        fragment.setValue(text);
        this.entry.getFragments().add(fragment);
        return this;
    }

    @Override
    public CompositeEntryFragmentStep entity(EntityDefinitionReferenceProvider entityReferenceProvider) {
        return this.entity(entityReferenceProvider.getEntityReference());
    }

    @Override
    public CompositeEntryFragmentStep entity(EntityDefinitionReference entityReference) {
        EntityTextFragment fragment = IntentFactory.eINSTANCE.createEntityTextFragment();
        fragment.setEntityReference(entityReference);
        this.entry.getFragments().add(fragment);
        return this;
    }
}
