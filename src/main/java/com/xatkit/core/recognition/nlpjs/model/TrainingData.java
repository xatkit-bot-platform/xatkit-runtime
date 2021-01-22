package com.xatkit.core.recognition.nlpjs.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.List;

@Data
@Builder
public class TrainingData {

    @NonNull
    private AgentConfig config;

    @NonNull
    private List<Intent> intents;

    private List<Entity> entities;
}
