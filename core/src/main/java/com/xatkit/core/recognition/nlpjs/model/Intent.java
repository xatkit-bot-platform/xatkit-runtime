package com.xatkit.core.recognition.nlpjs.model;

import lombok.Data;

import java.util.List;

@Data
public class Intent {

    private String intentName;
    private List<IntentExample> examples;
    private List<IntentParameter> parameters;

    public Intent(){}

    public Intent(String intentName, List<IntentExample> examples, List<IntentParameter> parameters) {
        this.intentName = intentName;
        this.examples = examples;
        this.parameters = parameters;
    }
}
