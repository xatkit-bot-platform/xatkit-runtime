package fr.zelus.jarvis.plugins.discord.module.io;

import fr.zelus.jarvis.AbstractJarvisTest;
import fr.zelus.jarvis.plugins.discord.JarvisDiscordUtils;
import fr.zelus.jarvis.plugins.discord.module.DiscordModule;
import fr.zelus.jarvis.stubs.StubJarvisCore;
import fr.zelus.jarvis.test.util.VariableLoaderHelper;
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

public class DiscordIntentProviderTest extends AbstractJarvisTest {

    private DiscordIntentProvider discordIntentProvider;

    private StubJarvisCore stubJarvisCore;

    private DiscordModule discordModule;

    @Before
    public void setUp() {
        stubJarvisCore = new StubJarvisCore();
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JarvisDiscordUtils.DISCORD_TOKEN_KEY, VariableLoaderHelper.getJarvisDiscordToken());
        discordModule = new DiscordModule(stubJarvisCore, configuration);
    }

    @After
    public void tearDown() {
        if (nonNull(discordIntentProvider)) {
            discordIntentProvider.close();
        }
        if(nonNull(discordModule)) {
            discordModule.shutdown();
        }
        if (nonNull(stubJarvisCore)) {
            stubJarvisCore.shutdown();
        }
    }

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();


    @Test(expected = NullPointerException.class)
    public void constructNullJarvisCore() {
        discordIntentProvider = new DiscordIntentProvider(null, new BaseConfiguration());
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        discordIntentProvider = new DiscordIntentProvider(discordModule, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructNoTokenConfiguration() {
        discordIntentProvider = new DiscordIntentProvider(discordModule, new BaseConfiguration());
    }

    @Test
    public void constructValidConfiguration() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JarvisDiscordUtils.DISCORD_TOKEN_KEY, VariableLoaderHelper.getJarvisDiscordToken());
        discordIntentProvider = new DiscordIntentProvider(discordModule, configuration);
        assertThat(discordIntentProvider.getJdaClient()).as("Not null JDA client").isNotNull();
        List<Object> listeners = discordIntentProvider.getJdaClient().getRegisteredListeners();
        softly.assertThat(listeners).as("JDA contains one listener").hasSize(1);
        softly.assertThat(listeners.get(0)).as("Listener is a PrivateMessageListener").isInstanceOf
                (PrivateMessageListener
                        .class);
    }
}
