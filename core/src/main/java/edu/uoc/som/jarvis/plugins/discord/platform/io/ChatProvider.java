package edu.uoc.som.jarvis.plugins.discord.platform.io;

import edu.uoc.som.jarvis.plugins.discord.platform.DiscordPlatform;
import org.apache.commons.configuration2.Configuration;

/**
 * A generic Discord user {@link edu.uoc.som.jarvis.core.platform.io.RuntimeIntentProvider}.
 * <p>
 * This class wraps the {@link DiscordIntentProvider} and allows to use it as a generic <i>ChatProvider</i> from the
 * <i>ChatPlatform</i>.
 *
 * @see DiscordIntentProvider
 */
public class ChatProvider extends DiscordIntentProvider {

    /**
     * Constructs a new {@link ChatProvider} from the provided {@code runtimePlatform} and {@code configuration}.
     *
     * @param runtimePlatform the {@link DiscordPlatform} containing this {@link ChatProvider}
     * @param configuration   the {@link Configuration} used to retrieve the Discord bot token
     * @throws NullPointerException     if the provided {@code runtimePlatform} or {@code configuration} is {@code null}
     * @throws IllegalArgumentException if the provided Discord bot token is {@code null} or empty
     * @see DiscordIntentProvider
     */
    public ChatProvider(DiscordPlatform runtimePlatform, Configuration configuration) {
        super(runtimePlatform, configuration);
    }
}
