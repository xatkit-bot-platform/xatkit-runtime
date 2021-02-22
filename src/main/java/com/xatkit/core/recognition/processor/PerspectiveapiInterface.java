package com.xatkit.core.recognition.processor;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.NonNull;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;

/**
 * The type PerspectiveAPI interface.
 */
public class PerspectiveapiInterface {

    /**
     * The list of all possible toxic attributes associated with a comment. Each attribute is available in a subset
     * of languages. See
     * <a href="https://support.perspectiveapi.com/s/about-the-api-attributes-and-languages">
     *     PerspectiveAPI Attributes & Languages</a> for a complete list of languages available for each attribute.
     */
    public enum AttributeType {

        /**
         * Rude, disrespectful, or unreasonable comment that is likely to make people leave a discussion.
         */
        TOXICITY,
        /**
         * Rude, disrespectful, or unreasonable comment that is likely to make people leave a discussion.
         */
        TOXICITY_EXPERIMENTAL,
        /**
         * A very hateful, aggressive, disrespectful comment or otherwise very likely to make a user leave a
         * discussion or give up on sharing their perspective. This model is much less sensitive to comments that
         * include positive uses of curse words, for example. A labelled dataset and details of the methodology can be
         * found in the same toxicity dataset that is available for the toxicity model.
         */
        SEVERE_TOXICITY,
        /**
         * A very hateful, aggressive, disrespectful comment or otherwise very likely to make a user leave a
         * discussion or give up on sharing their perspective. This model is much less sensitive to comments that
         * include positive uses of curse words, for example. A labelled dataset and details of the methodology can be
         * found in the same toxicity dataset that is available for the toxicity model.
         */
        SEVERE_TOXICITY_EXPERIMENTAL,
        /**
         * This model is similar to the TOXICITY model, but has lower latency and lower accuracy in its predictions.
         * Unlike TOXICITY, this model returns summary scores as well as span scores. This model uses character-level
         * n-grams fed into a logistic regression, a method that has been surprisingly effective at detecting abusive
         * language.
         */
        TOXICITY_FAST,
        /**
         * Negative or hateful comments targeting someone because of their identity.
         */
        IDENTITY_ATTACK,
        /**
         * Negative or hateful comments targeting someone because of their identity.
         */
        IDENTITY_ATTACK_EXPERIMENTAL,
        /**
         * Insulting, inflammatory, or negative comment towards a person or a group of people.
         */
        INSULT,
        /**
         * Insulting, inflammatory, or negative comment towards a person or a group of people.
         */
        INSULT_EXPERIMENTAL,
        /**
         * Swear words, curse words, or other obscene or profane language.
         */
        PROFANITY,
        /**
         * Swear words, curse words, or other obscene or profane language.
         */
        PROFANITY_EXPERIMENTAL,
        /**
         * Describes an intention to inflict pain, injury, or violence against an individual or group.
         */
        THREAT,
        /**
         * Describes an intention to inflict pain, injury, or violence against an individual or group.
         */
        THREAT_EXPERIMENTAL,
        /**
         * Contains references to sexual acts, body parts, or other lewd content.
         */
        SEXUALLY_EXPLICIT,
        /**
         * Pickup lines, complimenting appearance, subtle sexual innuendos, etc.
         */
        FLIRTATION,
        /* New York Times models below */
        /**
         * Attack on the author of an article or post.
         */
        ATTACK_ON_AUTHOR,
        /**
         * Attack on fellow commenter.
         */
        ATTACK_ON_COMMENTER,
        /**
         * Difficult to understand, nonsensical.
         */
        INCOHERENT,
        /**
         * Intending to provoke or inflame.
         */
        INFLAMMATORY,
        /**
         * Overall measure of the likelihood for the comment to be rejected according to the NYT's moderation.
         */
        LIKELY_TO_REJECT,
        /**
         * Obscene or vulgar language such as cursing.
         */
        OBSCENE,
        /**
         * Irrelevant and unsolicited commercial content.
         */
        SPAM,
        /**
         * Trivial or short comments.
         */
        UNSUBSTANTIAL;
    }

    private String apiKey;
    private String commentText;
    private String commentType = "PLAIN_TEXT";
    private AttributeType[] requestedAttributes;
    private String language;
    private Boolean doNotStore;
    private String clientToken;
    private String sessionId;
    private JSONObject request;
    private Map<String, AttributeType[]> languageAttributes;
    private String attributesPrefix;

