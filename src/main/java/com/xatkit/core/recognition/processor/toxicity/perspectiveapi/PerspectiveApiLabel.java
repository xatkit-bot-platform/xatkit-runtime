package com.xatkit.core.recognition.processor.toxicity.perspectiveapi;

/**
 * Labels representing the toxicity categories defined by Perspective API.
 * <p>
 * This enumeration is language-agnostic: it contains all the labels used in Perspective API. Check
 * {@link PerspectiveApiClient#getLanguageLabels()} to see the attributes available for a given language.
 */
public enum PerspectiveApiLabel {

    /**
     * Rude, disrespectful, or unreasonable comment that is likely to make people leave a discussion.
     */
    TOXICITY,
    /**
     * A very hateful, aggressive, disrespectful comment or otherwise very likely to make a user leave a
     * discussion or give up on sharing their perspective. This model is much less sensitive to comments that
     * include positive uses of curse words, for example. A labelled dataset and details of the methodology can be
     * found in the same toxicity dataset that is available for the toxicity model.
     */
    SEVERE_TOXICITY,
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
