package com.xatkit.plugins.discord;

import com.xatkit.core.JarvisException;
import com.xatkit.core.session.RuntimeContexts;
import com.xatkit.plugins.chat.ChatUtils;
import com.xatkit.plugins.discord.platform.DiscordPlatform;
import com.xatkit.plugins.discord.platform.io.DiscordIntentProvider;
import fr.inria.atlanmod.commons.log.Log;
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
public class DiscordUtils implements ChatUtils {

    /**
     * The {@link org.apache.commons.configuration2.Configuration} key to store the Discord bot token.
     *
     * @see DiscordIntentProvider
     * @see DiscordPlatform
     */
    public static String DISCORD_TOKEN_KEY = "jarvis.discord.token";

    /**
     * The {@link RuntimeContexts} key used to store discord-related information.
     */
    public static String DISCORD_CONTEXT_KEY = "discord";

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
