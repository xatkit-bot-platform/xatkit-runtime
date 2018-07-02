package fr.zelus.jarvis.discord.io;

import fr.zelus.jarvis.discord.JarvisDiscordUtils;
import fr.zelus.jarvis.stubs.StubJarvisCore;
import fr.zelus.jarvis.stubs.discord.StubMessage;
import fr.zelus.jarvis.stubs.discord.StubPrivateMessageReceivedEvent;
import fr.zelus.jarvis.util.VariableLoaderHelper;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.*;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class PrivateMessageListenerTest {

    private static StubJarvisCore stubJarvisCore;

    private DiscordInputProvider discordInputProvider;

    private PrivateMessageListener listener;

    @BeforeClass
    public static void setUpBeforeClass() {
        stubJarvisCore = new StubJarvisCore();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        if(nonNull(stubJarvisCore)) {
            stubJarvisCore.shutdown();
        }
    }

    @Before
    public void setUp() {
        if(nonNull(stubJarvisCore)) {
            stubJarvisCore.clearHandledMessages();
        }
    }

    @After
    public void tearDown() {
        if(nonNull(discordInputProvider)) {
            discordInputProvider.close();
        }
    }

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();


    @Test (expected = NullPointerException.class)
    public void constructNullJarvisCore() {
        listener = new PrivateMessageListener(null);
    }

    @Test
    public void constructValidJarvisCore() {
        listener = new PrivateMessageListener(stubJarvisCore);
        assertThat(listener.getJarvisCore()).as("Non null JarvisCore").isNotNull();
        assertThat(listener.getJarvisCore()).as("Valid JarvisCore").isEqualTo(stubJarvisCore);
    }

    @Test (expected = NullPointerException.class)
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
    }

    private DiscordInputProvider createValidDiscordInputProvider() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JarvisDiscordUtils.DISCORD_TOKEN_KEY, VariableLoaderHelper.getJarvisDiscordToken());
        discordInputProvider = new DiscordInputProvider(stubJarvisCore, configuration);
        return discordInputProvider;
    }

}
