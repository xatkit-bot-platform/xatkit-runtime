package fr.zelus.jarvis.discord;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisException;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

import javax.security.auth.login.LoginException;

import static java.util.Objects.nonNull;

public class JarvisDiscordUtils {

    public static String DISCORD_TOKEN_KEY = "jarvis.discord.token";

    private static JDA jda;

    public static JDA getJDA(String token) {
        if(nonNull(jda)) {
            if (jda.getToken().contains(token)) {
                return jda;
            } else {
                Log.warn("Creating a new JDA instance, the previous one will be deleted");
                jda.shutdown();
            }
        }
        try {
            jda = new JDABuilder(AccountType.BOT).setToken(token).buildBlocking();
        } catch(LoginException e) {
            String errorMessage = "Cannot connect to the Discord API, check that the provided token is valid";
            Log.error(errorMessage);
            throw new JarvisException(errorMessage, e);
        } catch(InterruptedException e) {
            String errorMessage = "An error occurred when starting the Discord client, see attached exception";
            Log.error(e, errorMessage);
            throw new JarvisException(errorMessage, e);
        }
        return jda;
    }

}
