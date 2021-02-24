package com.xatkit.core.recognition.processor;

import com.mashape.unirest.http.Unirest;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * The type Detoxify interface.
 */
public class DetoxifyInterface {

    /**
     * The list of all possible toxic attributes associated with a comment.
     */
    public enum AttributeType {
        /**
         * Rude, disrespectful, or unreasonable comment that is likely to make people leave a discussion.
         */
        TOXICITY,
        /**
         * A very hateful, aggressive, disrespectful comment or otherwise very likely to make a user leave a
         * discussion or give up on sharing their perspective. This model is much less sensitive to comments that
         * include positive uses of curse words, for example.
         */
        SEVERE_TOXICITY,
        /**
         * Negative or hateful comments targeting someone because of their identity.
         */
        IDENTITY_HATE,
        /**
         * Insulting, inflammatory, or negative comment towards a person or a group of people.
         */
        INSULT,
        /**
         * Describes an intention to inflict pain, injury, or violence against an individual or group.
         */
        THREAT,
        /**
         * Obscene or vulgar language such as cursing.
         */
        OBSCENE
    }

    private JSONObject request;
    private String attributesPrefix;

    /**
     * Constructor
     *
     * @param attributesPrefix the attributes prefix for the keys of the map they will be stored in
     */
    DetoxifyInterface(@NonNull String attributesPrefix) {
        request = new JSONObject();
        this.attributesPrefix = attributesPrefix;
    }

    /**
     * Analyzes the comment and returns the punctuation for each attribute
     *
     * @param comment the comment
     * @return the hash map with attributes and their respective scores
     */
    public HashMap<String, Double> analyzeRequest(String comment) {
        JSONObject response = new JSONObject();
        HashMap<String, Double> scores = new HashMap<>();
        try {
            for (AttributeType k : AttributeType.values()) {
                scores.put(attributesPrefix + k.toString(), -1.);
            }
            request.put("comment", comment);
            response = Unirest.post("http://localhost:8000/analyzeRequest")
                    .header("Content-Type", "application/json")
                    .body(request)
                    .asJson().getBody().getObject();
            response.keySet();
            Double score;
            for (String k : response.keySet()) {
                score = response.getDouble(k);
                scores.put(attributesPrefix + k, score);
            }
        } catch (Exception e) {
            Log.warn("Detoxify failed to compute toxicity scores");
            System.out.println("DetoxifyResponse=" + response.toString());
            e.printStackTrace();
        }
        return scores;
    }
}
