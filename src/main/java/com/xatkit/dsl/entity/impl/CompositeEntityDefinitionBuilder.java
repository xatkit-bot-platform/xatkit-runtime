package com.xatkit.dsl.entity.impl;

import com.xatkit.dsl.entity.CompositeEntryFragmentStep;
import com.xatkit.dsl.entity.CompositeEntryStep;
import com.xatkit.intent.CompositeEntityDefinition;
import com.xatkit.intent.IntentFactory;
import lombok.NonNull;

public class CompositeEntityDefinitionBuilder extends CustomEntityDefinitionProviderImpl<CompositeEntityDefinition> implements CompositeEntryStep {

    public CompositeEntityDefinitionBuilder() {
        this.entity = IntentFactory.eINSTANCE.createCompositeEntityDefinition();
    }

    @Override
    public @NonNull CompositeEntryFragmentStep entry() {
        return new CompositeEntityDefinitionEntryBuilder(this.entity);
    }
}
