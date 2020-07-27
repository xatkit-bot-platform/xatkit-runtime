package com.xatkit.dsl.entity.impl;

import com.xatkit.dsl.entity.CompositeEntryFragmentStep;
import com.xatkit.dsl.entity.CompositeEntryStep;
import com.xatkit.intent.CompositeEntityDefinition;
import com.xatkit.intent.CompositeEntityDefinitionEntry;
import com.xatkit.intent.IntentFactory;

public class CompositeEntityDefinitionDelegate extends CustomEntityDefinitionProviderImpl<CompositeEntityDefinition> implements CompositeEntryStep {

    public CompositeEntityDefinitionDelegate(CompositeEntityDefinition entity) {
        super(entity);
    }

    @Override
    public CompositeEntryFragmentStep entry() {
        CompositeEntityDefinitionEntry entry = IntentFactory.eINSTANCE.createCompositeEntityDefinitionEntry();
        this.entity.getEntries().add(entry);
        return new CompositeEntityDefinitionEntryDelegate(this.entity, entry);
    }
}
