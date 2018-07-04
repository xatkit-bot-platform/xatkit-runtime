package fr.zelus.jarvis.plugins.discord.io;

import fr.zelus.jarvis.plugins.discord.JarvisDiscordUtils;
import fr.zelus.jarvis.stubs.StubJarvisCore;
import fr.zelus.jarvis.util.VariableLoaderHelper;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class DiscordInputProviderTest {

    private DiscordInputProvider discordInputProvider;

    private StubJarvisCore stubJarvisCore;

    @Before
    public void setUp() {
        stubJarvisCore = new StubJarvisCore();
    }

    @After
    public void tearDown() {
        if (nonNull(discordInputProvider)) {
            discordInputProvider.close();
        }
    }

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();


    @Test(expected = NullPointerException.class)
    public void constructNullJarvisCore() {
        discordInputProvider = new DiscordInputProvider(null, new BaseConfiguration());
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        discordInputProvider = new DiscordInputProvider(stubJarvisCore, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructNoTokenConfiguration() {
        discordInputProvider = new DiscordInputProvider(stubJarvisCore, new BaseConfiguration());
    }

    @Test
    public void constructValidConfiguration() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JarvisDiscordUtils.DISCORD_TOKEN_KEY, VariableLoaderHelper.getJarvisDiscordToken());
        discordInputProvider = new DiscordInputProvider(stubJarvisCore, configuration);
        assertThat(discordInputProvider.getJdaClient()).as("Not null JDA client").isNotNull();
        List<Object> listeners = discordInputProvider.getJdaClient().getRegisteredListeners();
        softly.assertThat(listeners).as("JDA contains one listener").hasSize(1);
        softly.assertThat(listeners.get(0)).as("Listener is a PrivateMessageListener").isInstanceOf
                (PrivateMessageListener
                        .class);
    }
}
