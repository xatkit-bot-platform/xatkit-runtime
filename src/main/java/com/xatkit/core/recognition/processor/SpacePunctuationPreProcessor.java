package com.xatkit.core.recognition.processor;

import com.xatkit.execution.StateContext;

import java.util.regex.Pattern;

public class SpacePunctuationPreProcessor implements InputPreProcessor {


    @Override
    public String process(String input, StateContext context) {
        return input.replaceAll(Pattern.quote("?"), " ?");
    }
}
