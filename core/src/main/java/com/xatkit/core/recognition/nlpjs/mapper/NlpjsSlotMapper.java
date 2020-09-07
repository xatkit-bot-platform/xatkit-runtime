package com.xatkit.core.recognition.nlpjs.mapper;

import com.xatkit.core.recognition.EntityMapper;
import com.xatkit.intent.CustomEntityDefinition;

import java.text.MessageFormat;

import static com.xatkit.intent.EntityType.*;

public class NlpjsSlotMapper extends EntityMapper {

    public NlpjsSlotMapper() {
        super();
        this.registerEntities();
        this.setFallbackEntityMapping("any");
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

}
