package com.xatkit.core.recognition.nlpjs.adapter;

import com.google.gson.*;
import com.xatkit.core.recognition.nlpjs.model.ExtractedEntity;

import java.lang.reflect.Type;

public class ExtractedEntityDeserializer implements JsonDeserializer<ExtractedEntity> {

    @Override
    public ExtractedEntity deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        JsonObject jsonObject = json.getAsJsonObject();
        ExtractedEntity extractedEntity = new ExtractedEntity();

        if (jsonObject.has("accuracy")) {
            extractedEntity.setAccuracy(jsonObject.get("accuracy").getAsFloat());
        }

        if (jsonObject.has("entity")) {
            extractedEntity.setEntity(jsonObject.get("entity").getAsString());
        }

        if (jsonObject.has("sourceText")) {
            extractedEntity.setSourceText(jsonObject.get("sourceText").getAsString());
        }

        if (jsonObject.has("utteranceText")) {
            extractedEntity.setUtteranceText(jsonObject.get("utteranceText").getAsString());
        }

        if (jsonObject.has("type")) {
            String type = jsonObject.get("type").getAsString();
            extractedEntity.setType(type);
            if (type.equals("enum")) {
                extractedEntity.setValue(jsonObject.get("option").getAsString());
            } else if (type.equals("trim")) {
                extractedEntity.setValue(extractedEntity.getSourceText());
            }
        }

        if (jsonObject.has("resolution")) {
            JsonObject resolution = jsonObject.get("resolution").getAsJsonObject();
            if (resolution.has("subtype")) {
                extractedEntity.setSubType(resolution.get("subtype").getAsString());
            }
            if (resolution.has("type")) {
                if (resolution.get("type").getAsString().equals("date") && resolution.has("date")) {
                    extractedEntity.setType("date");
                    extractedEntity.setValue(resolution.get("date").getAsString());
                }
            }

            if (resolution.has("value") &&  resolution.get("value").isJsonPrimitive()) {
                JsonPrimitive jsonPrimitive = resolution.get("value").getAsJsonPrimitive();
                if (jsonPrimitive.isNumber()) {
                    extractedEntity.setValue(jsonPrimitive.getAsNumber());
                } else if (jsonPrimitive.isString()) {
                    extractedEntity.setValue(jsonPrimitive.getAsString());
                }
            }
        }

        return extractedEntity;
        }
    }