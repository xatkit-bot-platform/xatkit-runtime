package com.xatkit.plugins.discord.platform.io;

import com.xatkit.AbstractJarvisTest;
import com.xatkit.core.session.JarvisSession;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.IntentFactory;
import com.xatkit.plugins.discord.DiscordUtils;
import com.xatkit.plugins.discord.platform.DiscordPlatform;
import com.xatkit.stubs.StubJarvisCore;
import com.xatkit.stubs.discord.StubMessage;
import com.xatkit.stubs.discord.StubPrivateChannel;
import com.xatkit.stubs.discord.StubPrivateMessageReceivedEvent;
import com.xatkit.test.util.VariableLoaderHelper;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class PrivateMessageListenerTest extends AbstractJarvisTest {

    private StubJarvisCore stubJarvisCore;

    private DiscordPlatform discordPlatform;

    private DiscordIntentProvider discordIntentProvider;

    private PrivateMessageListener listener;

    private static EventDefinition VALID_EVENT_DEFINITION;

    @BeforeClass
    public static void setUpBeforeClass() {
        VALID_EVENT_DEFINITION = IntentFactory.eINSTANCE.createIntentDefinition();
        VALID_EVENT_DEFINITION.setName("Default Welcome Intent");
    }

    @Before
    public void setUp() {
        stubJarvisCore = new StubJarvisCore();
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(DiscordUtils.DISCORD_TOKEN_KEY, VariableLoaderHelper.getJarvisDiscordToken());
        discordPlatform = new DiscordPlatform(stubJarvisCore, configuration);
        discordIntentProvider = createValidDiscordInputProvider();
    }

    @After
    public void tearDown() {
        if (nonNull(discordIntentProvider)) {
            discordIntentProvider.close();
        }
        if(nonNull(discordPlatform)) {
            discordPlatform.shutdown();
        }
        if (nonNull(stubJarvisCore)) {
            stubJarvisCore.shutdown();
        }
    }

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();


    @Test(expected = NullPointerException.class)
    public void constructNullJarvisCore() {
        listener = new PrivateMessageListener(null, discordIntentProvider);
    }

    @Test(expected = NullPointerException.class)
    public void constructNullDiscordInputProvider() {
        listener = new PrivateMessageListener(stubJarvisCore, null);
    }

    @Test
    public void constructValidJarvisCore() {
        listener = new PrivateMessageListener(stubJarvisCore, discordIntentProvider);
        assertThat(listener.getJarvisCore()).as("Non null JarvisCore").isNotNull();
        assertThat(listener.getJarvisCore()).as("Valid JarvisCore").isEqualTo(stubJarvisCore);
    }

    @Test(expected = NullPointerException.class)
    public void onPrivateMessageReceivedNullMessage() {
        listener = new PrivateMessageListener(stubJarvisCore, discordIntentProvider);
        listener.onPrivateMessageReceived(null);
    }

    @Test
    public void onPrivateMessageEmptyMessage() {
        listener = new PrivateMessageListener(stubJarvisCore, discordIntentProvider);
        listener.onPrivateMessageReceived(new StubPrivateMessageReceivedEvent(discordIntentProvider.getJdaClient(),
                StubMessage.createEmptyStubMessage()));
        assertThat(stubJarvisCore.getHandledEvents()).as("Empty message skipped").isEmpty();
        assertThat(stubJarvisCore.getJarvisSession(StubPrivateChannel.PRIVATE_CHANNEL_NAME)).as("Null session")
                .isNull();
    }

    @Test
    public void onPrivateMessageValidMessage() {
        listener = new PrivateMessageListener(stubJarvisCore, discordIntentProvider);
        listener.onPrivateMessageReceived(new StubPrivateMessageReceivedEvent(discordIntentProvider.getJdaClient(),
                StubMessage.createTestStubMessage()));
        softly.assertThat(stubJarvisCore.getHandledEvents()).as("Event handled").hasSize(1);
        /*
         * Check equality on names, equals() should not be redefined for EObjects.
         */
        softly.assertThat(stubJarvisCore.getHandledEvents().get(0).getName()).as("Valid Event handled").isEqualTo
                (VALID_EVENT_DEFINITION.getName());
        JarvisSession session = stubJarvisCore.getJarvisSession(StubPrivateChannel.PRIVATE_CHANNEL_ID);
        assertThat(session).as("Not null session").isNotNull();
        Map<String, Object> discordContext = session.getRuntimeContexts().getContextVariables(DiscordUtils
                .DISCORD_CONTEXT_KEY);
        assertThat(discordContext).as("Not null discord context").isNotNull();
        softly.assertThat(discordContext).as("Not empty discord context").isNotEmpty();
        Object contextChannel = discordContext.get(DiscordUtils.CHAT_CHANNEL_CONTEXT_KEY);
        assertThat(contextChannel).as("Not null channel context variable").isNotNull();
        softly.assertThat(contextChannel).as("Channel context variable is a String").isInstanceOf(String.class);
        softly.assertThat(contextChannel).as("Valid channel context variable").isEqualTo(StubPrivateChannel
                .PRIVATE_CHANNEL_ID);
        Object contextUsername = discordContext.get(DiscordUtils.CHAT_USERNAME_CONTEXT_KEY);
        assertThat(contextUsername).as("Not null username context variable").isNotNull();
        softly.assertThat(contextUsername).as("Username context variable is a String").isInstanceOf(String.class);
        softly.assertThat(contextUsername).as("Valid username context variable").isEqualTo(StubMessage
                .TEST_MESSAGE_AUTHOR);
    }

    private DiscordIntentProvider createValidDiscordInputProvider() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(DiscordUtils.DISCORD_TOKEN_KEY, VariableLoaderHelper.getJarvisDiscordToken());
        return new DiscordIntentProvider(discordPlatform, configuration);
    }

}
