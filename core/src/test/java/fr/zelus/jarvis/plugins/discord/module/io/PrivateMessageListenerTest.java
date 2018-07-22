package fr.zelus.jarvis.plugins.discord.module.io;

import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.intent.EventDefinition;
import fr.zelus.jarvis.intent.IntentFactory;
import fr.zelus.jarvis.plugins.discord.JarvisDiscordUtils;
import fr.zelus.jarvis.stubs.StubJarvisCore;
import fr.zelus.jarvis.stubs.discord.StubMessage;
import fr.zelus.jarvis.stubs.discord.StubPrivateChannel;
import fr.zelus.jarvis.stubs.discord.StubPrivateMessageReceivedEvent;
import fr.zelus.jarvis.util.VariableLoaderHelper;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.*;

import java.util.Map;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class PrivateMessageListenerTest {

    private StubJarvisCore stubJarvisCore;

    private DiscordInputProvider discordInputProvider;

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
        discordInputProvider = createValidDiscordInputProvider();
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
        listener = new PrivateMessageListener(null, discordInputProvider);
    }

    @Test(expected = NullPointerException.class)
    public void constructNullDiscordInputProvider() {
        listener = new PrivateMessageListener(stubJarvisCore, null);
    }

    @Test
    public void constructValidJarvisCore() {
        listener = new PrivateMessageListener(stubJarvisCore, discordInputProvider);
        assertThat(listener.getJarvisCore()).as("Non null JarvisCore").isNotNull();
        assertThat(listener.getJarvisCore()).as("Valid JarvisCore").isEqualTo(stubJarvisCore);
    }

    @Test(expected = NullPointerException.class)
    public void onPrivateMessageReceivedNullMessage() {
        listener = new PrivateMessageListener(stubJarvisCore, discordInputProvider);
        listener.onPrivateMessageReceived(null);
    }

    @Test
    public void onPrivateMessageEmptyMessage() {
        listener = new PrivateMessageListener(stubJarvisCore, discordInputProvider);
        listener.onPrivateMessageReceived(new StubPrivateMessageReceivedEvent(discordInputProvider.getJdaClient(),
                StubMessage.createEmptyStubMessage()));
        assertThat(stubJarvisCore.getHandledEvents()).as("Empty message skipped").isEmpty();
        assertThat(stubJarvisCore.getJarvisSession(StubPrivateChannel.PRIVATE_CHANNEL_NAME)).as("Null session")
                .isNull();
    }

    @Test
    public void onPrivateMessageValidMessage() {
        listener = new PrivateMessageListener(stubJarvisCore, discordInputProvider);
        listener.onPrivateMessageReceived(new StubPrivateMessageReceivedEvent(discordInputProvider.getJdaClient(),
                StubMessage.createTestStubMessage()));
        softly.assertThat(stubJarvisCore.getHandledEvents()).as("Event handled").hasSize(1);
        /*
         * Check equality on names, equals() should not be redefined for EObjects.
         */
        softly.assertThat(stubJarvisCore.getHandledEvents().get(0).getName()).as("Valid Event handled").isEqualTo
                (VALID_EVENT_DEFINITION.getName());
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
        return new DiscordInputProvider(stubJarvisCore, configuration);
    }

}
