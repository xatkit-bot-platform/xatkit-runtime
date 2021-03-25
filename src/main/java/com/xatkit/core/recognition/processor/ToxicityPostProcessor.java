package com.xatkit.core.recognition.processor;

import com.xatkit.core.recognition.processor.toxicity.detoxify.DetoxifyClient;
import com.xatkit.core.recognition.processor.toxicity.detoxify.DetoxifyScore;
import com.xatkit.core.recognition.processor.toxicity.perspectiveapi.PerspectiveApiClient;
import com.xatkit.core.recognition.processor.toxicity.perspectiveapi.PerspectiveApiScore;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.RecognizedIntent;
import lombok.Getter;
import org.apache.commons.configuration2.Configuration;

import static java.util.Objects.nonNull;

/**
 * Annotates {@link RecognizedIntent}s with toxicity scores.
 * <p>
 * This processor supports the following toxicity detection solutions:
 * <ul>
 *     <li>Perspective API</li>
 *     <li>Detoxify</li>
 * </ul>
 * See {@link ToxicityPostProcessorConfiguration} to configure the solution(s) to use.
 * <p>
 * This processor can be configured to use multiple toxicity detection solution. Each solution's scores is associated
 * to a dedicated key in {@link RecognizedIntent#getNlpData()}:
 * <ul>
 *     <li>Perspective API: {@link #PERSPECTIVEAPI_PARAMETER_KEY}</li>
 *     <li>Detoxify: {@link #DETOXIFY_PARAMETER_KEY}</li>
 * </ul>
 */
public class ToxicityPostProcessor implements IntentPostProcessor {

    /**
     * The NLP-data key to access Perspective API scores.
     *
     * @see RecognizedIntent#getNlpData()
     */
    protected static final String PERSPECTIVEAPI_PARAMETER_KEY = "nlp.perspectiveapi";

    /**
     * The NLP-data key to access Detoxify scores.
     *
     * @see RecognizedIntent#getNlpData()
     */
    protected static final String DETOXIFY_PARAMETER_KEY = "nlp.detoxify";


    /**
     * The client used to query PerspectiveAPI.
     */
    @Getter
    private PerspectiveApiClient perspectiveapiClient;

    /**
     * The client used to query Detoxify.
     */
    @Getter
    private DetoxifyClient detoxifyClient;


    /**
     * Initializes the {@link ToxicityPostProcessor} with the toxicity clients set in the {@code configuration}.
     * <p>
     * This processor supports multiple toxicity processors, and merges their results into the
     * {@link RecognizedIntent#getNlpData()} map. See {@link #process(RecognizedIntent, StateContext)} for more
     * information.
     * <p>
     * See {@link ToxicityPostProcessorConfiguration} to configure this processor.
     *
     * @param configuration the Xatkit bot configuration that contains the ToxicityPostProcessor parameters
     * @see ToxicityPostProcessorConfiguration
     */
    public ToxicityPostProcessor(Configuration configuration) {
        ToxicityPostProcessorConfiguration processorConfiguration =
                new ToxicityPostProcessorConfiguration(configuration);
        if (processorConfiguration.usePerspectiveApi()) {
            this.perspectiveapiClient = new PerspectiveApiClient(configuration);
        }
        if (processorConfiguration.useDetoxifyApi()) {
            this.detoxifyClient = new DetoxifyClient(configuration);
        }
    }

    /**
     * Evaluates the toxicity of the {@code recognizedIntent} and stores it in the intent's data.
     * <p>
     * This method stores the toxicity evaluation in {@link RecognizedIntent#getNlpData()}. Each toxicity detection
     * solution's score is defined in a specific class and associated to a specific key:
     * <ul>
     *     <li>Perspective API: score stored in {@link PerspectiveApiScore}, accessible with the key
     *     {@link #PERSPECTIVEAPI_PARAMETER_KEY}</li>
     *     <li>Detoxify: score stored in {@link DetoxifyScore}, accessible with the key {@link #DETOXIFY_PARAMETER_KEY}
     *     </li>
     * </ul>
     *
     * @param recognizedIntent the {@link RecognizedIntent} to process
     * @param context          the {@link StateContext} associated to the {@code recognizedIntent}
     * @return the updated {@code recognizedIntent}
     */
    @Override
    public RecognizedIntent process(RecognizedIntent recognizedIntent, StateContext context) {

        if (nonNull(detoxifyClient)) {
            DetoxifyScore score = detoxifyClient.analyzeRequest(recognizedIntent.getMatchedInput());
            recognizedIntent.getNlpData().put(DETOXIFY_PARAMETER_KEY, score);
        }
        if (nonNull(perspectiveapiClient)) {
            PerspectiveApiScore score = perspectiveapiClient.analyzeRequest(recognizedIntent.getMatchedInput());
            recognizedIntent.getNlpData().put(PERSPECTIVEAPI_PARAMETER_KEY, score);
        }
        return recognizedIntent;
    }
}
