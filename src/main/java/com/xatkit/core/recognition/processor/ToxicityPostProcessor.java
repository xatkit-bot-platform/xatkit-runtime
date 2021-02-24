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
    protected final static String DETOXIFY_PARAMETER_KEY = "nlp.detoxify.original.";

    /**
     * Instantiates a new Toxicity post processor.
     *
     * @param configuration the Xatkit bot configuration that contains the ToxicityPostProcessor parameters
     */
    public ToxicityPostProcessor(Configuration configuration) {
        this.perspectiveapiClient = null;
        this.detoxifyClient = null;
        boolean xatkit_perspectiveapi = configuration.getProperty("xatkit.perspectiveapi") != null &&
                (boolean) configuration.getProperty("xatkit.perspectiveapi");
        boolean xatkit_detoxify = configuration.getProperty("xatkit.detoxify") != null &&
                (boolean) configuration.getProperty("xatkit.detoxify");
        if (xatkit_perspectiveapi) {
            String apiKey = (String) configuration.getProperty("xatkit.perspectiveapi.apiKey");
            String language = (String) configuration.getProperty("xatkit.perspectiveapi.language");
            Boolean doNotStore = (Boolean) configuration.getProperty("xatkit.perspectiveapi.doNotStore");
            String clientToken = (String) configuration.getProperty("xatkit.perspectiveapi.clientToken");
            String sessionId = (String) configuration.getProperty("xatkit.perspectiveapi.sessionId");
            this.perspectiveapiClient = new PerspectiveapiInterface(apiKey, language, doNotStore, clientToken,
                    sessionId,
                    PERSPECTIVEAPI_PARAMETER_KEY);
        }
        if (xatkit_detoxify) {
            this.detoxifyClient = new DetoxifyInterface(DETOXIFY_PARAMETER_KEY);
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
            results.putAll(detoxifyClient.analyzeRequest(recognizedIntent.getMatchedInput()));
        }
        if (perspectiveapiClient != null) {
            results.putAll(perspectiveapiClient.analyzeRequest(recognizedIntent.getMatchedInput()));
        }
        recognizedIntent.getNlpData().putAll(results);
        return recognizedIntent;
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
