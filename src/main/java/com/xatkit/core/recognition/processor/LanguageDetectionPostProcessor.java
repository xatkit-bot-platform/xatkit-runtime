package com.xatkit.core.recognition.processor;


import com.xatkit.execution.StateContext;
import com.xatkit.intent.RecognizedIntent;
import fr.inria.atlanmod.commons.log.Log;
import lombok.Getter;
import lombok.NonNull;
import opennlp.tools.langdetect.Language;
import opennlp.tools.langdetect.LanguageDetector;
import opennlp.tools.langdetect.LanguageDetectorME;
import opennlp.tools.langdetect.LanguageDetectorModel;
import org.apache.commons.configuration2.Configuration;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

import static java.util.Objects.isNull;

/**
 * Detects the language(s) of the provided {@link RecognizedIntent}.
 * <p>
 * This processor stores two information:
 * <ul>
 *     <li>The detected language(s) for the last message, stored in a {@link LanguageDetectionScore} object in
 *     {@link RecognizedIntent#getNlpData()}, that can be accessed through the
 *     {@link #OPENNLP_LAST_INPUT_SCORE_PARAMETER_KEY} key</li>
 *     <li>The detected language(s) for the aggregation of the last {@link #MAX_NUM_USER_MESSAGES} messages, stored
 *     in a {@link LanguageDetectionScore} object in {@link StateContext#getSession()}, that can be accessed through
 *     the {@link #OPENNLP_LAST_N_INPUTS_SCORE_PARAMETER_KEY}.
 *     </li>
 * </ul>
 * The score stored in the {@link RecognizedIntent} is not impacted by previous messages, this is useful in the
 * context of multi-lingual bots, or for users mixing multiple languages in the same conversation. The score stored
 * in the {@link StateContext} is typically more accurate (because it is computed over a larger corpus), but
 * represents the dominant language in the last {@link #MAX_NUM_USER_MESSAGES} messages and may not match the
 * language of the last message.
 *
 * @see #process(RecognizedIntent, StateContext)
 * @see LanguageDetectionScore
 */
public class LanguageDetectionPostProcessor implements IntentPostProcessor {

    /**
     * The {@link Configuration} parameter key to set the number of user messages to consider for language recognition.
     * <p>
     * Language detection produces more accurate results on large texts. Since a bot typically deals with short
     * messages, the processor collects the last {@code MAX_NUM_USER_MESSAGES} to create a larger corpus to extract
     * the language from.
     * <p>
     * This property is set to {@link #DEFAULT_MAX_NUM_USER_MESSAGES} by default.
     */
    public static final String MAX_NUM_USER_MESSAGES = "xatkit.opennlp.langdetect.lastNInputsMaxSize";

    /**
     * The default value for {@link #MAX_NUM_USER_MESSAGES}.
     */
    public static final int DEFAULT_MAX_NUM_USER_MESSAGES = 10;

    /**
     * The {@link Configuration} parameter key to set the number of languages to include in the detection result.
     * <p>
     * Language detection returns a list of languages ranked by probability. This property allows to reduce the
     * result size and retain only the {@code MAX_NUM_LANGUAGES_IN_SCORE} candidates.
     * <p>
     * This property is set to {@link #DEFAULT_MAX_NUM_LANGUAGES_IN_SCORE} by default.
     */
    public static final String MAX_NUM_LANGUAGES_IN_SCORE = "xatkit.opennlp.langdetect.maxLanguagesInScore";

    /**
     * The default value for {@link #MAX_NUM_LANGUAGES_IN_SCORE}.
     */
    public static final int DEFAULT_MAX_NUM_LANGUAGES_IN_SCORE = 3;

    /**
     * The {@link Configuration} parameter key to set the path to the language model.
     * <p>
     * This processor requires an external language model to compute the language of a given input. This property
     * must be set with a path to the model binary file.
     * <p>
     * You can download the model from the <a href="https://opennlp.apache.org/models.html">Apache OpenNLP website</a>.
     */
    public static final String OPENNLP_MODEL_PATH_PARAMETER_KEY = "xatkit.opennlp.langdetect.modelPath";

    /**
     * The NLP-data key to access the detected language for the last user input.
     *
     * @see LanguageDetectionScore
     * @see RecognizedIntent#getNlpData()
     */
    public static final String OPENNLP_LAST_INPUT_SCORE_PARAMETER_KEY = "nlp.opennlp.langdetect.lastInput";

    /**
     * The session key to access the detected language for the last {@link #MAX_NUM_USER_MESSAGES} user inputs.
     * <p>
     * This score is more accurate than {@link #OPENNLP_LAST_INPUT_SCORE_PARAMETER_KEY} since it evaluates the
     * language on the aggregation of the last {@link #MAX_NUM_USER_MESSAGES}.
     *
     * @see LanguageDetectionScore
     * @see StateContext#getSession()
     */
    public static final String OPENNLP_LAST_N_INPUTS_SCORE_PARAMETER_KEY = "nlp.opennlp.langdetect.lastNInputs";

    /**
     * The session key to access the message queue used to aggregate user messages.
     * <p>
     * This attribute is private: client code should not manipulate the message queue directly.
     *
     * @see StateContext#getSession()
     */
    private static final String OPENNLP_LAST_N_INPUTS_QUEUE_PARAMETER_KEY = "nlp.opennlp.langdetect.queue";

