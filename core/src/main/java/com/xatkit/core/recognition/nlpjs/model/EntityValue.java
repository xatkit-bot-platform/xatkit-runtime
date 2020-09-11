package com.xatkit.core.recognition.nlpjs.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EntityValue {

    private String value;

    private List<String> synonyms;

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String _value;
        private List<String> _synonyms;

        public Builder() {
            this._synonyms = new ArrayList<>();
        }

        public Builder value(String value) {
            this._value = value;
            return this;
        }

        public Builder synonyms(List<String> _synonyms) {
            this._synonyms = _synonyms;
            return this;
        }

        public Builder addSynonym(String synonym) {
            this._synonyms.add(synonym);
            return this;
        }

        public EntityValue build() {
            EntityValue entityValue = new EntityValue();
            entityValue.setValue(_value);
            entityValue.setSynonyms(_synonyms);
            return entityValue;
        }
    }
}
