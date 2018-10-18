package fr.zelus.jarvis.plugins.discord.module.io;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.intent.RecognizedIntent;
import fr.zelus.jarvis.plugins.discord.JarvisDiscordUtils;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link ListenerAdapter} that listen private messages and send them to the underlying {@link JarvisCore}.
 * <p>
 * This class is instantiated by its containing {@link DiscordIntentProvider}, and is used to react to Discord
 * messages. Note that this listener only forwards not empty, not {@code null} messages to the {@link JarvisCore}, in
 * order to avoid useless calls to the underlying DialogFlow API.
 */
public class PrivateMessageListener extends ListenerAdapter {

    /**
     * The {@link JarvisCore} instance used to handle received messages.
     */
    private JarvisCore jarvisCore;

    /**
     * The {@link DiscordIntentProvider} managing this listener.
     */
    private DiscordIntentProvider discordIntentProvider;

    /**
     * Constructs a new {@link PrivateMessageListener} from the provided {@code jarvisCore} and {@code
     * discordIntentProvider}.
     *
     * @param jarvisCore           the {@link JarvisCore} instance used to handled received messages
     * @param discordIntentProvider the {@link DiscordIntentProvider} managing this listener
     * @throws NullPointerException if the provided {@code jarvisCore} is {@code null}
     */
    public PrivateMessageListener(JarvisCore jarvisCore, DiscordIntentProvider discordIntentProvider) {
        super();
        checkNotNull(jarvisCore, "Cannot construct a %s from a null %s", this.getClass().getSimpleName(),
                JarvisCore.class.getSimpleName());
        checkNotNull(discordIntentProvider, "Cannot construct a %s from a null %s", this.getClass().getSimpleName(),
                DiscordIntentProvider.class.getSimpleName());
        this.jarvisCore = jarvisCore;
        this.discordIntentProvider = discordIntentProvider;
    }

    /**
     * Returns the {@link JarvisCore} instance used to handled received messages.
     * <p>
     * <b>Note:</b> this method is protected for testing purposes, and should not be called by client code.
     *
     * @return the {@link JarvisCore} instance used to handle received messages
     */
    protected JarvisCore getJarvisCore() {
        return this.jarvisCore;
    }

    /**
     * Handles the received private message event and forward it to the {@link JarvisCore}.
     * <p>
     * Empty and {@code null} messages are filtered in order to avoid useless calls to the underlying DialogFlow API.
     * In addition, messages send by other bots are also ignored to avoid infinite discussion loops.
     *
     * @param event the {@link PrivateMessageReceivedEvent} wrapping the received message
     * @throws NullPointerException if the provided event is {@code null}
     */
    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        checkNotNull(event, "Cannot handle a null %s", PrivateMessageReceivedEvent.class.getSimpleName());
        User author = event.getAuthor();
        checkNotNull(author, "Cannot handle a message from a null author");
        if (author.isBot()) {
            return;
        }
        MessageChannel channel = event.getChannel();
        String channelId = channel.getId();
        Message message = event.getMessage();
        String content = message.getContentRaw();
        if (content.isEmpty()) {
            Log.trace("Skipping {0}, the message is empty");
            return;
        }
        Log.info("Received message {0} from user {1} (id: {2})", content, author.getName(), author.getId());
        JarvisSession jarvisSession = discordIntentProvider.getModule().createSessionFromChannel(channel);
        /*
         * Call getRecognizedIntent before setting any context variable, the recognition triggers a decrement of all
         * the context variables.
         */
        RecognizedIntent recognizedIntent = discordIntentProvider.getRecognizedIntent(content, jarvisSession);
        /*
         * The discord-related values are stored in the local context with a lifespan count of 1: they are reset
         * every time a message is received, and may cause consistency issues when using multiple IntentProviders.
         */
        jarvisSession.getJarvisContext().setContextValue(JarvisDiscordUtils.DISCORD_CONTEXT_KEY, 1, JarvisDiscordUtils
                .DISCORD_CHANNEL_CONTEXT_KEY, channel.getId());
        jarvisSession.getJarvisContext().setContextValue(JarvisDiscordUtils.DISCORD_CONTEXT_KEY, 1, JarvisDiscordUtils
                .DISCORD_USERNAME_CONTEXT_KEY, author.getName());
        jarvisCore.handleEvent(recognizedIntent, jarvisSession);
    }
}
