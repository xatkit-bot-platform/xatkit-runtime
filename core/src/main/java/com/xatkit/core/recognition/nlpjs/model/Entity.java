package com.xatkit.core.recognition.nlpjs.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Entity {
    private String entityName;
    private EntityType type;
    private List<EntityValue> references;

    public static Builder newBuilder() {
        return new Builder();
    }


    public static class Builder {
        private String _entityName;
        private EntityType _type;
        private List<EntityValue> _references;

        public Builder() {
            this. _references = new ArrayList<>();
        }

        public Builder entityName(String entityName) {
            _entityName = entityName;
            return this;
        }

        public Builder type(EntityType type) {
            _type = type;
            return this;
        }

        public Builder references(List<EntityValue> entityValues) {
            _references = entityValues;
            return this;
        }

        public Builder addReference(EntityValue entityValue) {
            _references.add(entityValue);
            return this;
        }

        public Entity build(){
            Entity entity = new Entity();
            entity.setEntityName(_entityName);
            entity.setType(_type);
            entity.setReferences(_references);

            return entity;
        }

    }
}
