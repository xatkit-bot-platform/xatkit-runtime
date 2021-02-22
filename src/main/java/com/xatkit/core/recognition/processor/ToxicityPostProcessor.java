package com.xatkit.core.recognition.processor;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.RecognizedIntent;
import org.apache.commons.configuration2.Configuration;

import java.util.Map;

/**
 * Analyzes the probability of a comment being toxic with different types of toxicity attributes
 */
public class ToxicityPostProcessor implements IntentPostProcessor {

    /**
     * The PerspectiveAPI Client that will ask the requests to PerspectiveAPI
     */
    private PerspectiveapiInterface client;

    /**
     * The context parameter key used to store the toxicity attributes extracted from the user input.
     */
    protected final static String TOXICITY_PARAMETER_KEY = "nlp.perspectiveapi.";

    /**
     * Instantiates a new Toxicity post processor.
     *
     * @param configuration the Xatkit bot configuration that contains the ToxicityPostProcessor parameters
     */
    public ToxicityPostProcessor(Configuration configuration) {

        String apiKey = (String) configuration.getProperty("xatkit.perspectiveapi.apiKey");
        String language = (String) configuration.getProperty("xatkit.perspectiveapi.language");
        Boolean doNotStore = (Boolean) configuration.getProperty("xatkit.perspectiveapi.doNotStore");
        String clientToken = (String) configuration.getProperty("xatkit.perspectiveapi.clientToken");
        String sessionId = (String) configuration.getProperty("xatkit.perspectiveapi.sessionId");
        client = new PerspectiveapiInterface(apiKey, language, doNotStore, clientToken, sessionId,
                TOXICITY_PARAMETER_KEY);
    }

    /**
     * Analyzes the comment from the {@code recognizedIntent} and computes the toxic attributes values for that comment.
     *
     * @param recognizedIntent the {@link RecognizedIntent} to process
     * @param context          the {@link StateContext} associated to the {@code recognizedIntent}
     * @return the updated {@code recognizedIntent}
     */
    @Override
    public RecognizedIntent process(RecognizedIntent recognizedIntent, StateContext context) {

        try {
            Map<String, Double> result = client.analyzeRequest(recognizedIntent.getMatchedInput());
            recognizedIntent.getNlpData().putAll(result);
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return recognizedIntent;
    }

    /**
     * Gets client.
     *
     * @return the client
     */
    public PerspectiveapiInterface getClient() {
        return client;
    }
}
