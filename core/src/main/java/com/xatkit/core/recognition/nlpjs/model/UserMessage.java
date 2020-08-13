package com.xatkit.core.recognition.nlpjs.model;

import lombok.Data;

@Data
public class UserMessage {
    private String message;

    public UserMessage() { }

    public UserMessage(String message) {
        this.message = message;
    }
}
