package com.xatkit.core.recognition.processor.toxicity.perspectiveapi;

import lombok.NonNull;
import org.json.JSONObject;

/**
 * A {@link PerspectiveApiScore} with additional getters for English-specific scores.
 */
public class PerspectiveApiEnglishScore extends PerspectiveApiScore {

    /**
     * Initializes the {@link PerspectiveApiEnglishScore} with the provided {@code jsonObject}.
     *
     * @param jsonObject the {@link JSONObject} representing the Perspective API response
     * @throws NullPointerException if the provided {@code jsonObject} is {@code null}
     */
    public PerspectiveApiEnglishScore(@NonNull JSONObject jsonObject) {
        super(jsonObject);
    }

    /**
     * @return the {@link PerspectiveApiLabel#TOXICITY_FAST} score.
     */
    public Double getToxicityFastScore() {
        return this.getScore(PerspectiveApiLabel.TOXICITY_FAST);
    }

    /**
     * @return the {@link PerspectiveApiLabel#IDENTITY_ATTACK} score.
     */
    public Double getIdentityAttackScore() {
        return this.getScore(PerspectiveApiLabel.IDENTITY_ATTACK);
    }

    /**
     * @return the {@link PerspectiveApiLabel#INSULT} score.
     */
    public Double getInsultScore() {
        return this.getScore(PerspectiveApiLabel.INSULT);
    }

    /**
     * @return the {@link PerspectiveApiLabel#PROFANITY} score.
     */
    public Double getProfanityScore() {
        return this.getScore(PerspectiveApiLabel.PROFANITY);
    }

    /**
     * @return the {@link PerspectiveApiLabel#THREAT} score.
     */
    public Double getThreatScore() {
        return this.getScore(PerspectiveApiLabel.THREAT);
    }

    /**
     * @return the {@link PerspectiveApiLabel#SEXUALLY_EXPLICIT} score.
     */
    public Double getSexuallyExplicitScore() {
        return this.getScore(PerspectiveApiLabel.SEXUALLY_EXPLICIT);
    }

    /**
     * @return the {@link PerspectiveApiLabel#FLIRTATION} score.
     */
    public Double getFlirtationScore() {
        return this.getScore(PerspectiveApiLabel.FLIRTATION);
    }

    /**
     * @return the {@link PerspectiveApiLabel#ATTACK_ON_AUTHOR} score.
     */
    public Double getAttackOnAuthorScore() {
        return this.getScore(PerspectiveApiLabel.ATTACK_ON_AUTHOR);
    }

    /**
     * @return the {@link PerspectiveApiLabel#ATTACK_ON_COMMENTER} score.
     */
    public Double getAttackOnCommenterScore() {
        return this.getScore(PerspectiveApiLabel.ATTACK_ON_COMMENTER);
    }

    /**
     * @return the {@link PerspectiveApiLabel#INCOHERENT} score.
     */
    public Double getIncoherentScore() {
        return this.getScore(PerspectiveApiLabel.INCOHERENT);
    }

    /**
     * @return the {@link PerspectiveApiLabel#INFLAMMATORY} score.
     */
    public Double getInflammatoryScore() {
        return this.getScore(PerspectiveApiLabel.INFLAMMATORY);
    }

    /**
     * @return the {@link PerspectiveApiLabel#LIKELY_TO_REJECT} score.
     */
    public Double getLikelyToRejectScore() {
        return this.getScore(PerspectiveApiLabel.LIKELY_TO_REJECT);
    }

    /**
     * @return the {@link PerspectiveApiLabel#OBSCENE} score.
     */
    public Double getObsceneScore() {
        return this.getScore(PerspectiveApiLabel.OBSCENE);
    }

    /**
     * @return the {@link PerspectiveApiLabel#SPAM} score.
     */
    public Double getSpamScore() {
        return this.getScore(PerspectiveApiLabel.SPAM);
    }

    /**
     * @return the {@link PerspectiveApiLabel#UNSUBSTANTIAL} score.
     */
    public Double getUnsubstantialScore() {
        return this.getScore(PerspectiveApiLabel.UNSUBSTANTIAL);
    }
}
