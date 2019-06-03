package com.xatkit.plugins.discord.platform.io;

import com.xatkit.plugins.discord.DiscordUtils;
import com.xatkit.plugins.discord.platform.DiscordPlatform;
import com.xatkit.AbstractXatkitTest;
import com.xatkit.stubs.StubXatkitCore;
import com.xatkit.test.util.VariableLoaderHelper;
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

public class DiscordIntentProviderTest extends AbstractXatkitTest {

    private DiscordIntentProvider discordIntentProvider;

    private StubXatkitCore stubXatkitCore;

    private DiscordPlatform discordPlatform;

    @Before
    public void setUp() {
        stubXatkitCore = new StubXatkitCore();
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(DiscordUtils.DISCORD_TOKEN_KEY, VariableLoaderHelper.getXatkitDiscordToken());
        discordPlatform = new DiscordPlatform(stubXatkitCore, configuration);
    }

    @After
    public void tearDown() {
        if (nonNull(discordIntentProvider)) {
            discordIntentProvider.close();
        }
        if(nonNull(discordPlatform)) {
            discordPlatform.shutdown();
        }
        if (nonNull(stubXatkitCore)) {
            stubXatkitCore.shutdown();
        }
    }

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();


    @Test(expected = NullPointerException.class)
    public void constructNullXatkitCore() {
        discordIntentProvider = new DiscordIntentProvider(null, new BaseConfiguration());
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        discordIntentProvider = new DiscordIntentProvider(discordPlatform, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructNoTokenConfiguration() {
        discordIntentProvider = new DiscordIntentProvider(discordPlatform, new BaseConfiguration());
    }

    @Test
    public void constructValidConfiguration() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(DiscordUtils.DISCORD_TOKEN_KEY, VariableLoaderHelper.getXatkitDiscordToken());
        discordIntentProvider = new DiscordIntentProvider(discordPlatform, configuration);
        assertThat(discordIntentProvider.getJdaClient()).as("Not null JDA client").isNotNull();
        List<Object> listeners = discordIntentProvider.getJdaClient().getRegisteredListeners();
        softly.assertThat(listeners).as("JDA contains one listener").hasSize(1);
        softly.assertThat(listeners.get(0)).as("Listener is a PrivateMessageListener").isInstanceOf
                (PrivateMessageListener.class);
    }
}
