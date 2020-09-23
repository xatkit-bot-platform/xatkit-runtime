package com.xatkit.core.recognition.nlpjs.model;

import lombok.Data;

@Data
public class AgentConfig {

    private String language;
    private boolean clean;

    public AgentConfig() {
    }

    public AgentConfig(String language, boolean clean) {
        this.language = language;
        this.clean = clean;
    }
}
