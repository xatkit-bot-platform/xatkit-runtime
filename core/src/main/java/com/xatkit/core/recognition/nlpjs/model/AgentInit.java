
package com.xatkit.core.recognition.nlpjs.model;


import lombok.Data;


@Data
public class AgentInit {

    private String agentId;

    public AgentInit(){
    }

    public AgentInit(String agentId){
        this.agentId = agentId;
  }

}
