package com.xatkit.core.recognition.nlpjs.model;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

@JsonAdapter(AgentStatus.Adapter.class)
public enum AgentStatus {

    NEW("new"),
    TRAINING("training"),
    READY("ready");

    private String value;

    AgentStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    public static AgentStatus fromValue(String text) {
        for (AgentStatus b : AgentStatus.values()) {
            if (String.valueOf(b.value).equals(text)) {
                return b;
            }
        }
        return null;
    }

    public static class Adapter extends TypeAdapter<AgentStatus> {
        
        @Override
        public void write(final JsonWriter jsonWriter, final AgentStatus enumeration) throws IOException {
            jsonWriter.value(enumeration.getValue());
        }

        @Override
        public AgentStatus read(final JsonReader jsonReader) throws IOException {
            String value = jsonReader.nextString();
            return AgentStatus.fromValue(String.valueOf(value));
        }
    }
}