package com.xatkit.core.recognition.nlpjs.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Entity {

    private String entityName;

    private EntityType type;

    private List<EntityValue> references;

    private List<String> afterLast;

    private List<String> beforeLast;

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private String _entityName;

        private EntityType _type;

        private List<EntityValue> _references;

        private List<String> _afterLast;

        private List<String> _beforeLast;


        public Builder() {
            this._references = new ArrayList<>();
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

        public Builder afterLast(List<String> afterLastValues) {
            _afterLast = afterLastValues;
            return this;
        }

        public Builder beforeLast(List<String> beforeLast) {
            _beforeLast = beforeLast;
            return this;
        }

        public Builder addReference(EntityValue entityValue) {
            _references.add(entityValue);
            return this;
        }

        public Entity build() {
            Entity entity = new Entity();
            entity.setEntityName(_entityName);
            entity.setType(_type);
            entity.setReferences(_references);
            entity.setAfterLast(_afterLast);
            entity.setBeforeLast(_beforeLast);

            return entity;
        }

    }
}
