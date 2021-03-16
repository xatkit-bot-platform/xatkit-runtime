package com.xatkit.core.recognition.dialogflow;

import com.xatkit.core.recognition.IntentRecognitionProviderException;
import com.xatkit.test.util.VariableLoaderHelper;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class DialogFlowClientsTest {

    private DialogFlowConfiguration validConfiguration;

    private DialogFlowClients dialogFlowClients;

    @Before
    public void setUp() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(DialogFlowConfiguration.PROJECT_ID_KEY,
                VariableLoaderHelper.getVariable(DialogFlowConfiguration.PROJECT_ID_KEY));
        configuration.addProperty(DialogFlowConfiguration.GOOGLE_CREDENTIALS_PATH_KEY, VariableLoaderHelper
                .getVariable(DialogFlowConfiguration.GOOGLE_CREDENTIALS_PATH_KEY));
        this.validConfiguration = new DialogFlowConfiguration(configuration);
    }

    @After
    public void tearDown() {
        if(nonNull(dialogFlowClients)) {
            dialogFlowClients.shutdown();
        }
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() throws IntentRecognitionProviderException {
        dialogFlowClients = new DialogFlowClients(null);
    }

    @Test
    public void constructValidConfiguration() throws IntentRecognitionProviderException {
        dialogFlowClients = new DialogFlowClients(validConfiguration);
        assertThat(dialogFlowClients).isNotNull();
        assertThat(dialogFlowClients.isShutdown()).isFalse();
        assertThat(dialogFlowClients.getAgentsClient()).isNotNull();
        assertThat(dialogFlowClients.getAgentsClient().isShutdown()).isFalse();
        assertThat(dialogFlowClients.getSessionsClient()).isNotNull();
        assertThat(dialogFlowClients.getSessionsClient().isShutdown()).isFalse();
        assertThat(dialogFlowClients.getIntentsClient()).isNotNull();
        assertThat(dialogFlowClients.getIntentsClient().isShutdown()).isFalse();
        assertThat(dialogFlowClients.getEntityTypesClient()).isNotNull();
        assertThat(dialogFlowClients.getEntityTypesClient().isShutdown()).isFalse();
    }

    @Test(expected = IntentRecognitionProviderException.class)
    public void constructNoPathInConfiguration() throws IntentRecognitionProviderException {
        Configuration configuration = this.validConfiguration.getBaseConfiguration();
        configuration.setProperty(DialogFlowConfiguration.GOOGLE_CREDENTIALS_PATH_KEY, null);
        DialogFlowConfiguration dialogFlowConfiguration = new DialogFlowConfiguration(configuration);
        dialogFlowClients = new DialogFlowClients(dialogFlowConfiguration);
    }

    @Test
    public void shutdown() throws IntentRecognitionProviderException {
        dialogFlowClients = new DialogFlowClients(validConfiguration);
        dialogFlowClients.shutdown();
        assertThat(dialogFlowClients.isShutdown()).isTrue();
        assertThat(dialogFlowClients.getAgentsClient().isShutdown()).isTrue();
        assertThat(dialogFlowClients.getSessionsClient().isShutdown()).isTrue();
        assertThat(dialogFlowClients.getIntentsClient().isShutdown()).isTrue();
        assertThat(dialogFlowClients.getEntityTypesClient().isShutdown()).isTrue();
    }
}
