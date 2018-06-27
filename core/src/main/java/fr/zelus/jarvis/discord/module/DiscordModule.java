package fr.zelus.jarvis.discord.module;

import fr.zelus.jarvis.core.JarvisModule;
import fr.zelus.jarvis.discord.JarvisDiscordUtils;
import net.dv8tion.jda.core.JDA;
import org.apache.commons.configuration2.Configuration;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static fr.zelus.jarvis.discord.JarvisDiscordUtils.DISCORD_TOKEN_KEY;
import static java.util.Objects.nonNull;

public class DiscordModule extends JarvisModule {

    public static JDA jda;

    public DiscordModule(Configuration configuration) {
        super(configuration);
        checkNotNull(configuration, "Cannot construct a DiscordModule from a null configuration");
        String discordToken = configuration.getString(DISCORD_TOKEN_KEY);
        checkArgument(nonNull(discordToken) && !discordToken.isEmpty(), "Cannot construct a DiscordModule from the " +
                "provided token %s, please ensure that the jarvis configuration contains a valid Discord bot API " +
                "token associated to the key %s", discordToken, DISCORD_TOKEN_KEY);
        jda = JarvisDiscordUtils.getJDA(discordToken);
    }
}