    /**
     * The path of the binary file containing the language model.
     */
    private String modelPath;

    /**
     * The underlying language detector.
     */
    @Getter
    private LanguageDetector languageDetector;

    /**
     * The number of user messages to consider for language recognition.
     *
     * @see #MAX_NUM_USER_MESSAGES
     */
    private int lastNInputsMaxSize;

    /**
     * The number of languages to include in the detection result.
     *
     * @see #MAX_NUM_LANGUAGES_IN_SCORE
     */
    private int maxLanguagesInScore;


    /**
     * Initializes the {@link LanguageDetectionPostProcessor}.
     * <p>
     * The provided {@code configuration} needs to contain a valid path to the language model file used to detect
     * language (see {@link #OPENNLP_MODEL_PATH_PARAMETER_KEY}).
     *
     * @param configuration the configuration used to initialize this processor
     */
    public LanguageDetectionPostProcessor(@NonNull Configuration configuration) {
        modelPath = configuration.getString(OPENNLP_MODEL_PATH_PARAMETER_KEY);
        LanguageDetectorModel trainedModel;
        try {
            File modelFile = new File(modelPath);
            trainedModel = new LanguageDetectorModel(modelFile);
        } catch (Exception e) {
            Log.error(e, "An error occurred while initializing the {0}, this processor won't perform language "
                    + "detection. See the attached exception for more information.", this.getClass().getSimpleName());
            return;
        }
        languageDetector = new LanguageDetectorME(trainedModel);
        lastNInputsMaxSize = configuration.getInt(MAX_NUM_USER_MESSAGES, DEFAULT_MAX_NUM_USER_MESSAGES);
        if (lastNInputsMaxSize < DEFAULT_MAX_NUM_USER_MESSAGES) {
            Log.warn("The specified number of messages to use to detect user language ({0}) is too small and may "
                    + "produce inaccurate results. It is recommended to select a value higher than {1} to improve the"
                    + " quality of the detection.", lastNInputsMaxSize, DEFAULT_MAX_NUM_USER_MESSAGES);
        }
        maxLanguagesInScore = configuration.getInt(MAX_NUM_LANGUAGES_IN_SCORE, DEFAULT_MAX_NUM_LANGUAGES_IN_SCORE);
        int supportedLanguageCount = languageDetector.getSupportedLanguages().length;
        if (maxLanguagesInScore > supportedLanguageCount) {
            Log.warn("Cannot compute the {0} best candidates, the language model only supports {1}. Results will "
                    + "include all the languages from the model.", maxLanguagesInScore, supportedLanguageCount);
            maxLanguagesInScore = supportedLanguageCount;
        }
    }

    /**
     * Detects the user language from the provided {@code recognizedIntent}.
     * <p>
     * The detected language(s) is stored in a {@link LanguageDetectionScore} object in
     * {@link RecognizedIntent#getNlpData()}, and can be accessed through the
     * {@link #OPENNLP_LAST_INPUT_SCORE_PARAMETER_KEY} key.
     * <p>
     * This method also updates the detected language(s) for the aggregation of the last
     * {@link #MAX_NUM_USER_MESSAGES} messages. This score is stored in a {@link LanguageDetectionScore} object in
     * {@link StateContext#getSession()}, and can be accessed through the
     * {@link #OPENNLP_LAST_N_INPUTS_SCORE_PARAMETER_KEY} key.
     *
     * @param recognizedIntent the {@link RecognizedIntent} to process
     * @param context          the {@link StateContext} associated to the {@code recognizedIntent}
     * @return the updated {@code recognizedIntent}
     * @see LanguageDetectionScore
     * @see #OPENNLP_LAST_INPUT_SCORE_PARAMETER_KEY
     * @see #OPENNLP_LAST_N_INPUTS_SCORE_PARAMETER_KEY
     */
    @Override
    public RecognizedIntent process(RecognizedIntent recognizedIntent, StateContext context) {
        if (isNull(languageDetector)) {
            /*
             * Skip language detection if the language detector was not initialized properly.
             * We don't have to log anything: the constructor already logged an error when trying to initialize the
             * language detector.
             */
            return recognizedIntent;
        }
        String lastInputText = recognizedIntent.getMatchedInput();
        Language[] langsLast = languageDetector.predictLanguages(lastInputText);
        LanguageDetectionScore scoreLast = new LanguageDetectionScore(langsLast, maxLanguagesInScore);
        recognizedIntent.getNlpData().put(OPENNLP_LAST_INPUT_SCORE_PARAMETER_KEY, scoreLast);

        @SuppressWarnings("unchecked")
        Queue<String> lastNInputs =
                (Queue<String>) context.getSession().computeIfAbsent(OPENNLP_LAST_N_INPUTS_QUEUE_PARAMETER_KEY,
                        k -> new LinkedList<>());

        lastNInputs.add(lastInputText);
        if (lastNInputs.size() > lastNInputsMaxSize) {
            lastNInputs.remove();
        }
        String lastInputsText = String.join("\n", lastNInputs);
        Language[] langsLastN = languageDetector.predictLanguages(lastInputsText);
        LanguageDetectionScore scoreLastN = new LanguageDetectionScore(langsLastN, maxLanguagesInScore);
        context.getSession().put(OPENNLP_LAST_N_INPUTS_SCORE_PARAMETER_KEY, scoreLastN);
        return recognizedIntent;
    }
}
