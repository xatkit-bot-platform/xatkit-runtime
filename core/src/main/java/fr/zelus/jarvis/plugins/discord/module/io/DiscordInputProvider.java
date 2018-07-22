package fr.zelus.jarvis.plugins.discord.module.io;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.io.IntentProvider;
import fr.zelus.jarvis.plugins.discord.JarvisDiscordUtils;
import net.dv8tion.jda.core.JDA;
import org.apache.commons.configuration2.Configuration;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static fr.zelus.jarvis.plugins.discord.JarvisDiscordUtils.DISCORD_TOKEN_KEY;
import static java.util.Objects.nonNull;

/**
 * A Discord user {@link IntentProvider}.
 * <p>
 * This class relies on the <a href="https://github.com/DV8FromTheWorld/JDA">JDA library</a> to receive direct
 * messages and react to them. Note that this input provider only captures direct messages (sent in private channels)
 * sent to the Discord bot associated to this class.
 * <p>
 * Instances of this class must be configured with a {@link Configuration} instance holding the Discord bot API token
 * in the property {@link JarvisDiscordUtils#DISCORD_TOKEN_KEY}. This token is used to authenticate the bot and
 * receive messages through the JDA client.
 *
 * @see JarvisDiscordUtils
 * @see IntentProvider
 */
public class DiscordInputProvider extends IntentProvider {

    /**
     * The {@link String} representing the Discord bot API token.
     * <p>
     * This token is used to authenticate the bot and receive messages through the {@link #jdaClient}.
     */
    private String discordToken;

    /**
     * The {@link JDA} client managing the Discord connection.
     */
    private JDA jdaClient;

    /**
     * Constructs a new {@link DiscordInputProvider} from the provided {@link JarvisCore} and {@link Configuration}.
     * <p>
     * This constructor initializes the underlying {@link JDA} client and creates a message listener that forwards to
     * the {@code jarvisCore} instance not empty direct messages sent by users (not bots) to the bot private channel
     * (see {@link PrivateMessageListener}.
     * <p>
     * <b>Note:</b> {@link DiscordInputProvider} requires a valid Discord bot API token to be initialized, and
     * calling the default constructor will throw an {@link IllegalArgumentException} when looking for the Discord
     * bot token.
     *
     * @param jarvisCore    the {@link JarvisCore} instance used to handle messages
     * @param configuration the {@link Configuration} used to retrieve the Discord bot token
     * @throws NullPointerException     if the provided {@link Configuration} is {@code null}
     * @throws IllegalArgumentException if the provided Discord bot token is {@code null} or empty
     * @see JarvisDiscordUtils
     * @see PrivateMessageListener
     */
    public DiscordInputProvider(JarvisCore jarvisCore, Configuration configuration) {
        super(jarvisCore, configuration);
        checkNotNull(configuration, "Cannot construct a DiscordInputProvider from a null configuration");
        this.discordToken = configuration.getString(DISCORD_TOKEN_KEY);
        checkArgument(nonNull(discordToken) && !discordToken.isEmpty(), "Cannot construct a DiscordInputProvider " +
                "from the provided token %s, please ensure that the jarvis configuration contains a valid Discord bot" +
                "API token associated to the key %s", discordToken, DISCORD_TOKEN_KEY);
        jdaClient = JarvisDiscordUtils.getJDA(discordToken);
        Log.info("Starting to listen jarvis Discord direct messages");
        jdaClient.addEventListener(new PrivateMessageListener(jarvisCore, this));
    }

    /**
     * Returns the {@link JDA} client associated to this class.
     * <p>
     * <b>Note:</b> this method is protected for testing purposes, and should not be called by client code.
     *
     * @return the {@link JDA} client associated to this class
     */
    protected JDA getJdaClient() {
        return jdaClient;
    }

    @Override
    public void run() {
        /*
         * Required because the JDA listener is started in another thread, and if this thread terminates the main
         * application terminates.
         */
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {

            }
        }
    }

    /**
     * Disconnects the underlying {@link JDA} client.
     */
    @Override
    public void close() {
        Log.info("Closing Discord Client");
        this.jdaClient.removeEventListener(this.jdaClient.getRegisteredListeners().toArray());
        this.jdaClient.shutdownNow();
    }
}
