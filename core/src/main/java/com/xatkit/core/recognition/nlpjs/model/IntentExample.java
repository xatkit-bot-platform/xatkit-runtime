package com.xatkit.core.recognition.nlpjs.model;

import lombok.Data;

@Data
public class IntentExample {
    private String userSays;

    public IntentExample(){}

    public IntentExample(String userSays) {
        this.userSays = userSays;
    }
}
