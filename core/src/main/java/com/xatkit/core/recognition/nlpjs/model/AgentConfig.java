package com.xatkit.core.recognition.nlpjs.model;

import lombok.Data;

@Data
public class AgentConfig {
    private String language;

    public AgentConfig(){

    }
    public AgentConfig(String language){
        this.language = language;
    }
}
