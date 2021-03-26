package com.xatkit.core.recognition.processor;

import lombok.Getter;
import lombok.NonNull;
import opennlp.tools.langdetect.Language;

/**
 * The scores returned by the {@link LanguageDetectionPostProcessor}.
 * <p>
 * This class defines getters for the complete list of predicted languages and their respective confidences, as well
 * as a method for getting a specific language confidence.
 */
public class LanguageDetectionScore {

    /**
     * The default score.
     * <p>
     * This score is used as a placeholder when the {@code LanguageDetectionScore} does not have a defined score for
     * a given language.
     */
    public static final Double DEFAULT_SCORE = -1d;

    /**
     * The name of the predicted languages for a given text, sorted decreasingly by confidence and matching the
     * confidences in {@link #languageConfidences}. Names are in ISO_639-3 standard.
     */
    @Getter
    private String[] languageNames;

    /**
     * The confidences (or scores) of the predicted languages for a given text, sorted decreasingly and matching their
     * language names in {@link #languageNames}.
     */
    @Getter
    private Double[] languageConfidences;


    /**
     * Initializes the {@link LanguageDetectionScore} with the provided {@code Language[]} and {@code maxLanguages}.
     *
     * @param langs the {@link Language[]} representing the complete list of languages with their respective confidences
     * @param maxLanguages the number of languages to store in the {@code score} object (a subset of {@code langs})
     */
    public LanguageDetectionScore(@NonNull Language[] langs, int maxLanguages) {
        languageNames = new String[maxLanguages];
        languageConfidences = new Double[maxLanguages];
        for (int i = 0; i < maxLanguages; i++) {
            languageNames[i] = langs[i].getLang();
            languageConfidences[i] = langs[i].getConfidence();
        }
    }
    /**
     * Returns the score associated to the provided {@code lang}.
     *
     * @param lang the language name in ISO_639-3 standard to retrieve the score from
     * @return the score. If {@code lang} is not available, {@code DEFAULT_SCORE} is returned.
     */
    public final Double getScore(String lang) {
        for (int i = 0; i < languageNames.length; i++) {
            if (languageNames[i].equals(lang)) {
                return languageConfidences[i];
            }
        }
        return DEFAULT_SCORE;
    }

}
