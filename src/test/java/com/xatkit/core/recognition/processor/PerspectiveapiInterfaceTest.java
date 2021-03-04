package com.xatkit.core.recognition.processor;

import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class PerspectiveapiInterfaceTest {

    private Configuration configuration;

    private PerspectiveapiInterface client;
    private Set<String> languages;

    @Before
    public void setUp() {
        this.configuration = new BaseConfiguration();
        configuration.addProperty(PerspectiveapiConfiguration.API_KEY, "YOUR PERSPECTIVE API KEY");
        this.client = new PerspectiveapiInterface(configuration);
        this.languages = this.client.getLanguageAttributes().keySet();
    }

    @Test
    public void analyzeRequestTest() throws UnirestException {
        for (String language : languages) {
            configuration.setProperty(PerspectiveapiConfiguration.LANGUAGE, language);
            client = new PerspectiveapiInterface(configuration);
            Map<String, Double> scores = client.analyzeRequest("test");

            ToxicityLabel[] languageAttributes = client.getLanguageAttributes().get(language);
            for (ToxicityLabel attribute : languageAttributes) {
                Double score = scores.get(attribute.toString());
                assertThat(score).isBetween(0d,1d);
            }
        }
    }
}