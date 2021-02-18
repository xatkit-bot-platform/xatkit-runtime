package com.xatkit.core.recognition.processor;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.NonNull;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class PerspectiveapiInterface {

    public enum AttributeType {

        /**
         * Rude, disrespectful, or unreasonable comment that is likely to make people
         * leave a discussion.
         */
        TOXICITY,
        /**
         * Rude, disrespectful, or unreasonable comment that is likely to make people
         * leave a discussion.
         */
        TOXICITY_EXPERIMENTAL,
        /**
         * A very hateful, aggressive, disrespectful comment or otherwise very likely to
         * make a user leave a discussion or give up on sharing their perspective. This
         * model is much less sensitive to comments that include positive uses of curse
         * words, for example. A labelled dataset and details of the methodology can be
         * found in the same toxicity dataset that is available for the toxicity model.
         */
        SEVERE_TOXICITY,
        /**
         * A very hateful, aggressive, disrespectful comment or otherwise very likely to
         * make a user leave a discussion or give up on sharing their perspective. This
         * model is much less sensitive to comments that include positive uses of curse
         * words, for example. A labelled dataset and details of the methodology can be
         * found in the same toxicity dataset that is available for the toxicity model.
         */
        SEVERE_TOXICITY_EXPERIMENTAL,
        /**
         * This model is similar to the TOXICITY model, but has lower latency and lower
         * accuracy in its predictions. Unlike TOXICITY, this model returns summary
         * scores as well as span scores. This model uses character-level n-grams fed
         * into a logistic regression, a method that has been surprisingly effective at
         * detecting abusive language.
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
         * Insulting, inflammatory, or negative comment towards a person or a group of
         * people.
         */
        INSULT,
        /**
         * Insulting, inflammatory, or negative comment towards a person or a group of
         * people.
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
         * Describes an intention to inflict pain, injury, or violence against an
         * individual or group.
         */
        THREAT,
        /**
         * Describes an intention to inflict pain, injury, or violence against an
         * individual or group.
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
         * Overall measure of the likelihood for the comment to be rejected according to
         * the NYT's moderation.
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

    private static String apiKey;
    private static String commentText;
    private static String commentType = "PLAIN_TEXT";
    private static AttributeType[] requestedAttributes;
    private static String language;
    private static Boolean doNotStore;
    private static String clientToken;
    private static String sessionId;
    private static JSONObject request;
    public static Map<String, AttributeType[]> languageAttributes;
    static {
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
    }

    /**
     * Constructor
     */
    PerspectiveapiInterface(@NonNull String apiKey, String language, Boolean doNotStore, String clientToken,
                            String sessionId) {
        PerspectiveapiInterface.apiKey = apiKey;
        PerspectiveapiInterface.language = language != null ? language : "en";
        PerspectiveapiInterface.doNotStore = doNotStore != null ? doNotStore : false;
        PerspectiveapiInterface.clientToken = clientToken;
        PerspectiveapiInterface.sessionId = sessionId;
        PerspectiveapiInterface.setRequestedAttributes();
    }

    /**
     * Sets the attributes available for the chosen language
     */
    private static AttributeType[] setRequestedAttributes() {
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
    public static String setCommentText(String commentText) {
        PerspectiveapiInterface.commentText = commentText;
        return commentText;
    }

    /**
     * Analyzes the comment and returns the punctuation for each attribute present in the perspectiveapi class
     */
    public static HashMap<String, Double> analyzeRequest(String keyPrefix) throws UnirestException {
        request = PerspectiveapiInterface.createJSONObjectRequest();
        JSONObject response = Unirest.post("https://commentanalyzer.googleapis"
                + ".com/v1alpha1/comments:analyze?key=" + apiKey)
                .header("Content-Type", "application/json")
                .body(request)
                .asJson().getBody().getObject();
        JSONObject attributes = response.getJSONObject("attributeScores");
        HashMap<String, Double> scores = new HashMap<>();
        for (String k : attributes.keySet()) {
            Double score = attributes.getJSONObject(k).getJSONObject("summaryScore").getDouble("value");
            scores.put(keyPrefix + k, score);
        }
        return scores;
    }

    /**
     * Creates the JSON object used to make the request to PerspectiveAPI
     */
    private static JSONObject createJSONObjectRequest() {

        JSONObject request = new JSONObject();
        JSONObject comment = new JSONObject();
        comment.put("text", PerspectiveapiInterface.commentText);
        comment.put("type", PerspectiveapiInterface.commentType);
        JSONObject requestedAttributes = new JSONObject();
        for (AttributeType a : PerspectiveapiInterface.requestedAttributes) {
            requestedAttributes.put(a.toString(), (Map<?, ?>) null);
        }
        JSONArray languages = new JSONArray();
        languages.put(language);

        request.put("comment", comment);
        request.put("requestedAttributes", requestedAttributes);
        request.put("languages", languages);
        request.put("doNotStore", PerspectiveapiInterface.doNotStore);
        request.put("clientToken", PerspectiveapiInterface.clientToken);
        request.put("sessionId", PerspectiveapiInterface.sessionId);
        return request;
    }
}