    /**
     * Constructor
     *
     * @param apiKey           the api key
     * @param language         the language
     * @param doNotStore       if true then data will not be stored to PerspectiveAPI. Default is false
     * @param clientToken      the client token
     * @param sessionId        the session id
     * @param attributesPrefix the attributes prefix for the keys of the map they will be stored in
     */
    PerspectiveapiInterface(@NonNull String apiKey, String language, Boolean doNotStore, String clientToken,
                            String sessionId, String attributesPrefix) {

        this.apiKey = apiKey;
        this.language = language != null ? language : "en";
        this.doNotStore = doNotStore != null ? doNotStore : false;
        this.clientToken = clientToken;
        this.sessionId = sessionId;
        this.attributesPrefix = attributesPrefix;
        languageAttributes = new HashMap<>();
        languageAttributes.put("en", new AttributeType[] {
                AttributeType.TOXICITY,
                AttributeType.SEVERE_TOXICITY,
                AttributeType.TOXICITY_FAST,
                AttributeType.IDENTITY_ATTACK,
                AttributeType.INSULT,
                AttributeType.PROFANITY,
                AttributeType.THREAT,
                AttributeType.SEXUALLY_EXPLICIT,
                AttributeType.FLIRTATION,
                AttributeType.ATTACK_ON_AUTHOR,
                AttributeType.ATTACK_ON_COMMENTER,
                AttributeType.INCOHERENT,
                AttributeType.INFLAMMATORY,
                AttributeType.LIKELY_TO_REJECT,
                AttributeType.OBSCENE,
                AttributeType.SPAM,
                AttributeType.UNSUBSTANTIAL
        });
        languageAttributes.put("es", new AttributeType[] {
                AttributeType.TOXICITY,
                AttributeType.SEVERE_TOXICITY,
                AttributeType.IDENTITY_ATTACK_EXPERIMENTAL,
                AttributeType.INSULT_EXPERIMENTAL,
                AttributeType.PROFANITY_EXPERIMENTAL,
                AttributeType.THREAT_EXPERIMENTAL
        });
        this.setRequestedAttributes();
    }

    /**
     * Sets the attributes available for the chosen language
     */
    private AttributeType[] setRequestedAttributes() {
        if (language.equals("es")) {
            requestedAttributes = languageAttributes.get("es");
        }
        else {
            requestedAttributes = languageAttributes.get("en");
        }
        return requestedAttributes;
    }

    /**
     * Sets the comment that will be analyzed
     */
    private String setCommentText(String commentText) {
        this.commentText = commentText;
        return commentText;
    }

    /**
     * Analyzes the comment and returns the punctuation for each attribute
     *
     * @param comment the comment
     * @return the hash map with attributes and their respective scores
     * @throws UnirestException the unirest exception
     */
    public HashMap<String, Double> analyzeRequest(String comment) throws UnirestException {
        this.setCommentText(comment);
        request = this.createJSONObjectRequest();
        JSONObject response = Unirest.post("https://commentanalyzer.googleapis"
                + ".com/v1alpha1/comments:analyze?key=" + apiKey)
                .header("Content-Type", "application/json")
                .body(request)
                .asJson().getBody().getObject();
        HashMap<String, Double> scores = new HashMap<>();
        for (AttributeType k : AttributeType.values()) {
            scores.put(attributesPrefix + k.toString(), -1.);
        }
        JSONObject attributes = new JSONObject();
        try {
            attributes = response.getJSONObject("attributeScores");
            Double score;
            for (String k : attributes.keySet()) {
                score = attributes.getJSONObject(k).getJSONObject("summaryScore").getDouble("value");
                scores.put(attributesPrefix + k, score);
            }
        } catch (JSONException e) {
            System.out.println("PERSPECTIVEAPI_ERROR: " + response);
        }
        return scores;
    }

    /**
     * Creates the JSON object used to make the request to PerspectiveAPI
     */
    private JSONObject createJSONObjectRequest() {

        JSONObject request = new JSONObject();
        JSONObject comment = new JSONObject();
        comment.put("text", this.commentText);
        comment.put("type", this.commentType);
        JSONObject requestedAttributes = new JSONObject();
        for (AttributeType a : this.requestedAttributes) {
            requestedAttributes.put(a.toString(), (Map<?, ?>) null);
        }
        JSONArray languages = new JSONArray();
        languages.put(language);

        request.put("comment", comment);
        request.put("requestedAttributes", requestedAttributes);
        request.put("languages", languages);
        request.put("doNotStore", this.doNotStore);
        request.put("clientToken", this.clientToken);
        request.put("sessionId", this.sessionId);
        return request;
    }

    /**
     * Gets language attributes. For testing purposes
     *
     * @return the language attributes
     */
    public Map<String, AttributeType[]> getLanguageAttributes() {
        return languageAttributes;
    }
}