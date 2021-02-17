package com.xatkit.core.recognition.processor;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.NonNull;
import org.json.JSONObject;

import java.util.ArrayList;
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
    private static ArrayList<AttributeType> requestedAttributes;
    private static ArrayList<String> languages;
    private static Boolean doNotStore;
    private static String clientToken;
    private static String sessionId;

    private JSONObject request;

    /**
     * Constructor
     */
    PerspectiveapiInterface(@NonNull String apiKey, @NonNull String commentText,
                            @NonNull ArrayList<AttributeType> requestedAttributes,
                            ArrayList<String> languages, Boolean doNotStore, String clientToken, String sessionId) {
        PerspectiveapiInterface.apiKey = apiKey;
        PerspectiveapiInterface.commentText = commentText;
        PerspectiveapiInterface.requestedAttributes = (ArrayList<AttributeType>) requestedAttributes.clone();
        PerspectiveapiInterface.languages = languages != null ? (ArrayList<String>) languages.clone() : null;
        PerspectiveapiInterface.doNotStore = doNotStore != null ? doNotStore : false;
        PerspectiveapiInterface.clientToken = clientToken;
        PerspectiveapiInterface.sessionId = sessionId;
    }

    /**
     * Sets the comment that will be analyzed
     */
    public static String setCommentText(String commentText) {
        PerspectiveapiInterface.commentText = commentText;
        return commentText;
    }

    /**
     * Adds the new attributes to the perspectiveapi class
     */
    public static ArrayList<AttributeType> addRequestedAttributes(ArrayList<AttributeType> attributes) {
        for (AttributeType a : attributes) {
            if (!PerspectiveapiInterface.requestedAttributes.contains(a)) {
                PerspectiveapiInterface.requestedAttributes.add(a);
            }

        }
        return PerspectiveapiInterface.requestedAttributes;
    }

    /**
     * Removes the attributes of the perspectiveapi class
     */
    public static ArrayList<AttributeType> removeRequestedAttributes(ArrayList<AttributeType> attributes) {
        PerspectiveapiInterface.requestedAttributes.removeAll(attributes);
        return PerspectiveapiInterface.requestedAttributes;
    }

    /**
     * Analyzes the comment and returns the punctuation for each attribute present in the perspectiveapi class
     */
    public static HashMap<String, Double> analyzeRequest(String keyPrefix) throws UnirestException {
        JSONObject request = PerspectiveapiInterface.createJSONObjectRequest();
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
        request.put("comment", comment);
        request.put("requestedAttributes", requestedAttributes);
        request.put("doNotStore", PerspectiveapiInterface.doNotStore);
        request.put("clientToken", PerspectiveapiInterface.clientToken);
        request.put("sessionId", PerspectiveapiInterface.sessionId);
        return request;
    }

    /**
     * Prints the class parameters
     */
    public static void printParameters() {

        System.out.println("========== PerspectiveAPI Parameters ==========\n");
        System.out.println(apiKey);
        System.out.println(commentText);
        System.out.println(commentType);
        System.out.println(requestedAttributes);
        System.out.println(languages);
        System.out.println(doNotStore);
        System.out.println(clientToken);
        System.out.println(sessionId);
        System.out.println("\n===============================================");
    }
}