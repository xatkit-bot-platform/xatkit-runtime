package com.xatkit.core.recognition.processor.toxicity.perspectiveapi;

import com.xatkit.test.util.VariableLoaderHelper;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Set;

import static org.assertj.core.api.Java6Assertions.assertThat;

@Ignore("Tests aren't working at the moment, to be improved")
public class PerspectiveApiClientTest {

    private Configuration configuration;

    private PerspectiveApiClient client;
    private Set<String> languages;

    @Before
    public void setUp() {
        this.configuration = new BaseConfiguration();
        configuration.addProperty(PerspectiveApiConfiguration.API_KEY,
                VariableLoaderHelper.getVariable(PerspectiveApiConfiguration.API_KEY));
        this.client = new PerspectiveApiClient(configuration);
        this.languages = this.client.getLanguageLabels().keySet();
    }

    @Test
    public void analyzeRequestTest() {
        for (String language : languages) {
            configuration.setProperty(PerspectiveApiConfiguration.LANGUAGE, language);
            client = new PerspectiveApiClient(configuration);
            PerspectiveApiScore scores = client.analyzeRequest("test");
            PerspectiveApiLabel[] languageAttributes = client.getLanguageLabels().get(language);
            for (PerspectiveApiLabel attribute : languageAttributes) {
                Double score = scores.getScore(attribute);
                assertThat(score).isBetween(0d, 1d);
            }
        }
    }
}
