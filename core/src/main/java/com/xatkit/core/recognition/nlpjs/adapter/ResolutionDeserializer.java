package com.xatkit.core.recognition.nlpjs.adapter;

import com.google.gson.*;
import com.xatkit.core.recognition.nlpjs.model.Resolution;

import java.lang.reflect.Type;

public class ResolutionDeserializer implements JsonDeserializer<Resolution> {

    @Override
    public Resolution deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        JsonObject jsonObject = json.getAsJsonObject();
        Resolution resolution = new Resolution();

        if (jsonObject.has("subtype"))
            resolution.setSubtype(jsonObject.get("subtype").getAsString());

        if (jsonObject.has("type") && jsonObject.get("type").getAsString().equals("date")
        && jsonObject.has("date")) {
            resolution.setType("date");
            resolution.setValue(jsonObject.get("date").getAsString());
        }

        if (jsonObject.has("value") &&  jsonObject.get("value").isJsonPrimitive()) {
                JsonPrimitive jsonPrimitive = jsonObject.get("value").getAsJsonPrimitive();
                if (jsonPrimitive.isNumber())
                    resolution.setValue(jsonPrimitive.getAsNumber());
                else if (jsonPrimitive.isString())
                    resolution.setValue(jsonPrimitive.getAsString());
            }
        return resolution;
        }


    }