package com.xatkit.core.recognition.nlpjs.model;

import lombok.Data;

@Data
public class Agent {

    private AgentStatus status;

    public Agent() {
    }

    public Agent(AgentStatus status) {
        this.status = status;
    }

}
