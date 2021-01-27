package com.xatkit.core.recognition.nlpjs.model;


import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class AgentInit {

    private String agentId;

    private String language;

}
