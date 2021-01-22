package com.xatkit.core.recognition.nlpjs.model;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
public class Intent {

    private String intentName;

    @Singular
    private List<IntentExample> examples;

    @Singular
    private List<IntentParameter> parameters;
}
