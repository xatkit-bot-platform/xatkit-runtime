package com.xatkit.core.recognition.processor;

import com.xatkit.execution.StateContext;
import com.xatkit.intent.RecognizedIntent;
import org.apache.commons.configuration2.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Analyzes the probability of a comment being toxic with different types of toxicity attributes
 */
public class ToxicityPostProcessor implements IntentPostProcessor {

    /**
     * The PerspectiveAPI Client that will ask the requests to PerspectiveAPI
     */
    private PerspectiveapiInterface perspectiveapiClient;
    private DetoxifyInterface detoxifyClient;

    /**
     * The context parameter key used to store the toxicity attributes extracted from the user input.
     */
    protected final static String PERSPECTIVEAPI_PARAMETER_KEY = "nlp.perspectiveapi.";
    protected final static String DETOXIFY_PARAMETER_KEY = "nlp.detoxify.";

    /**
     * Instantiates a new Toxicity post processor.
     *
     * @param configuration the Xatkit bot configuration that contains the ToxicityPostProcessor parameters
     */
    public ToxicityPostProcessor(Configuration configuration) {
        this.perspectiveapiClient = null;
        this.detoxifyClient = null;
        boolean xatkit_perspectiveapi = configuration.getBoolean("xatkit.perspectiveapi", false);
        boolean xatkit_detoxify = configuration.getBoolean("xatkit.detoxify", false);
        if (xatkit_perspectiveapi) {
            this.perspectiveapiClient = new PerspectiveapiInterface(configuration);
        }
        if (xatkit_detoxify) {
            this.detoxifyClient = new DetoxifyInterface(configuration);
        }
    }

    /**
     * Analyzes the comment from the {@code recognizedIntent} and computes the toxic attributes values for that
     * comment. If an attribute can not be set, its value is -1
     *
     * @param recognizedIntent the {@link RecognizedIntent} to process
     * @param context          the {@link StateContext} associated to the {@code recognizedIntent}
     * @return the updated {@code recognizedIntent}
     */
    @Override
    public RecognizedIntent process(RecognizedIntent recognizedIntent, StateContext context) {

        Map<String, Double> results = new HashMap<>();
        if (detoxifyClient != null) {
            Map<String, Double> result = detoxifyClient.analyzeRequest(recognizedIntent.getMatchedInput());
            results.putAll(adaptResult(result, DETOXIFY_PARAMETER_KEY));
        }
        if (perspectiveapiClient != null) {
            Map<String, Double> result = perspectiveapiClient.analyzeRequest(recognizedIntent.getMatchedInput());
            results.putAll(adaptResult(result, PERSPECTIVEAPI_PARAMETER_KEY));
        }
        recognizedIntent.getNlpData().putAll(results);
        return recognizedIntent;
    }

    // TODO documentation
    private Map<String, Double> adaptResult(Map<String, Double> from, String prefix) {
        Map<String, Double> adaptedResult = new HashMap<>();
        from.forEach((k, v) -> {
            adaptedResult.put(prefix + k, v);
        });
        return adaptedResult;
    }

    /**
     * Gets perspectiveapi client. For testing purposes
     *
     * @return the perspectiveapi client
     */
    public PerspectiveapiInterface getPerspectiveapiClient() {
        return perspectiveapiClient;
    }

    /**
     * Gets detoxify client. For testing purposes
     *
     * @return the detoxify client
     */
    public DetoxifyInterface getDetoxifyClient() {
        return detoxifyClient;
    }
}
