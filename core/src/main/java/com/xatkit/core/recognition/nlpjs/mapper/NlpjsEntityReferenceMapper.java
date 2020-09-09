package com.xatkit.core.recognition.nlpjs.mapper;

import com.xatkit.core.recognition.EntityMapper;
import com.xatkit.intent.CustomEntityDefinition;

import lombok.NonNull;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.xatkit.intent.EntityType.*;

public class NlpjsEntityReferenceMapper extends EntityMapper {

    private Map<String, List<String>> reversedEntities;

    public NlpjsEntityReferenceMapper() {
        super();
        this.registerEntities();
        this.setFallbackEntityMapping("any");
        this.reversedEntities = this.reverseEntityTypes();
    }

    private void registerEntities() {
        this.addEntityMapping(ANY, "any");
        this.addEntityMapping(EMAIL,"email");
        this.addEntityMapping(PHONE_NUMBER,"phonenumber");
        this.addEntityMapping(URL,"url");
        this.addEntityMapping(NUMBER,"number");
        this.addEntityMapping(PERCENTAGE,"percentage");
    }

    @Override
    public void addCustomEntityMapping(CustomEntityDefinition entityDefinition, String concreteEntity) {
        /*
         * Supporting this would imply to populate the DialogFlowEntityMapper when loading an existing agent. There
         * is no need for such feature for now.
         */
        throw new UnsupportedOperationException(MessageFormat.format("{0} does not allow to register custom entity " +
                        "mappings, use getMappingFor(EntityDefinition) to get NLP.js-compatible mapping of {1}",
                this.getClass().getSimpleName(), CustomEntityDefinition.class.getSimpleName()));
    }

    @Override
    protected String getMappingForCustomEntity(CustomEntityDefinition customEntityDefinition) {
        return customEntityDefinition.getName();
    }

    private Map<String, List<String>> reverseEntityTypes() {
        Map<String, List<String>> reversedEntities = new HashMap<>();
        for (Map.Entry<String, String> entityEntry: entities.entrySet()) {
            if (reversedEntities.containsKey(entityEntry.getValue())) {
                List<String> entityTypes = reversedEntities.get(entityEntry.getKey());
                entityTypes.add(entityEntry.getKey());
            }
            else {
                List<String> entityTypes = new ArrayList<>();
                entityTypes.add(entityEntry.getKey());
                reversedEntities.put(entityEntry.getValue(),entityTypes);
            }
        }
        return reversedEntities;
    }

    public List<String> getReversedEntity(@NonNull String nlpjsEntityType) {
        return reversedEntities.get(nlpjsEntityType);
    }


}
