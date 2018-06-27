package fr.zelus.jarvis.discord.io;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.discord.JarvisDiscordUtils;
import fr.zelus.jarvis.io.InputProvider;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.commons.configuration2.Configuration;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static fr.zelus.jarvis.discord.JarvisDiscordUtils.DISCORD_TOKEN_KEY;
import static java.util.Objects.nonNull;


public class DiscordInputProvider extends InputProvider {


    private String discordToken;

    private JDA jdaClient;

    public DiscordInputProvider(JarvisCore jarvisCore, Configuration configuration) {
        super(jarvisCore, configuration);
        checkNotNull(configuration, "Cannot construct a DiscordInputProvider from a null configuration");
        this.discordToken = configuration.getString(DISCORD_TOKEN_KEY);
        checkArgument(nonNull(discordToken) && !discordToken.isEmpty(), "Cannot construct a DiscordInputProvider " +
                "from the provided token %s, please ensure that the jarvis configuration contains a valid Discord bot" +
                "API token associated to the key %s", discordToken, DISCORD_TOKEN_KEY);
        jdaClient = JarvisDiscordUtils.getJDA(discordToken);
        Log.info("Starting to listen jarvis Discord direct messages");
        jdaClient.addEventListener(new ListenerAdapter() {

            @Override
            public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
                User author = event.getAuthor();
                if (author.isBot()) {
                    return;
                }
                MessageChannel channel = event.getChannel();
                String channelName = channel.getName();
                Message message = event.getMessage();
                String content = message.getContentRaw();
                if (content.isEmpty()) {
                    Log.trace("Skipping {0}, the message is empty");
                    return;
                }
                Log.info("Received message {0}", content);
                jarvisCore.handleMessage(content);
            }
        });
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

    @Override
    public void close() {
        Log.info("Closing Discord Client");
        this.jdaClient.shutdownNow();
    }
}
