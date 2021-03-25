package com.xatkit.core.recognition.processor.toxicity.perspectiveapi;

import lombok.NonNull;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * The toxicity scores returned by the {@link PerspectiveApiClient}.
 * <p>
 * The {@link #UNSET_SCORE} field can be used to represent unset scores, e.g. a placeholder, null-safe value to
 * return when an error occurred during the scoring.
 * <p>
 * This class defines getters for common scores that are computed for every language. See the subclasses of this
 * class to access language-specific scores.
 */
public class PerspectiveApiScore {

    /**
     * A null-safe unset score.
     * <p>
     * Accessing a specific score on this object always returns {@link #DEFAULT_SCORE}.
     */
    public static final PerspectiveApiScore UNSET_SCORE = new PerspectiveApiScore();

    /**
     * The default score.
     * <p>
     * This score is used as a placeholder when Perspective API does not return a specific score.
     */
    public static final Double DEFAULT_SCORE = -1d;

    /**
     * The scores extracted from the provided {@link JSONObject}.
     */
    private Map<String, Double> scores = new HashMap<>();

    /**
     * Disabled constructor.
     */
    private PerspectiveApiScore() {
        /*
         * This constructor is used to initialize the UNSET_SCORE field.
         */
    }

    /**
     * Initializes the {@link PerspectiveApiScore} with the provided {@code jsonObject}.
     *
     * @param jsonObject the {@link JSONObject} representing the Perspective API response
     * @throws NullPointerException if the provided {@code jsonObject} is {@code null}
     */
    public PerspectiveApiScore(@NonNull JSONObject jsonObject) {
        JSONObject attributes = jsonObject.getJSONObject("attributeScores");
        for (String k : attributes.keySet()) {
            Double score = attributes.getJSONObject(k).getJSONObject("summaryScore").getDouble("value");
            scores.put(k, score);
        }
    }

    /**
     * Returns the score associated to the provided {@code label}.
     *
     * @param label the {@link PerspectiveApiLabel} to retrieve the score from
     * @return the score
     */
    public final Double getScore(PerspectiveApiLabel label) {
        return this.scores.getOrDefault(label.toString(), DEFAULT_SCORE);
    }

    /**
     * @return the {@link PerspectiveApiLabel#TOXICITY} score.
     */
    public Double getToxicityScore() {
        return this.getScore(PerspectiveApiLabel.TOXICITY);
    }

    /**
     * @return the {@link PerspectiveApiLabel#SEVERE_TOXICITY} score.
     */
    public Double getSevereToxicityScore() {
        return this.getScore(PerspectiveApiLabel.SEVERE_TOXICITY);
    }
}
