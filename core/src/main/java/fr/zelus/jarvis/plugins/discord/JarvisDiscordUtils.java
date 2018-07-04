package fr.zelus.jarvis.plugins.discord;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisException;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

import javax.security.auth.login.LoginException;

import static java.util.Objects.nonNull;

/**
 * An utility class that holds Discord-related helpers.
 * <p>
 * This class defines the jarvis configuration key to store the Discord bot token, as well as an utility method
 * to retrieve an existing instance of the {@link JDA} client, in order to avoid multiple client instantiations.
 */
public class JarvisDiscordUtils {

    /**
     * The {@link org.apache.commons.configuration2.Configuration} key to store the Discord bot token.
     *
     * @see fr.zelus.jarvis.plugins.discord.io.DiscordInputProvider
     * @see fr.zelus.jarvis.plugins.discord.module.DiscordModule
     */
    public static String DISCORD_TOKEN_KEY = "jarvis.discord.token";

    /**
     * The {@link fr.zelus.jarvis.core.session.JarvisContext} key used to store discord-related information.
     */
    public static String DISCORD_CONTEXT_KEY = "discord";

    /**
     * The {@link fr.zelus.jarvis.core.session.JarvisContext} variable key used to store discord channel information.
     */
    public static String DISCORD_CHANNEL_CONTEXT_KEY = "channel";

    /**
     * The {@link fr.zelus.jarvis.core.session.JarvisContext} variable key used to store discord username information.
     */
    public static String DISCORD_USERNAME_CONTEXT_KEY = "username";

    /**
     * The reusable {@link JDA} client.
     * <p>
     * This field ensures that there is only one {@link JDA} instance, that can be accessed using
     * {@link #getJDA(String)}. This avoid multiple client instantiations.
     */
    private static JDA jda;

    /**
     * Returns the {@link JDA} client associated to the provided {@code token}.
     * <p>
     * This method returns the stored {@link #jda} client if its token corresponds to the provided one. If not, this
     * method creates a new {@link JDA} instance and overrides the existing one.
     *
     * @param token the Discord bot token to retrieve the {@link JDA} client for
     * @return the {@link JDA} client associated to the provided {@code token}
     */
    public static JDA getJDA(String token) {
        if (nonNull(jda)) {
            if (jda.getToken().contains(token)) {
                /*
                 * Use contains instead of equals, the getToken() method returns a string starting by the client
                 * type, followed by the token.
                 */
                return jda;
            } else {
                Log.warn("Creating a new JDA instance, the previous one will be deleted");
                jda.shutdown();
            }
        }
        try {
            jda = new JDABuilder(AccountType.BOT).setToken(token).buildBlocking();
        } catch (LoginException e) {
            String errorMessage = "Cannot connect to the Discord API, check that the provided token is valid";
            Log.error(errorMessage);
            throw new JarvisException(errorMessage, e);
        } catch (InterruptedException e) {
            String errorMessage = "An error occurred when starting the Discord client, see attached exception";
            Log.error(e, errorMessage);
            throw new JarvisException(errorMessage, e);
        }
        return jda;
    }

}
