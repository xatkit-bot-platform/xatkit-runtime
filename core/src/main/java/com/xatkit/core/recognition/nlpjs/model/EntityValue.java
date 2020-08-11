package com.xatkit.core.recognition.nlpjs.model;

import lombok.Data;

import java.util.List;

@Data
public class EntityValue {
    private String value;
    private List<String> synonyms;

    public EntityValue(){
    }

    public EntityValue(String value, List<String> synonyms) {
        this.value = value;
        this.synonyms = synonyms;
    }
}
