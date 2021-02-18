package com.xatkit.core.recognition.processor;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.RecognizedIntent;

public class ToxicityPostProcessor implements IntentPostProcessor {

    /**
     * The API key of your project
     */
    String API_KEY = "YOUR PERSPECTIVEAPI KEY";

    /**
     * The context parameter key used to store the toxicity attributes extracted from the user input.
     */
    protected final static String TOXICITY_PARAMETER_KEY = "nlp.perspectiveapi.";


    @Override
    public void init() {
        new PerspectiveapiInterface(API_KEY, "en", null, null,null);
    }

    @Override
    public RecognizedIntent process(RecognizedIntent recognizedIntent, StateContext context) {

        PerspectiveapiInterface.setCommentText(recognizedIntent.getMatchedInput());
        try {
            recognizedIntent.getNlpData().putAll(PerspectiveapiInterface.analyzeRequest(TOXICITY_PARAMETER_KEY));
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return recognizedIntent;
    }
}
