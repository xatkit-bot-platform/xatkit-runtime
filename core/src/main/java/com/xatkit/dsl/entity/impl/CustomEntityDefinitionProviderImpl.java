package com.xatkit.dsl.entity.impl;

import com.xatkit.dsl.entity.CustomEntityDefinitionProvider;
import com.xatkit.dsl.entity.EntityDefinitionReferenceProvider;
import com.xatkit.intent.CustomEntityDefinition;
import com.xatkit.intent.CustomEntityDefinitionReference;
import com.xatkit.intent.EntityDefinitionReference;
import com.xatkit.intent.IntentFactory;

// if we don't put the implemnets here we can modulate: when the subclass implements the interface we are good,
// otherwise the method is not visible.
// Not sure what to use here
public class CustomEntityDefinitionProviderImpl<T extends CustomEntityDefinition> implements
        CustomEntityDefinitionProvider, EntityDefinitionReferenceProvider {

    protected T entity;

    public CustomEntityDefinitionProviderImpl(T entity) {
        this.entity = entity;
    }

//    @Override
    public T getEntity() {
        return this.entity;
    }

//    @Override
    public EntityDefinitionReference getEntityReference() {
        CustomEntityDefinitionReference reference = IntentFactory.eINSTANCE.createCustomEntityDefinitionReference();
        reference.setCustomEntity(this.entity);
        return reference;
    }
}
