package fr.zelus.jarvis.discord.module;

import fr.zelus.jarvis.core.JarvisModule;
import fr.zelus.jarvis.discord.JarvisDiscordUtils;
import net.dv8tion.jda.core.JDA;
import org.apache.commons.configuration2.Configuration;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static fr.zelus.jarvis.discord.JarvisDiscordUtils.DISCORD_TOKEN_KEY;
import static java.util.Objects.nonNull;

/**
 * A {@link JarvisModule} class that connects and interacts with Discord.
 * <p>
 * This module manages a connection to the Discord API, and provides a single
 * {@link fr.zelus.jarvis.core.JarvisAction} to post messages on a given channel.
 * <p>
 * This class is part of jarvis' core modules, and can be used in an orchestration model by importing the <i>core
 * .DiscordModule</i> package.
 */
public class DiscordModule extends JarvisModule {

    /**
     * The {@link JDA} client managing the Discord connection.
     * <p>
     * The {@link JDA} client is created from the Discord bot token stored in this class' {@link Configuration}
     * constructor parameter, and is used to authenticate the bot and post messages through the Discord API.
     *
     * @see #DiscordModule(Configuration)
     * @see JarvisDiscordUtils
     */
    private JDA jdaClient;

    /**
     * Constructs a new {@link DiscordModule} from the provided {@link Configuration}.
     * <p>
     * This constructor initializes the underlying {@link JDA} client with the Discord bot API token retrieved from
     * the {@link Configuration}.
     * <p>
     * <b>Note:</b> {@link DiscordModule} requires a valid Discord bot token to be initialized, and calling the
     * default constructor will throw an {@link IllegalArgumentException} when looking for the Discord bot token.
     *
     * @param configuration the {@link Configuration} used to retrieve the Discord bot token
     * @throws NullPointerException     if the provided {@link Configuration} is {@code null}
     * @throws IllegalArgumentException if the provided Discord bot token is {@code null} or empty
     * @see JarvisDiscordUtils
     */
    public DiscordModule(Configuration configuration) {
        super(configuration);
        checkNotNull(configuration, "Cannot construct a DiscordModule from a null configuration");
        String discordToken = configuration.getString(DISCORD_TOKEN_KEY);
        checkArgument(nonNull(discordToken) && !discordToken.isEmpty(), "Cannot construct a DiscordModule from the " +
                "provided token %s, please ensure that the jarvis configuration contains a valid Discord bot API " +
                "token associated to the key %s", discordToken, DISCORD_TOKEN_KEY);
        jdaClient = JarvisDiscordUtils.getJDA(discordToken);
    }

    /**
     * Returns the {@link JDA} client managing the Discord connection.
     *
     * @return the {@link JDA} client managing the Discord connection
     */
    public JDA getJdaClient() {
        return jdaClient;
    }
}
