package com.xatkit.core.recognition.nlpjs.model;

import lombok.Data;

import java.util.List;

@Data
public class Entity {
    private String entityName;
    private String type;
    private List<EntityValue> references;

    public Entity(){
    }

    public Entity(String entityName, String type, List<EntityValue> references) {
        this.entityName = entityName;
        this.type = type;
        this.references = references;
    }
}
