package edu.uoc.som.jarvis.test.util;

import edu.uoc.som.jarvis.intent.*;

public class ElementFactory {

    public static EntityDefinitionReference createBaseEntityDefinitionReference(EntityType entityType) {
        BaseEntityDefinition entityDefinition = IntentFactory.eINSTANCE.createBaseEntityDefinition();
        entityDefinition.setEntityType(entityType);
        BaseEntityDefinitionReference reference = IntentFactory.eINSTANCE.createBaseEntityDefinitionReference();
        reference.setBaseEntity(entityDefinition);
        return reference;
    }
}
