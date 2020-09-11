package com.xatkit.core.recognition.nlpjs.model;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

@JsonAdapter(EntityType.Adapter.class)
public enum EntityType {

    ENUM("enum"),
    REGEX("REGEX");

    private String value;

    EntityType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    public static EntityType fromValue(String text) {
        for (EntityType b : EntityType.values()) {
            if (String.valueOf(b.value).equals(text)) {
                return b;
            }
        }
        return null;
    }

    public static class Adapter extends TypeAdapter<EntityType> {
        @Override
        public void write(final JsonWriter jsonWriter, final EntityType enumeration) throws IOException {
            jsonWriter.value(enumeration.getValue());
        }

        @Override
        public EntityType read(final JsonReader jsonReader) throws IOException {
            String value = jsonReader.nextString();
            return EntityType.fromValue(String.valueOf(value));
        }
    }
}