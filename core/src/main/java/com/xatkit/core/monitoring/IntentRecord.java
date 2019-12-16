package com.xatkit.core.monitoring;

import com.xatkit.intent.RecognizedIntent;

/**
 * A database record holding intent-related information.
 */
class IntentRecord implements Record {

    private static final long serialVersionUID = 42L;

    /**
     * The utterance that has been mapped to the intent.
     */
    private String utterance;

    /**
     * The name of the intent extracted from the utterance.
     */
    private String intentName;

    /**
     * The confidence level associated to the intent extracted from the utterance.
     * <p>
     * This value is a percentage contained in {@code [0..1]}
     */
    private Float recognitionConfidence;

    public IntentRecord(RecognizedIntent recognizedIntent) {
        this.utterance = recognizedIntent.getMatchedInput();
        this.intentName = recognizedIntent.getDefinition().getName();
        this.recognitionConfidence = recognizedIntent.getRecognitionConfidence();
    }

    public String getUtterance() {
        return this.utterance;
    }

    public String getIntentName() {
        return this.intentName;
    }

    public Float getRecognitionConfidence() {
        return this.recognitionConfidence;
    }

    @Override
    public int hashCode() {
        return this.utterance.hashCode() + this.intentName.hashCode() + this.recognitionConfidence.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IntentRecord) {
            IntentRecord other = (IntentRecord) obj;
            return other.utterance.equals(this.utterance) && other.intentName.equals(this.intentName)
                    && other.recognitionConfidence.equals(this.recognitionConfidence);
        }
        return super.equals(obj);
    }
}
