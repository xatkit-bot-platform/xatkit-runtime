package com.xatkit.core.recognition.nlpjs;

import com.xatkit.AbstractXatkitTest;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NlpjsConfigurationTest extends AbstractXatkitTest {

    private static final String TEST_NLPJS_SERVER = "http://localhost:8080";

    private static final String TEST_AGENT_ID = "default";

    private Configuration baseConfiguration;

    private NlpjsConfiguration configuration;

    @Before
    public void setUp() {
        /*
         * Create a minimal configuration with required properties.
         */
        this.baseConfiguration = new BaseConfiguration();
        this.baseConfiguration.addProperty(NlpjsConfiguration.NLPJS_SERVER_KEY, TEST_NLPJS_SERVER);
        this.baseConfiguration.addProperty(NlpjsConfiguration.AGENT_ID_KEY, TEST_AGENT_ID);
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        this.configuration = new NlpjsConfiguration(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructEmptyConfiguration() {
        this.configuration = new NlpjsConfiguration(new BaseConfiguration());
    }

    @Test
    public void constructValidProjectIdValidAgentId() {
        this.configuration = new NlpjsConfiguration(this.baseConfiguration);
        assertThat(configuration.getNlpjsServer()).isEqualTo(TEST_NLPJS_SERVER);
        assertThat(configuration.getAgentId()).isEqualTo(TEST_AGENT_ID);
    }

    @Test (expected = IllegalArgumentException.class)
    public void constructWithoutProjectId() {
        Configuration baseConfiguration = new BaseConfiguration();
        baseConfiguration.addProperty(NlpjsConfiguration.AGENT_ID_KEY, TEST_AGENT_ID);
        this.configuration = new NlpjsConfiguration(baseConfiguration);
    }

    @Test (expected = IllegalArgumentException.class)
    public void constructWithoutAgentId() {
        Configuration baseConfiguration = new BaseConfiguration();
        baseConfiguration.addProperty(NlpjsConfiguration.NLPJS_SERVER_KEY, TEST_NLPJS_SERVER);
        this.configuration = new NlpjsConfiguration(baseConfiguration);
    }

    @Test
    public void constructWithLanguageCode() {
        this.baseConfiguration.addProperty(NlpjsConfiguration.LANGUAGE_CODE_KEY, "es");
        this.configuration = new NlpjsConfiguration(this.baseConfiguration);
        assertThat(configuration.getLanguageCode()).isEqualTo("es");

        this.baseConfiguration.addProperty(NlpjsConfiguration.LANGUAGE_CODE_KEY, "es_ES");
        this.configuration = new NlpjsConfiguration(this.baseConfiguration);
        assertThat(configuration.getLanguageCode()).isEqualTo("es");
    }

    @Test
    public void constructWithoutLanguageCode() {
        this.configuration = new NlpjsConfiguration(this.baseConfiguration);
        assertThat(configuration.getLanguageCode()).isEqualTo(NlpjsConfiguration.DEFAULT_LANGUAGE_CODE);
    }

    @Test
    public void constructWithCleanOnStartup() {
        this.baseConfiguration.addProperty(NlpjsConfiguration.CLEAN_AGENT_ON_STARTUP_KEY, false);
        this.configuration = new NlpjsConfiguration(this.baseConfiguration);
        assertThat(configuration.isCleanAgentOnStartup()).isFalse();
    }

    @Test
    public void constructWithoutCleanOnStartup() {
        this.configuration = new NlpjsConfiguration(this.baseConfiguration);
        assertThat(configuration.isCleanAgentOnStartup()).isTrue();
    }
}
