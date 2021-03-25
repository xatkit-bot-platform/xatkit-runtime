package com.xatkit.core.recognition.processor.toxicity.detoxify;

import lombok.NonNull;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * The toxicity scores returned by the {@link DetoxifyClient}.
 * <p>
 * The {@link #UNSET_SCORE} field can be used to represent unset scores, e.g. a placeholder, null-safe value to return
 * when an error occurred during the scoring.
 */
public class DetoxifyScore {

    /**
     * A null-safe unset score.
     * <p>
     * Accessing a specific score on this object always returns {@link #DEFAULT_SCORE}.
     */
    public static final DetoxifyScore UNSET_SCORE = new DetoxifyScore();

    /**
     * The default score.
     * <p>
     * This score is used as a placeholder when the Detoxify server does not return a specific score.
     */
    public static final Double DEFAULT_SCORE = -1d;

    /**
     * The scores extracted from the provided {@link JSONObject}.
     */
    private Map<String, Double> scores = new HashMap<>();

    /**
     * Disabled constructor.
     */
    private DetoxifyScore() {
        /*
         * This constructor is used to initialize the UNSET_SCORE field.
         */
    }

    /**
     * Initializes the {@link DetoxifyScore} with the provided {@code jsonObject}.
     *
     * @param jsonObject the {@link JSONObject} representing the Detoxify server response
     * @throws NullPointerException if the provided {@code jsonObject} is {@code null}
     */
    public DetoxifyScore(@NonNull JSONObject jsonObject) {
        for (String k : jsonObject.keySet()) {
            Double score = jsonObject.getDouble(k);
            scores.put(k, score);
        }
    }

    /**
     * Returns the score associated to the provided {@code label}.
     *
     * @param label the {@link DetoxifyLabel} to retrieve the score from
     * @return the score
     */
    public final Double getScore(DetoxifyLabel label) {
        return this.scores.getOrDefault(label.toString(), DEFAULT_SCORE);
    }

    /**
     * @return the {@link DetoxifyLabel#TOXICITY} score.
     */
    public Double getToxicityScore() {
        return this.getScore(DetoxifyLabel.TOXICITY);
    }

    /**
     * @return the {@link DetoxifyLabel#SEVERE_TOXICITY} score.
     */
    public Double getSevereToxicityScore() {
        return this.getScore(DetoxifyLabel.SEVERE_TOXICITY);
    }

    /**
     * @return the {@link DetoxifyLabel#IDENTITY_HATE} score.
     */
    public Double getIdentityHateScore() {
        return this.getScore(DetoxifyLabel.IDENTITY_HATE);
    }

    /**
     * @return the {@link DetoxifyLabel#INSULT} score.
     */
    public Double getInsultScore() {
        return this.getScore(DetoxifyLabel.INSULT);
    }

    /**
     * @return the {@link DetoxifyLabel#THREAT} score.
     */
    public Double getThreatScore() {
        return this.getScore(DetoxifyLabel.THREAT);
    }

    /**
     * @return the {@link DetoxifyLabel#OBSCENE} score.
     */
    public Double getObsceneScore() {
        return this.getScore(DetoxifyLabel.OBSCENE);
    }
}
