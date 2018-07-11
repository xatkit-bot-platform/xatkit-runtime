package fr.zelus.jarvis.plugins.discord.io;

import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.plugins.discord.JarvisDiscordUtils;
import fr.zelus.jarvis.stubs.StubJarvisCore;
import fr.zelus.jarvis.stubs.discord.StubMessage;
import fr.zelus.jarvis.stubs.discord.StubPrivateChannel;
import fr.zelus.jarvis.stubs.discord.StubPrivateMessageReceivedEvent;
import fr.zelus.jarvis.util.VariableLoaderHelper;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class PrivateMessageListenerTest {

    private StubJarvisCore stubJarvisCore;

    private DiscordInputProvider discordInputProvider;

    private PrivateMessageListener listener;

    @Before
    public void setUp() {
        stubJarvisCore = new StubJarvisCore();

    }

    @After
    public void tearDown() {
        if (nonNull(discordInputProvider)) {
            discordInputProvider.close();
        }
        if (nonNull(stubJarvisCore)) {
            stubJarvisCore.shutdown();
        }
    }

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();


    @Test(expected = NullPointerException.class)
    public void constructNullJarvisCore() {
        listener = new PrivateMessageListener(null);
    }

    @Test
    public void constructValidJarvisCore() {
        listener = new PrivateMessageListener(stubJarvisCore);
        assertThat(listener.getJarvisCore()).as("Non null JarvisCore").isNotNull();
        assertThat(listener.getJarvisCore()).as("Valid JarvisCore").isEqualTo(stubJarvisCore);
    }

    @Test(expected = NullPointerException.class)
    public void onPrivateMessageReceivedNullMessage() {
        listener = new PrivateMessageListener(stubJarvisCore);
        listener.onPrivateMessageReceived(null);
    }

    @Test
    public void onPrivateMessageEmptyMessage() {
        discordInputProvider = createValidDiscordInputProvider();
        listener = new PrivateMessageListener(stubJarvisCore);
        listener.onPrivateMessageReceived(new StubPrivateMessageReceivedEvent(discordInputProvider.getJdaClient(),
                StubMessage.createEmptyStubMessage()));
        assertThat(stubJarvisCore.getHandledMessages()).as("Empty message skipped").isEmpty();
        assertThat(stubJarvisCore.getJarvisSession(StubPrivateChannel.PRIVATE_CHANNEL_NAME)).as("Null session")
                .isNull();
    }

    @Test
    public void onPrivateMessageValidMessage() {
        discordInputProvider = createValidDiscordInputProvider();
        listener = new PrivateMessageListener(stubJarvisCore);
        listener.onPrivateMessageReceived(new StubPrivateMessageReceivedEvent(discordInputProvider.getJdaClient(),
                StubMessage.createTestStubMessage()));
        softly.assertThat(stubJarvisCore.getHandledMessages()).as("Message handled").hasSize(1);
        softly.assertThat(stubJarvisCore.getHandledMessages().get(0)).as("Valid Message handled").isEqualTo
                (StubMessage.TEST_MESSAGE_CONTENT);
        JarvisSession session = stubJarvisCore.getJarvisSession(StubPrivateChannel.PRIVATE_CHANNEL_NAME);
        assertThat(session).as("Not null session").isNotNull();
        Map<String, Object> discordContext = session.getJarvisContext().getContextVariables(JarvisDiscordUtils
                .DISCORD_CONTEXT_KEY);
        assertThat(discordContext).as("Not null discord context").isNotNull();
        softly.assertThat(discordContext).as("Not empty discord context").isNotEmpty();
        Object contextChannel = discordContext.get(JarvisDiscordUtils.DISCORD_CHANNEL_CONTEXT_KEY);
        assertThat(contextChannel).as("Not null channel context variable").isNotNull();
        softly.assertThat(contextChannel).as("Channel context variable is a String").isInstanceOf(String.class);
        softly.assertThat(contextChannel).as("Valid channel context variable").isEqualTo(StubPrivateChannel
                .PRIVATE_CHANNEL_ID);
        Object contextUsername = discordContext.get(JarvisDiscordUtils.DISCORD_USERNAME_CONTEXT_KEY);
        assertThat(contextUsername).as("Not null username context variable").isNotNull();
        softly.assertThat(contextUsername).as("Username context variable is a String").isInstanceOf(String.class);
        softly.assertThat(contextUsername).as("Valid username context variable").isEqualTo(StubMessage
                .TEST_MESSAGE_AUTHOR);
    }

    private DiscordInputProvider createValidDiscordInputProvider() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JarvisDiscordUtils.DISCORD_TOKEN_KEY, VariableLoaderHelper.getJarvisDiscordToken());
        discordInputProvider = new DiscordInputProvider(stubJarvisCore, configuration);
        return discordInputProvider;
    }

}
