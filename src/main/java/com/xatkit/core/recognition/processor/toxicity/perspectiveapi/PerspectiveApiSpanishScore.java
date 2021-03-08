package com.xatkit.core.recognition.processor.toxicity.perspectiveapi;

import lombok.NonNull;
import org.json.JSONObject;

/**
 * A {@link PerspectiveApiScore} with additional getters for Spanish-specific scores.
 */
public class PerspectiveApiSpanishScore extends PerspectiveApiScore {

    /**
     * Initializes the {@link PerspectiveApiSpanishScore} with the provided {@code jsonObject}.
     *
     * @param jsonObject the {@link JSONObject} representing the Perspective API response
     * @throws NullPointerException if the provided {@code jsonObject} is {@code null}
     */
    public PerspectiveApiSpanishScore(@NonNull JSONObject jsonObject) {
        super(jsonObject);
    }

    /**
     * @return the {@link PerspectiveApiLabel#IDENTITY_ATTACK_EXPERIMENTAL} score.
     */
    public Double getIdentityAttackExperimentalScore() {
        return this.getScore(PerspectiveApiLabel.IDENTITY_ATTACK_EXPERIMENTAL);
    }

    /**
     * @return the {@link PerspectiveApiLabel#INSULT_EXPERIMENTAL} score.
     */
    public Double getInsultExperimentalScore() {
        return this.getScore(PerspectiveApiLabel.INSULT_EXPERIMENTAL);
    }

    /**
     * @return the {@link PerspectiveApiLabel#PROFANITY_EXPERIMENTAL} score.
     */
    public Double getProfanityExperimentalScore() {
        return this.getScore(PerspectiveApiLabel.PROFANITY_EXPERIMENTAL);
    }

    /**
     * @return the {@link PerspectiveApiLabel#THREAT_EXPERIMENTAL} score.
     */
    public Double getThreatExperimentalScore() {
        return this.getScore(PerspectiveApiLabel.THREAT_EXPERIMENTAL);
    }
}
