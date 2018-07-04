package fr.zelus.jarvis.plugins.slack.io;

import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.plugins.slack.JarvisSlackUtils;
import fr.zelus.jarvis.stubs.StubJarvisCore;
import fr.zelus.jarvis.util.VariableLoaderHelper;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class SlackInputProviderTest {

    private SlackInputProvider slackInputProvider;

    private StubJarvisCore stubJarvisCore;

    private static String SLACK_CHANNEL = "test";

    @Before
    public void setUp() {
        stubJarvisCore = new StubJarvisCore();
    }

    public void tearDown() {
        if (nonNull(slackInputProvider)) {
            slackInputProvider.close();
        }
        if(nonNull(stubJarvisCore)) {
            stubJarvisCore.shutdown();
        }
    }

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test(expected = NullPointerException.class)
    public void constructNullJarvisCore() {
        slackInputProvider = new SlackInputProvider(null, new BaseConfiguration());
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
        JarvisSession session = stubJarvisCore.getJarvisSession(SLACK_CHANNEL);
        assertThat(session).as("Not null session").isNotNull();
        Map<String, Object> slackContext = session.getJarvisContext().getContextVariables(JarvisSlackUtils.SLACK_CONTEXT_KEY);
        assertThat(slackContext).as("Not null slack context").isNotNull();
        softly.assertThat(slackContext).as("Not empty slack context").isNotEmpty();
        Object contextChannel = slackContext.get(JarvisSlackUtils.SLACK_CHANNEL_CONTEXT_KEY);
        assertThat(contextChannel).as("Not null channel context variable").isNotNull();
        softly.assertThat(contextChannel).as("Channel context variable is a String").isInstanceOf(String.class);
        softly.assertThat(contextChannel).as("Valid channel context variable").isEqualTo(SLACK_CHANNEL);
    }

    @Test
    public void sendSlackMessageInvalidType() {
        slackInputProvider = getValidSlackInputProvider();
        slackInputProvider.getRtmClient().onMessage(getMessageInvalidType());
        assertThat(stubJarvisCore.getHandledMessages()).as("Empty handled messages").isEmpty();
        assertThat(stubJarvisCore.getJarvisSession(SLACK_CHANNEL)).as("Null session").isNull();
    }

    @Test
    public void sendSlackMessageNullText() {
        slackInputProvider = getValidSlackInputProvider();
        slackInputProvider.getRtmClient().onMessage(getMessageNullText());
        assertThat(stubJarvisCore.getHandledMessages()).as("Empty handled messages").isEmpty();
        assertThat(stubJarvisCore.getJarvisSession(SLACK_CHANNEL)).as("Null session").isNull();
    }

    @Test
    public void sendSlackMessageNullChannel() {
        slackInputProvider = getValidSlackInputProvider();
        slackInputProvider.getRtmClient().onMessage(getMessageNullChannel());
        assertThat(stubJarvisCore.getHandledMessages()).as("Empty handled messages").isEmpty();
        assertThat(stubJarvisCore.getJarvisSession(SLACK_CHANNEL)).as("Null session").isNull();
    }

    @Test
    public void sendSlackMessageNullUser() {
        slackInputProvider = getValidSlackInputProvider();
        slackInputProvider.getRtmClient().onMessage(getMessageNullUser());
        assertThat(stubJarvisCore.getHandledMessages()).as("Empty handled messages").isEmpty();
        assertThat(stubJarvisCore.getJarvisSession(SLACK_CHANNEL)).as("Null session").isNull();
    }

    @Test
    public void sendSlackMessageEmptyMessage() {
        slackInputProvider = getValidSlackInputProvider();
        slackInputProvider.getRtmClient().onMessage(getMessageEmptyText());
        assertThat(stubJarvisCore.getHandledMessages()).as("Empty handled messages").isEmpty();
        assertThat(stubJarvisCore.getJarvisSession(SLACK_CHANNEL)).as("Null session").isNull();
    }

    private SlackInputProvider getValidSlackInputProvider() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JarvisSlackUtils.SLACK_TOKEN_KEY, VariableLoaderHelper.getJarvisSlackToken());
        return new SlackInputProvider(stubJarvisCore, configuration);
    }

    private String getValidMessage() {
        return MessageFormat.format("'{'\"type\":\"message\",\"text\":\"hello\", \"channel\":\"{0}\", " +
                        "\"user\":\"123\"'}'", SLACK_CHANNEL);
    }

    private String getMessageInvalidType() {
        return MessageFormat.format("'{'\"type\":\"invalid\",\"text\":\"hello\", \"channel\":\"{0}\", " +
                        "\"user\":\"123\"'}'", SLACK_CHANNEL);
    }

    private String getMessageNullText() {
        return MessageFormat.format("'{'\"type\":\"message\", \"channel\":\"{0}\", \"user\":\"123\"'}'",
                 SLACK_CHANNEL);
    }

    private String getMessageNullChannel() {
        return "{\"type\":\"message\", \"user\":\"123\"}";
    }

    private String getMessageNullUser() {
        return "{\"type\":\"message\"}";
    }

    private String getMessageEmptyText() {
        return MessageFormat.format( "'{'\"type\":\"message\",\"text\":\"\", \"channel\":\"{0}\", " +
                "\"user\":\"123\"'}'", SLACK_CHANNEL);
    }

}
