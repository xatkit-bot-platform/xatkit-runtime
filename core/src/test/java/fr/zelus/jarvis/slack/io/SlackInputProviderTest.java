package fr.zelus.jarvis.slack.io;

import fr.zelus.jarvis.slack.JarvisSlackUtils;
import fr.zelus.jarvis.stubs.StubJarvisCore;
import fr.zelus.jarvis.util.VariableLoaderHelper;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class SlackInputProviderTest {

    private SlackInputProvider slackInputProvider;

    private StubJarvisCore stubJarvisCore;

    @Before
    public void setUp() {
        stubJarvisCore = new StubJarvisCore();
    }

    public void tearDown() {
        if(nonNull(slackInputProvider)) {
            slackInputProvider.close();
        }
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        slackInputProvider = new SlackInputProvider(stubJarvisCore, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructNoTokenConfiguration() {
        slackInputProvider = new SlackInputProvider(stubJarvisCore, new BaseConfiguration());
    }

    @Test
    public void constructValidConfiguration() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JarvisSlackUtils.SLACK_TOKEN_KEY, VariableLoaderHelper.getJarvisSlackToken());
        slackInputProvider = new SlackInputProvider(stubJarvisCore, configuration);
        assertThat(slackInputProvider.getRtmClient()).as("Not null RTM client").isNotNull();
    }

    @Test
    public void sendValidSlackMessage() throws IOException {
        slackInputProvider = getValidSlackInputProvider();
        slackInputProvider.getRtmClient().onMessage(getValidMessage());
        assertThat(stubJarvisCore.getHandledMessages()).as("Valid handled messages").contains("hello");
    }

    @Test
    public void sendSlackMessageInvalidType() throws IOException {
        slackInputProvider = getValidSlackInputProvider();
        slackInputProvider.getRtmClient().onMessage(getMessageInvalidType());
        assertThat(stubJarvisCore.getHandledMessages()).as("Empty handled messages").isEmpty();
    }

    @Test
    public void sendSlackMessageNullText() throws IOException {
        slackInputProvider = getValidSlackInputProvider();
        slackInputProvider.getRtmClient().onMessage(getMessageNullText());
        assertThat(stubJarvisCore.getHandledMessages()).as("Empty handled messages").isEmpty();
    }

    @Test
    public void sendSlackMessageEmptyMessage() throws IOException {
        slackInputProvider = getValidSlackInputProvider();
        slackInputProvider.getRtmClient().onMessage(getMessageEmptyText());
        assertThat(stubJarvisCore.getHandledMessages()).as("Empty handled messages").isEmpty();
    }

    private SlackInputProvider getValidSlackInputProvider() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JarvisSlackUtils.SLACK_TOKEN_KEY, VariableLoaderHelper.getJarvisSlackToken());
        return new SlackInputProvider(stubJarvisCore, configuration);
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
