package com.xatkit.core.recognition.processor.toxicity.perspectiveapi;

import com.mashape.unirest.http.Unirest;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;
import org.apache.commons.configuration2.Configuration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Client for Perspective API.
 * <p>
 * The API key for the Perspective API must be specified in the provided configuration. See
 * {@link PerspectiveApiConfiguration#API_KEY} for more information.
 */
public class PerspectiveApiClient {

    /**
     * The configuration of the client.
     */
    private PerspectiveApiConfiguration configuration;

    /**
     * Maps supported languages to the corresponding {@link PerspectiveApiLabel}s.
     * <p>
     * Requests sent to Perspective API must define the labels to extract, and the API returns an error if a label is
     * not supported by the input language. This map allows to specify which labels are associated to every language,
     * and make sure the request always contain existing labels for a given language.
     */
    private Map<String, PerspectiveApiLabel[]> languageLabels;

    /**
     * Initializes the client with the provided {@code baseConfiguration}.
     * <p>
     * The provided {@code baseConfiguration} must define the Perspective API key to use (see
     * {@link PerspectiveApiConfiguration#API_KEY} for more information).
     * <p>
     * <b>Note</b>: the client's default configuration uses "en" as language. See
     * {@link PerspectiveApiConfiguration#LANGUAGE} to use a different language.
     *
     * @param baseConfiguration the {@link Configuration} used to initialize the client
     * @throws NullPointerException if the provided {@code baseConfiguration} is {@code null} or if it does not
     *                              contain the Perspective API key
     */
    public PerspectiveApiClient(@NonNull Configuration baseConfiguration) {
        this.configuration = new PerspectiveApiConfiguration(baseConfiguration);
        languageLabels = new HashMap<>();
        languageLabels.put("en", new PerspectiveApiLabel[]{
                PerspectiveApiLabel.TOXICITY,
                PerspectiveApiLabel.SEVERE_TOXICITY,
                PerspectiveApiLabel.TOXICITY_FAST,
                PerspectiveApiLabel.IDENTITY_ATTACK,
                PerspectiveApiLabel.INSULT,
                PerspectiveApiLabel.PROFANITY,
                PerspectiveApiLabel.THREAT,
                PerspectiveApiLabel.SEXUALLY_EXPLICIT,
                PerspectiveApiLabel.FLIRTATION,
                PerspectiveApiLabel.ATTACK_ON_AUTHOR,
                PerspectiveApiLabel.ATTACK_ON_COMMENTER,
                PerspectiveApiLabel.INCOHERENT,
                PerspectiveApiLabel.INFLAMMATORY,
                PerspectiveApiLabel.LIKELY_TO_REJECT,
                PerspectiveApiLabel.OBSCENE,
                PerspectiveApiLabel.SPAM,
                PerspectiveApiLabel.UNSUBSTANTIAL
        });
        languageLabels.put("es", new PerspectiveApiLabel[]{
                PerspectiveApiLabel.TOXICITY,
                PerspectiveApiLabel.SEVERE_TOXICITY,
                PerspectiveApiLabel.IDENTITY_ATTACK_EXPERIMENTAL,
                PerspectiveApiLabel.INSULT_EXPERIMENTAL,
                PerspectiveApiLabel.PROFANITY_EXPERIMENTAL,
                PerspectiveApiLabel.THREAT_EXPERIMENTAL
        });
    }

    /**
     * Computes the toxicity scores for the provided {@code input}.
     *
     * @param input the text to analyze
     * @return the computed scores, or {@link PerspectiveApiScore#UNSET_SCORE} if an error occurred while computing
     * the scores
     */
    public PerspectiveApiScore analyzeRequest(String input) {
        try {
            JSONObject request = this.createJSONObjectRequest(input);
            JSONObject response = Unirest.post("https://commentanalyzer.googleapis"
                    + ".com/v1alpha1/comments:analyze?key=" + configuration.getApiKey())
                    .header("Content-Type", "application/json")
                    .body(request)
                    .asJson().getBody().getObject();
            switch (configuration.getLanguage()) {
                case "en":
                    return new PerspectiveApiEnglishScore(response);
                case "es":
                    return new PerspectiveApiSpanishScore(response);
                default:
                    throw new IllegalStateException(MessageFormat.format("Cannot compute the PerspectiveApi scores "
                            + "for the provided language {0}", configuration.getLanguage()));
            }
        } catch (Exception e) {
            Log.error(e, "An error occurred while computing the toxicity scores, see the attached exception for "
                    + "more information");
        }
        return PerspectiveApiScore.UNSET_SCORE;
    }

    /**
     * Creates the JSON object used to make the request to PerspectiveAPI.
     *
     * @param input the text to analyze
     * @return the created {@link JSONObject}
     */
    private JSONObject createJSONObjectRequest(String input) {

        JSONObject request = new JSONObject();
        JSONObject comment = new JSONObject();
        comment.put("text", input);
        comment.put("type", "PLAIN_TEXT");
        JSONObject requestedAttributes = new JSONObject();
        PerspectiveApiLabel[] requestedAttributesList = languageLabels.get(configuration.getLanguage());
        for (PerspectiveApiLabel a : requestedAttributesList) {
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
     * Returns the language-label mapping.
     * <p>
     * This method is public for testing purposes.
     *
     * @return the language-label mapping
     */
    public Map<String, PerspectiveApiLabel[]> getLanguageLabels() {
        return languageLabels;
    }
}
