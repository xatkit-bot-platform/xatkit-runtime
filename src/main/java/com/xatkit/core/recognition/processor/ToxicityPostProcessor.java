package com.xatkit.core.recognition.processor;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.RecognizedIntent;

import java.util.ArrayList;

public class ToxicityPostProcessor implements IntentPostProcessor {

    /**
     * The context parameter key used to store the toxicity attributes extracted from the user input.
     */
    protected final static String TOXICITY_PARAMETER_KEY = "nlp.perspectiveapi.";


    @Override
    public void init() {
        String API_KEY = "AIzaSyAcpNUpDO_Dult9LWYOnL_GIWmYztw89Qk";
        ArrayList<PerspectiveapiInterface.AttributeType> attribs = new ArrayList<>();
        attribs.add(PerspectiveapiInterface.AttributeType.TOXICITY);
        new PerspectiveapiInterface(API_KEY, "buenos dias", attribs,
                null, null, null, null);
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
