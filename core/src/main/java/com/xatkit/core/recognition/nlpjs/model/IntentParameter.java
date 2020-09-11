package com.xatkit.core.recognition.nlpjs.model;

import lombok.Data;

@Data
public class IntentParameter {

    private String slot;

    public IntentParameter(){}

    public IntentParameter(String slot) {
        this.slot = slot;
    }
}
