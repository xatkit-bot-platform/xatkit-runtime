package edu.uoc.som.jarvis.plugins.discord.platform;

import edu.uoc.som.jarvis.core.JarvisCore;
import edu.uoc.som.jarvis.core.platform.RuntimePlatform;
import edu.uoc.som.jarvis.core.platform.action.RuntimeAction;
import edu.uoc.som.jarvis.core.session.JarvisSession;
import edu.uoc.som.jarvis.plugins.chat.platform.ChatPlatform;
import edu.uoc.som.jarvis.plugins.discord.DiscordUtils;
import edu.uoc.som.jarvis.plugins.discord.platform.action.PostMessage;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.MessageChannel;
import org.apache.commons.configuration2.Configuration;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

/**
 * A {@link RuntimePlatform} class that connects and interacts with Discord.
 * <p>
 * This runtimePlatform manages a connection to the Discord API, and provides a set of
 * {@link RuntimeAction}s to interact with the Discord API:
 * <ul>
 * <li>{@link edu.uoc.som.jarvis.plugins.discord.platform.action.Reply}: reply to a user input</li>
 * <li>{@link PostMessage}: post a message to a given Discord
 * channel</li>
 * </ul>
 * <p>
 * This class is part of jarvis' core platforms, and can be used in an execution model by importing the
 * <i>DiscordPlatform</i> package.
 */
public class DiscordPlatform extends ChatPlatform {

    /**
     * The {@link JDA} client managing the Discord connection.
     * <p>
     * The {@link JDA} client is created from the Discord bot token stored in this class' {@link Configuration}
     * constructor parameter, and is used to authenticate the bot and post messages through the Discord API.
     *
     * @see #DiscordPlatform(JarvisCore, Configuration)
     * @see DiscordUtils
     */
    private JDA jdaClient;

    /**
     * Constructs a new {@link DiscordPlatform} from the provided {@link JarvisCore} and {@link Configuration}.
     * <p>
     * This constructor initializes the underlying {@link JDA} client with the Discord bot API token retrieved from
     * the {@link Configuration}.
     * <p>
     * <b>Note:</b> {@link DiscordPlatform} requires a valid Discord bot token to be initialized, and calling the
     * default constructor will throw an {@link IllegalArgumentException} when looking for the Discord bot token.
     *
     * @param jarvisCore    the {@link JarvisCore} instance associated to this runtimePlatform
     * @param configuration the {@link Configuration} used to retrieve the Discord bot token
     * @throws NullPointerException     if the provided {@code jarvisCore} or {@link Configuration} is {@code null}
     * @throws IllegalArgumentException if the provided Discord bot token is {@code null} or empty
     * @see DiscordUtils
     */
    public DiscordPlatform(JarvisCore jarvisCore, Configuration configuration) {
        super(jarvisCore, configuration);
        String discordToken = configuration.getString(DiscordUtils.DISCORD_TOKEN_KEY);
        checkArgument(nonNull(discordToken) && !discordToken.isEmpty(), "Cannot construct a DiscordPlatform from the " +
                "provided token %s, please ensure that the jarvis configuration contains a valid Discord bot API " +
                "token associated to the key %s", discordToken, DiscordUtils.DISCORD_TOKEN_KEY);
        jdaClient = DiscordUtils.getJDA(discordToken);
    }

    /**
     * Returns the {@link JDA} client managing the Discord connection.
     *
     * @return the {@link JDA} client managing the Discord connection
     */
    public JDA getJdaClient() {
        return jdaClient;
    }

    /**
     * Returns the {@link JarvisSession} associated to the provided {@link MessageChannel}.
     *
     * @param messageChannel the {@link MessageChannel} to retrieve the {@link JarvisSession} from
     * @return the {@link JarvisSession} associated to the provided {@link MessageChannel}
     */
    public JarvisSession createSessionFromChannel(MessageChannel messageChannel) {
        return this.jarvisCore.getOrCreateJarvisSession(messageChannel.getId());
    }
}
