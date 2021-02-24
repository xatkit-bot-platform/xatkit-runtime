package com.xatkit.core.recognition.processor;

import com.mashape.unirest.http.exceptions.UnirestException;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class PerspectiveapiInterfaceTest {

    private String apiKey;
    private PerspectiveapiInterface client;
    private Set<String> languages;

    @Before
    public void setUp() {
        this.apiKey = "YOUR-PERSPECTIVEAPI-KEY";
        this.client = new PerspectiveapiInterface(apiKey, null, null, null, null,
                ToxicityPostProcessor.PERSPECTIVEAPI_PARAMETER_KEY);
        this.languages = this.client.getLanguageAttributes().keySet();
    }

    @Test
    public void analyzeRequestTest() throws UnirestException {
        for (String language : languages) {
            client = new PerspectiveapiInterface(apiKey, language, null, null, null,
                    ToxicityPostProcessor.PERSPECTIVEAPI_PARAMETER_KEY);
            Map<String, Double> scores = client.analyzeRequest("test");

            PerspectiveapiInterface.AttributeType[] languageAttributes = client.getLanguageAttributes().get(language);
            for (PerspectiveapiInterface.AttributeType attribute : languageAttributes) {
                Double score = scores.get(ToxicityPostProcessor.PERSPECTIVEAPI_PARAMETER_KEY + attribute.toString());
                assertTrue(score >= 0);
                assertTrue(score <= 1);
            }
        }
    }
}