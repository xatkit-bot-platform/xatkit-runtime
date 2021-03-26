package com.xatkit.core.recognition.processor;

import lombok.Getter;
import lombok.NonNull;
import opennlp.tools.langdetect.Language;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores the languages detected by the {@link LanguageDetectionPostProcessor} and their confidence.
 * <p>
 * This class exposes two lists:
 * <ul>
 *     <li>{@link #getLanguageNames()}: the names of the detected languages (ISO_639-3 standard), sorted by
 *     decreasing confidence</li>
 *     <li>{@link #getLanguageConfidences()}: the confidence score associated to each language</li>
 * </ul>
 * <p>
 * This class also provides the {@link #getConfidence(String)} utility method to retrieve the confidence associated
 * to a specific language.
 *
 * @see LanguageDetectionPostProcessor
 */
public class LanguageDetectionScore {

    /**
     * The default score.
     * <p>
     * This score is returned by {@link #getConfidence(String)} when there is no score associated to a given language.
     */
    public static final Double DEFAULT_SCORE = -1d;

    /**
     * The immutable list of detected languages, sorted by decreasing confidence.
     * <p>
     * Languages in this list are formatted according to the ISO_639-3 standard.
     */
    @Getter
    private final List<String> languageNames;

    /**
     * The immutable list of confidence score associated to the languages stored in {@link #languageNames}.
     */
    @Getter
    private final List<Double> languageConfidences;


    /**
     * Initializes the {@link LanguageDetectionScore} with the provided {@code languages} and {@code maxLanguages}.
     * <p>
     * The created {@link LanguageDetectionScore} contains the languages and the confidence score of the first {@code
     * maxLanguages} elements in the provided {@code languages}.
     *
     * @param languages    the list of languages to create the score from
     * @param maxLanguages the number of languages to store in the created score
     */
    public LanguageDetectionScore(@NonNull Language[] languages, int maxLanguages) {
        List<String> tmpLanguageNames = new ArrayList<>();
        List<Double> tmpLanguageConfidences = new ArrayList<>();
        for (int i = 0; i < maxLanguages; i++) {
            tmpLanguageNames.add(languages[i].getLang());
            tmpLanguageConfidences.add(languages[i].getConfidence());
        }
        this.languageNames = Collections.unmodifiableList(tmpLanguageNames);
        this.languageConfidences = Collections.unmodifiableList(tmpLanguageConfidences);
    }

    /**
     * Returns the confidence associated to the provided {@code language}.
     *
     * @param language the language name (ISO_639-3 standard) to retrieve the confidence from
     * @return the confidence, or {@link #DEFAULT_SCORE} if there is no confidence associated to the provided {@code
     * language}
     */
    public final Double getConfidence(String language) {
        for (int i = 0; i < languageNames.size(); i++) {
            if (languageNames.get(i).equals(language)) {
                return languageConfidences.get(i);
            }
        }
        return DEFAULT_SCORE;
    }

}
