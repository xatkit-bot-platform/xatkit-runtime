package com.xatkit.core.recognition.processor.toxicity.detoxify;

/**
 * Labels representing the toxicity categories defined by Perspective API.
 */
public enum DetoxifyLabel {

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
     * Negative or hateful comments targeting someone because of their identity.
     */
    IDENTITY_HATE,
    /**
     * Insulting, inflammatory, or negative comment towards a person or a group of people.
     */
    INSULT,
    /**
     * Describes an intention to inflict pain, injury, or violence against an individual or group.
     */
    THREAT,
    /**
     * Obscene or vulgar language such as cursing.
     */
    OBSCENE
}
