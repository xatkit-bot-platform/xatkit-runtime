package com.xatkit.core.recognition.dialogflow;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DialogFlowConfigurationTest {

    private static String PROJECT_ID_VALUE = "PROJECT";

    private Configuration baseConfiguration;

    private DialogFlowConfiguration configuration;

    @Before
    public void setUp() {
        this.baseConfiguration = new BaseConfiguration();
        this.baseConfiguration.addProperty(DialogFlowConfiguration.PROJECT_ID_KEY, PROJECT_ID_VALUE);
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        configuration = new DialogFlowConfiguration(null);
    }

    /*
     * IllegalArgumentException expected because the configuration must contain a project id.
     */
    @Test(expected = IllegalArgumentException.class)
    public void constructEmptyConfiguration() {
        /*
         * Use new BaseConfiguration() to make sure the configuration is empty.
         */
        configuration = new DialogFlowConfiguration(new BaseConfiguration());
    }

    @Test
    public void constructWithProjectId() {
        configuration = new DialogFlowConfiguration(baseConfiguration);
        assertThat(configuration.getProjectId()).isEqualTo(PROJECT_ID_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructWithoutProjectId() {
        /*
         * Use new BaseConfiguration() to make sure the base configuration does not contain the project id.
         */
        configuration = new DialogFlowConfiguration(new BaseConfiguration());
    }

    @Test
    public void constructWithLanguageCode() {
        baseConfiguration.addProperty(DialogFlowConfiguration.LANGUAGE_CODE_KEY, "LANGUAGE");
        configuration = new DialogFlowConfiguration(baseConfiguration);
        assertThat(configuration.getLanguageCode()).isEqualTo("LANGUAGE");
    }

    @Test
    public void constructWithoutLanguageCode() {
        configuration = new DialogFlowConfiguration(baseConfiguration);
        assertThat(configuration.getLanguageCode()).isEqualTo("en-US");
    }

    @Test
    public void constructWithGoogleCredentialsPath() {
        baseConfiguration.addProperty(DialogFlowConfiguration.GOOGLE_CREDENTIALS_PATH_KEY, "PATH");
        configuration = new DialogFlowConfiguration(baseConfiguration);
        assertThat(configuration.getGoogleCredentialsPath()).isEqualTo("PATH");
    }

    @Test
    public void constructWithoutGoogleCredentialsPath() {
        configuration = new DialogFlowConfiguration(baseConfiguration);
        assertThat(configuration.getGoogleCredentialsPath()).isNull();
    }

    @Test
    public void constructWithConfidenceThreshold() {
        baseConfiguration.addProperty(DialogFlowConfiguration.CONFIDENCE_THRESHOLD_KEY, 10.3f);
        configuration = new DialogFlowConfiguration(baseConfiguration);
        assertThat(configuration.getConfidenceThreshold()).isEqualTo(10.3f);
    }

    @Test
    public void constructWithoutConfidenceThreshold() {
        configuration = new DialogFlowConfiguration(baseConfiguration);
        assertThat(configuration.getConfidenceThreshold()).isEqualTo(0);
    }

    @Test
    public void constructWithCleanAgentOnStartup() {
        baseConfiguration.addProperty(DialogFlowConfiguration.CLEAN_AGENT_ON_STARTUP_KEY, true);
        configuration = new DialogFlowConfiguration(baseConfiguration);
        assertThat(configuration.isCleanAgentOnStartup()).isTrue();
    }

    @Test
    public void constructWithoutCleanAgentOnStartup() {
        configuration = new DialogFlowConfiguration(baseConfiguration);
        assertThat(configuration.isCleanAgentOnStartup()).isFalse();
    }

    @Test
    public void constructWithEnableIntentLoading() {
        baseConfiguration.addProperty(DialogFlowConfiguration.ENABLE_INTENT_LOADING_KEY, false);
        configuration = new DialogFlowConfiguration(baseConfiguration);
        assertThat(configuration.isEnableIntentLoader()).isFalse();
    }

    @Test
    public void constructWithoutEnableIntentLoading() {
        configuration = new DialogFlowConfiguration(baseConfiguration);
        assertThat(configuration.isEnableIntentLoader()).isTrue();
    }

    @Test
    public void constructWithEnableEntityLoading() {
        baseConfiguration.addProperty(DialogFlowConfiguration.ENABLE_ENTITY_LOADING_KEY, false);
        configuration = new DialogFlowConfiguration(baseConfiguration);
        assertThat(configuration.isEnableEntityLoader()).isFalse();
    }

    @Test
    public void constructWithoutEnableEntityLoading() {
        configuration = new DialogFlowConfiguration(baseConfiguration);
        assertThat(configuration.isEnableEntityLoader()).isTrue();
    }

    @Test
    public void constructWithCustomFollowupLifespan() {
        baseConfiguration.addProperty(DialogFlowConfiguration.CUSTOM_FOLLOWUP_LIFESPAN, 10);
        configuration = new DialogFlowConfiguration(baseConfiguration);
        assertThat(configuration.getCustomFollowupLifespan()).isEqualTo(10);
    }

    @Test
    public void constructWithoutCustomFollowupLifespan() {
        configuration = new DialogFlowConfiguration(baseConfiguration);
        assertThat(configuration.getCustomFollowupLifespan()).isEqualTo(2);
    }

}