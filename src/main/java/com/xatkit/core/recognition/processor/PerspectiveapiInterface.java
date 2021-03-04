package com.xatkit.core.recognition.processor;

import com.mashape.unirest.http.Unirest;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;
import org.apache.commons.configuration2.Configuration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * The type PerspectiveAPI interface.
 */
public class PerspectiveapiInterface {

    private static String COMMENT_TYPE = "PLAIN_TEXT";

    private static double UNSET_SCORE = -1.;

    private PerspectiveapiConfiguration configuration;

    private ToxicityLabel[] requestedAttributes;
    private Map<String, ToxicityLabel[]> languageAttributes;

    /**
     * TODO
     * Constructor
     *
//     * @param apiKey           the api key
//     * @param language         the language
//     * @param doNotStore       if true then data will not be stored to PerspectiveAPI. Default is false
//     * @param clientToken      the client token
//     * @param sessionId        the session id
//     * @param attributesPrefix the attributes prefix for the keys of the map they will be stored in
     */
    PerspectiveapiInterface(@NonNull Configuration baseConfiguration) {
        this.configuration = new PerspectiveapiConfiguration(baseConfiguration);

        languageAttributes = new HashMap<>();
        languageAttributes.put("en", new ToxicityLabel[] {
                ToxicityLabel.TOXICITY,
                ToxicityLabel.SEVERE_TOXICITY,
                ToxicityLabel.TOXICITY_FAST,
                ToxicityLabel.IDENTITY_ATTACK,
                ToxicityLabel.INSULT,
                ToxicityLabel.PROFANITY,
                ToxicityLabel.THREAT,
                ToxicityLabel.SEXUALLY_EXPLICIT,
                ToxicityLabel.FLIRTATION,
                ToxicityLabel.ATTACK_ON_AUTHOR,
                ToxicityLabel.ATTACK_ON_COMMENTER,
                ToxicityLabel.INCOHERENT,
                ToxicityLabel.INFLAMMATORY,
                ToxicityLabel.LIKELY_TO_REJECT,
                ToxicityLabel.OBSCENE,
                ToxicityLabel.SPAM,
                ToxicityLabel.UNSUBSTANTIAL
        });
        languageAttributes.put("es", new ToxicityLabel[] {
                ToxicityLabel.TOXICITY,
                ToxicityLabel.SEVERE_TOXICITY,
                ToxicityLabel.IDENTITY_ATTACK_EXPERIMENTAL,
                ToxicityLabel.INSULT_EXPERIMENTAL,
                ToxicityLabel.PROFANITY_EXPERIMENTAL,
                ToxicityLabel.THREAT_EXPERIMENTAL
        });
    }

    /**
     * Analyzes the comment and returns the punctuation for each attribute
     *
     * @param comment the comment
     * @return the hash map with attributes and their respective scores
     */
    public Map<String, Double> analyzeRequest(String comment) {
        JSONObject response = new JSONObject();
        HashMap<String, Double> scores = new HashMap<>();
        for (ToxicityLabel k : ToxicityLabel.values()) {
            scores.put(k.toString(), UNSET_SCORE);
        }
        try {
            JSONObject request = this.createJSONObjectRequest(comment);
            response = Unirest.post("https://commentanalyzer.googleapis"
                    + ".com/v1alpha1/comments:analyze?key=" + configuration.getApiKey())
                    .header("Content-Type", "application/json")
                    .body(request)
                    .asJson().getBody().getObject();
            JSONObject attributes = response.getJSONObject("attributeScores");
            for (String k : attributes.keySet()) {
                Double score = attributes.getJSONObject(k).getJSONObject("summaryScore").getDouble("value");
                scores.put(k, score);
            }
        } catch (Exception e) {
            // FIXME JSON logging doesn't work properly
            Log.warn(e, "PerspectiveAPI failed to compute toxicity scores");
            System.out.println("PerspectiveAPIResponse=" + response.toString());
        }
        return scores;
    }

    /**
     * Creates the JSON object used to make the request to PerspectiveAPI
     */
    private JSONObject createJSONObjectRequest(String commentText) {

        JSONObject request = new JSONObject();
        JSONObject comment = new JSONObject();
        comment.put("text", commentText);
        comment.put("type", COMMENT_TYPE);
        JSONObject requestedAttributes = new JSONObject();
        ToxicityLabel[] requestedAttributesList = languageAttributes.get(configuration.getLanguage());
        for (ToxicityLabel a : requestedAttributesList) {
            /*
             * Each requested attribute required a JSON object (with optional properties). We don't use any of them
             * but still need to create an empty object.
             */
            requestedAttributes.put(a.toString(), (Map<?, ?>) null);
        }
        JSONArray languages = new JSONArray();
        languages.put(configuration.getLanguage());

        request.put("comment", comment);
        request.put("requestedAttributes", requestedAttributes);
        request.put("languages", languages);
        request.put("doNotStore", configuration.isDoNotStore());
        request.put("clientToken", configuration.getClientToken());
        request.put("sessionId", configuration.getSessionId());
        return request;
    }

    /**
     * Gets language attributes. For testing purposes
     *
     * @return the language attributes
     */
    public Map<String, ToxicityLabel[]> getLanguageAttributes() {
        return languageAttributes;
    }
}