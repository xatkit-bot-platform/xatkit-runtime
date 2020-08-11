package com.xatkit.core.recognition.nlpjs.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TrainingData {

    private AgentConfig config;
    private List<Intent> intents;
    private List<Entity> entities;


    public TrainingData() {
        intents = new ArrayList<>();
        entities = new ArrayList<>();
    }

    public TrainingData(AgentConfig config, List<Intent> intents, List<Entity> entities) {
        this.config = config;
        this.intents = intents;
        this.entities = entities;
    }
}
