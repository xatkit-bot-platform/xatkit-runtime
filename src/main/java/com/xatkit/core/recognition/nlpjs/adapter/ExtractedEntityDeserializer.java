package com.xatkit.core.recognition.nlpjs.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.xatkit.core.recognition.nlpjs.model.ExtractedEntity;

import java.lang.reflect.Type;

/**
 * A {@link JsonDeserializer} for NLP.js {@link ExtractedEntity}.
 * <p>
 * This class parses the provided {@link JsonElement} and builds an {@link ExtractedEntity} instance from its
 * attributes. This class is used by the {@link com.xatkit.core.recognition.nlpjs.NlpjsClient} to parse results from
 * the NLP.js server.
 */
public class ExtractedEntityDeserializer implements JsonDeserializer<ExtractedEntity> {

    /**
     * Parses the provided {@code json} and creates an {@link ExtractedEntity} instance from its attributes.
     *
     * @param json    the {@link JsonElement} to parse
     * @param typeOfT the type to create
     * @param context the deserialization context
     * @return the created {@link ExtractedEntity}
     * @throws JsonParseException if a parsing error occurred
     */
    @Override
    public ExtractedEntity deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

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

            if (resolution.has("value") && resolution.get("value").isJsonPrimitive()) {
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