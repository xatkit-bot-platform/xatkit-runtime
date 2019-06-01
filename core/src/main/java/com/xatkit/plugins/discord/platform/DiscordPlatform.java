package com.xatkit.plugins.discord.platform;

import com.xatkit.core.session.XatkitSession;
import com.xatkit.plugins.discord.DiscordUtils;
import com.xatkit.plugins.discord.platform.action.PostMessage;
import com.xatkit.plugins.discord.platform.action.Reply;
import com.xatkit.core.XatkitCore;
import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.plugins.chat.platform.ChatPlatform;
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
 * <li>{@link Reply}: reply to a user input</li>
 * <li>{@link PostMessage}: post a message to a given Discord
 * channel</li>
 * </ul>
 * <p>
 * This class is part of xatkit's core platforms, and can be used in an execution model by importing the
 * <i>DiscordPlatform</i> package.
 */
public class DiscordPlatform extends ChatPlatform {

    /**
     * The {@link JDA} client managing the Discord connection.
     * <p>
     * The {@link JDA} client is created from the Discord bot token stored in this class' {@link Configuration}
     * constructor parameter, and is used to authenticate the bot and post messages through the Discord API.
     *
     * @see #DiscordPlatform(XatkitCore, Configuration)
     * @see DiscordUtils
     */
    private JDA jdaClient;

    /**
     * Constructs a new {@link DiscordPlatform} from the provided {@link XatkitCore} and {@link Configuration}.
     * <p>
     * This constructor initializes the underlying {@link JDA} client with the Discord bot API token retrieved from
     * the {@link Configuration}.
     * <p>
     * <b>Note:</b> {@link DiscordPlatform} requires a valid Discord bot token to be initialized, and calling the
     * default constructor will throw an {@link IllegalArgumentException} when looking for the Discord bot token.
     *
     * @param xatkitCore    the {@link XatkitCore} instance associated to this runtimePlatform
     * @param configuration the {@link Configuration} used to retrieve the Discord bot token
     * @throws NullPointerException     if the provided {@code xatkitCore} or {@link Configuration} is {@code null}
     * @throws IllegalArgumentException if the provided Discord bot token is {@code null} or empty
     * @see DiscordUtils
     */
    public DiscordPlatform(XatkitCore xatkitCore, Configuration configuration) {
        super(xatkitCore, configuration);
        String discordToken = configuration.getString(DiscordUtils.DISCORD_TOKEN_KEY);
        checkArgument(nonNull(discordToken) && !discordToken.isEmpty(), "Cannot construct a DiscordPlatform from the " +
                "provided token %s, please ensure that the Xatkit configuration contains a valid Discord bot API " +
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
     * Returns the {@link XatkitSession} associated to the provided {@link MessageChannel}.
     *
     * @param messageChannel the {@link MessageChannel} to retrieve the {@link XatkitSession} from
     * @return the {@link XatkitSession} associated to the provided {@link MessageChannel}
     */
    public XatkitSession createSessionFromChannel(MessageChannel messageChannel) {
        return this.xatkitCore.getOrCreateXatkitSession(messageChannel.getId());
    }
}
