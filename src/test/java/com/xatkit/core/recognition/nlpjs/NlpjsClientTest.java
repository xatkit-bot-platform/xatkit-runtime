package com.xatkit.core.recognition.nlpjs;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.recognition.nlpjs.model.Agent;
import com.xatkit.core.recognition.nlpjs.model.AgentConfig;
import com.xatkit.core.recognition.nlpjs.model.Intent;
import com.xatkit.core.recognition.nlpjs.model.IntentExample;
import com.xatkit.core.recognition.nlpjs.model.RecognitionResult;
import com.xatkit.core.recognition.nlpjs.model.TrainingData;
import com.xatkit.core.recognition.nlpjs.model.UserMessage;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link NlpjsClient}.
 * <p>
 * Note: this class requires a local instance of the NLP.js server running on port 8080.
 */
public class NlpjsClientTest extends AbstractXatkitTest {

    private static final String VALID_URL = "http://localhost:8080";

    private static final String VALID_AGENT = "default";

    private NlpjsClient nlpjsClient;

    @Before
    public void setUp() {
        this.nlpjsClient = null;
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        this.nlpjsClient = new NlpjsClient(null);
    }

    @Test
    public void constructValidConfiguration() {
        this.nlpjsClient = new NlpjsClient(getValidConfiguration());
        assertThat(nlpjsClient.isShutdown()).isFalse();
    }

    @Test
    public void getAgentInfoValidAgentId() {
        this.nlpjsClient = getNlpjsClient();
        Agent agent = this.nlpjsClient.getAgentInfo("default");
        assertThat(agent).isNotNull();
        assertThat(agent.getStatus().getValue()).isEqualTo("ready");
    }

    @Test(expected = NullPointerException.class)
    public void getAgentInfoNullAgentId() {
        this.nlpjsClient = getNlpjsClient();
        this.nlpjsClient.getAgentInfo(null);
    }

    @Test(expected = NlpjsClientException.class)
    public void getAgentInfoInvalidAgentId() {
        this.nlpjsClient = getNlpjsClient();
        this.nlpjsClient.getAgentInfo("invalid");
    }

    /*
     * TODO test createAgent methods once we have a way to delete agents.
     */

    @Test
    public void trainAgentValidAgentIdValidTrainingData() {
        this.nlpjsClient = getNlpjsClient();
        boolean trained = this.nlpjsClient.trainAgent("default", getTestTrainingData());
        assertThat(trained).isTrue();
    }

    @Test(expected = NullPointerException.class)
    public void trainAgentNullAgentId() {
        this.nlpjsClient = getNlpjsClient();
        this.nlpjsClient.trainAgent(null, getTestTrainingData());
    }

    @Test(expected = NlpjsClientException.class)
    public void trainAgentInvalidAgentId() {
        this.nlpjsClient = getNlpjsClient();
        this.nlpjsClient.trainAgent("invalid", getTestTrainingData());
    }

    @Test(expected = NullPointerException.class)
    public void trainAgentNullTrainingData() {
        this.nlpjsClient = getNlpjsClient();
        this.nlpjsClient.trainAgent("default", null);
    }

    @Test
    public void getIntentValidAgentIdValidUserMessage() {
        this.nlpjsClient = getNlpjsClient();
        RecognitionResult recognitionResult = this.nlpjsClient.getIntent("default", new UserMessage("Hello"));
        /*
         * It's not important to check the content of the RecognitionResult: we are using an untrained agent so the
         * recognition is not relevant.
         */
        assertThat(recognitionResult.getUtterance()).isEqualTo("Hello");
    }

    @Test(expected = NullPointerException.class)
    public void getIntentNullAgentId() {
        this.nlpjsClient = getNlpjsClient();
        this.nlpjsClient.getIntent(null, new UserMessage("Hello"));
    }

    @Test(expected = NlpjsClientException.class)
    public void getIntentInvalidAgentId() {
        this.nlpjsClient = getNlpjsClient();
        this.nlpjsClient.getIntent("invalid", new UserMessage("Hello"));
    }

    @Test(expected = NullPointerException.class)
    public void getIntentNullUserMessage() {
        this.nlpjsClient = getNlpjsClient();
        this.nlpjsClient.getIntent("default", null);
    }

    @Test
    public void isShutdown() {
        this.nlpjsClient = getNlpjsClient();
        assertThat(nlpjsClient.isShutdown()).isFalse();
    }

    private NlpjsClient getNlpjsClient() {
        return new NlpjsClient(this.getValidConfiguration());
    }

    private NlpjsConfiguration getValidConfiguration() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(NlpjsConfiguration.NLPJS_SERVER_KEY, VALID_URL);
        configuration.addProperty(NlpjsConfiguration.AGENT_ID_KEY, VALID_AGENT);
        return new NlpjsConfiguration(configuration);
    }

    private TrainingData getTestTrainingData() {
        return TrainingData.builder()
                .config(new AgentConfig("en", true))
                .intents(Collections.singletonList(
                        Intent.builder()
                                .intentName("TestIntent")
                                .example(new IntentExample("Hello"))
                                .build()))
                .build();
    }
}
