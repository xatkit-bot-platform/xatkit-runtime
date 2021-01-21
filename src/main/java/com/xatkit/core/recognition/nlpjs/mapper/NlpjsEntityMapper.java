package com.xatkit.core.recognition.nlpjs.mapper;

import com.xatkit.core.recognition.IntentRecognitionProviderException;
import com.xatkit.core.recognition.nlpjs.model.Entity;
import com.xatkit.core.recognition.nlpjs.model.EntityType;
import com.xatkit.core.recognition.nlpjs.model.EntityValue;
import com.xatkit.intent.BaseEntityDefinition;
import com.xatkit.intent.CompositeEntityDefinition;
import com.xatkit.intent.CustomEntityDefinition;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.MappingEntityDefinition;
import com.xatkit.intent.MappingEntityDefinitionEntry;
import lombok.NonNull;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class NlpjsEntityMapper {

    public Entity mapEntiyDefinition(@NonNull EntityDefinition entityDefinition)
            throws IntentRecognitionProviderException {
        if (entityDefinition instanceof BaseEntityDefinition) {
            throw new IllegalArgumentException(MessageFormat.format("Cannot map the provided {0} {1}. Base entities "
                            + "are already mapped in NLP.js", EntityDefinition.class.getSimpleName(),
                    entityDefinition.toString()));
        } else if (entityDefinition instanceof CustomEntityDefinition) {
            return mapCustomEntityDefinition((CustomEntityDefinition) entityDefinition);
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Cannot register the provided {0}, unsupported "
                            + "{1}", entityDefinition.getClass().getSimpleName(),
                    EntityDefinition.class.getSimpleName()));
        }
    }

    private Entity mapCustomEntityDefinition(@NonNull CustomEntityDefinition customEntityDefinition)
            throws IntentRecognitionProviderException {
        String entityName = customEntityDefinition.getName();
        Entity.EntityBuilder builder = Entity.builder();
        builder.entityName(entityName);
        if (customEntityDefinition instanceof MappingEntityDefinition) {
            MappingEntityDefinition mappingEntityDefinition = (MappingEntityDefinition) customEntityDefinition;
            List<EntityValue> entityValues = createReferencesFromMapping(mappingEntityDefinition);
            builder.type(EntityType.ENUM).references(entityValues);

        } else if (customEntityDefinition instanceof CompositeEntityDefinition) {
            throw new IntentRecognitionProviderException(MessageFormat.format("Cannot register the entity {0}. "
                    + "Composite entities are not supported by NLP.js", customEntityDefinition));
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Cannot register the provided {0}, unsupported {1}",
                    customEntityDefinition.getClass().getSimpleName(), EntityDefinition.class.getSimpleName()));
        }
        return builder.build();
    }

    private List<EntityValue> createReferencesFromMapping(MappingEntityDefinition mappingEntityDefinition) {
        List<EntityValue> entityValues = new ArrayList<>();
        for (MappingEntityDefinitionEntry entry : mappingEntityDefinition.getEntries()) {
            EntityValue.EntityValueBuilder builder = EntityValue.builder()
                    .value(entry.getReferenceValue())
                    .synonyms(entry.getSynonyms())
                    .synonym(entry.getReferenceValue());
            entityValues.add(builder.build());
        }
        return entityValues;
    }
}
