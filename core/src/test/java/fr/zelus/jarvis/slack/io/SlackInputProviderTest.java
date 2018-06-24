package fr.zelus.jarvis.slack.io;

import fr.zelus.jarvis.slack.JarvisSlackUtils;
import fr.zelus.jarvis.util.VariableLoaderHelper;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class SlackInputProviderTest {

    private SlackInputProvider slackInputProvider;

    private PipedInputStream inputStream;

    private BufferedReader reader;

    public void tearDown() throws IOException {
        if(nonNull(reader)) {
            reader.close();
        }
        if(nonNull(slackInputProvider)) {
            slackInputProvider.close();
        }
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        slackInputProvider = new SlackInputProvider(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructNoTokenConfiguration() {
        slackInputProvider = new SlackInputProvider(new BaseConfiguration());
    }

    @Test
    public void constructValidConfiguration() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JarvisSlackUtils.SLACK_TOKEN_KEY, VariableLoaderHelper.getJarvisSlackToken());
        slackInputProvider = new SlackInputProvider(configuration);
        assertThat(slackInputProvider.getOutputStream()).as("Not null output stream").isNotNull();
        assertThat(slackInputProvider.getRtmClient()).as("Not null RTM client").isNotNull();
    }

    @Test
    public void sendValidSlackMessage() throws IOException {
        slackInputProvider = getValidSlackInputProvider();
        bindStreams(slackInputProvider);
        slackInputProvider.getRtmClient().onMessage(getValidMessage());
        assertThat(reader.ready()).as("Stream not empty").isTrue();
        assertThat(reader.readLine()).as("Valid stream content").isEqualTo("hello");
    }

    @Test
    public void sendSlackMessageInvalidType() throws IOException {
        slackInputProvider = getValidSlackInputProvider();
        bindStreams(slackInputProvider);
        slackInputProvider.getRtmClient().onMessage(getMessageInvalidType());
        assertThat(reader.ready()).as("Stream is empty").isFalse();
    }

    @Test
    public void sendSlackMessageNullText() throws IOException {
        slackInputProvider = getValidSlackInputProvider();
        bindStreams(slackInputProvider);
        slackInputProvider.getRtmClient().onMessage(getMessageNullText());
        assertThat(reader.ready()).as("Stream is empty").isFalse();
    }

    @Test
    public void sendSlackMessageEmptyMessage() throws IOException {
        slackInputProvider = getValidSlackInputProvider();
        bindStreams(slackInputProvider);
        slackInputProvider.getRtmClient().onMessage(getMessageEmptyText());
        assertThat(reader.ready()).as("Stream is empty").isFalse();
    }

    private SlackInputProvider getValidSlackInputProvider() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JarvisSlackUtils.SLACK_TOKEN_KEY, VariableLoaderHelper.getJarvisSlackToken());
        return new SlackInputProvider(configuration);
    }

    private void bindStreams(SlackInputProvider slackInputProvider) throws IOException {
        inputStream = new PipedInputStream(slackInputProvider.getOutputStream());
        reader = new BufferedReader(new InputStreamReader(inputStream));
    }

    private String getValidMessage() {
        return "{\"type\":\"message\",\"text\":\"hello\"}";
    }

    private String getMessageInvalidType() {
        return "{\"type\":\"invalid\",\"text\":\"hello\"}";
    }

    private String getMessageNullText() {
        return "{\"type\":\"message\"}";
    }

    private String getMessageEmptyText() {
        return "{\"type\":\"message\",\"text\":\"\"}";
    }

}
