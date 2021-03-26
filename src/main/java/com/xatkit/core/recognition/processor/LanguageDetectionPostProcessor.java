package com.xatkit.core.recognition.processor;


import com.xatkit.execution.StateContext;
import com.xatkit.intent.RecognizedIntent;
import lombok.Getter;
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
 * Annotates {@link RecognizedIntent}s with language predictions and {@link StateContext}'s session with language
 * predictions on the last N matched inputs.
 *
 * Each of the scores can be accessed with a dedicated key:
 * <ul>
 *     <li>Langauge prediction of the last user message: {@link #OPENNLP_LAST_INPUT_SCORE_PARAMETER_KEY} in
 *     {@link RecognizedIntent#getNlpData()}</li>
 *     <li>Language prediction of the last N user messages: {@link #OPENNLP_LAST_N_INPUTS_SCORE_PARAMETER_KEY} in
 *     {@link StateContext#getSession()} (because it id session dependant rather than intent dependant)
 * </ul>
 */
public class LanguageDetectionPostProcessor implements IntentPostProcessor {

    /**
     * The {@link Configuration} parameter key to set {@link #lastNInputsMaxSize}.
     */
    public final static String MAX_NUM_USER_MESSAGES = "xatkit.opennlp.langdetect.lastNInputsMaxSize";

    /**
     * The {@link Configuration} parameter key to set {@link #lastNInputsMaxSize}.
     */
    public final static String MAX_NUM_LANGUAGES_IN_SCORE = "xatkit.opennlp.langdetect.maxLanguagesInScore";

    /**
     * The {@link Configuration} parameter key to set {@link #modelPath}.
     */
    public final static String OPENNLP_MODEL_PATH_PARAMETER_KEY = "xatkit.opennlp.langdetect.modelPath";

    /**
     * The NLP-data key to access last input score.
     *
     * @see RecognizedIntent#getNlpData()
     */
    protected final static String OPENNLP_LAST_INPUT_SCORE_PARAMETER_KEY = "nlp.opennlp.langdetect.lastInput";

    /**
     * The session key to access the {@link #lastNInputsMaxSize} last inputs score.
     *
     * @see StateContext#getSession()
     */
    protected final static String OPENNLP_LAST_N_INPUTS_SCORE_PARAMETER_KEY = "nlp.opennlp.langdetect.lastNInputs";

    /**
     * The session key to access the {@link #lastNInputsMaxSize} last inputs.
     *
     * @see StateContext#getSession()
     */
    protected final static String OPENNLP_LAST_N_INPUTS_QUEUE_PARAMETER_KEY = "nlp.opennlp.langdetect.queue";

    /**
     * The path of the binary file containing the language model.
     */
    private String modelPath;

    /**
     * The Language detector.
     */
    @Getter
    LanguageDetector languageDetector;

    /**
     * The maximum number of inputs to store in {@link StateContext#getSession()}'s
     * {@link #OPENNLP_LAST_N_INPUTS_SCORE_PARAMETER_KEY}.
     */
    int lastNInputsMaxSize;

    /**
     * The maximum number of languages to be added to the prediction score
     * {@link #OPENNLP_LAST_N_INPUTS_SCORE_PARAMETER_KEY}.
     */
    int maxLanguagesInScore;


    /**
     * Initializes the {@link LanguageDetectionPostProcessor}.
     *
     * @param configuration the Xatkit bot configuration that contains the {@link LanguageDetectionPostProcessor}
     * parameters
     */
    public LanguageDetectionPostProcessor(Configuration configuration) {
        modelPath = configuration.getString(OPENNLP_MODEL_PATH_PARAMETER_KEY);
        LanguageDetectorModel trainedModel = null;
        try {
            File modelFile = new File(modelPath);
            trainedModel = new LanguageDetectorModel(modelFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assert trainedModel != null;
        languageDetector = new LanguageDetectorME(trainedModel);
        lastNInputsMaxSize = configuration.getInt(MAX_NUM_USER_MESSAGES, 10);
        if (lastNInputsMaxSize < 2) {
            lastNInputsMaxSize = 10;
        }
        maxLanguagesInScore = configuration.getInt(MAX_NUM_LANGUAGES_IN_SCORE, 3);
        if (maxLanguagesInScore < 1) {
            maxLanguagesInScore = 3;
        }
        if (maxLanguagesInScore > languageDetector.getSupportedLanguages().length) {
            maxLanguagesInScore = languageDetector.getSupportedLanguages().length;
        }
    }

    /**
     * Predicts the language used in the {@code recognizedIntent} and pushes the {@code recognizedIntent} into a
     * queue located in {@link StateContext#getSession()} to predict the language of the concatenation of the
     * queue's messages, which is also stored in {@link StateContext#getSession()}.
     *
     * The prediction is wrapped into a {@link LanguageDetectionScore} that contains the confidence for the
     * {@link #maxLanguagesInScore} best scored languages
     *
     * @param recognizedIntent the {@link RecognizedIntent} to process
     * @param context          the {@link StateContext} associated to the {@code recognizedIntent}
     * @return the updated {@code recognizedIntent}
     * @see LanguageDetectionScore
     */
    @Override
    public RecognizedIntent process(RecognizedIntent recognizedIntent, StateContext context) {
        String lastInputText = recognizedIntent.getMatchedInput();
        Language[] langsLast = languageDetector.predictLanguages(lastInputText);
        LanguageDetectionScore scoreLast = new LanguageDetectionScore(langsLast, maxLanguagesInScore);
        recognizedIntent.getNlpData().put(OPENNLP_LAST_INPUT_SCORE_PARAMETER_KEY, scoreLast);

        Queue<String> lastNInputs = (Queue<String>) context.getSession().get(OPENNLP_LAST_N_INPUTS_QUEUE_PARAMETER_KEY);
        if (isNull(lastNInputs)) {
            lastNInputs = new LinkedList<>();
            context.getSession().put(OPENNLP_LAST_N_INPUTS_QUEUE_PARAMETER_KEY, lastNInputs);
        }
        lastNInputs.add(lastInputText);
        if (lastNInputs.size() > lastNInputsMaxSize) {
            lastNInputs.remove();
        }
        String lastInputsText = "";
        for (String message : lastNInputs) {
            lastInputsText += message + "\n";
        }
        Language[] langsLastN = languageDetector.predictLanguages(lastInputsText);
        LanguageDetectionScore scoreLastN = new LanguageDetectionScore(langsLastN, maxLanguagesInScore);
        context.getSession().put(OPENNLP_LAST_N_INPUTS_SCORE_PARAMETER_KEY, scoreLastN);
        return recognizedIntent;
    }
}
