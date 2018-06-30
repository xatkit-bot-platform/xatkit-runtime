package fr.zelus.jarvis.discord.io;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisCore;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link ListenerAdapter} that listen private messages and send them to the underlying {@link JarvisCore}.
 * <p>
 * This class is instantiated by its containing {@link DiscordInputProvider}, and is used to react to Discord
 * messages. Note that this listener only forwards not empty, not {@code null} messages to the {@link JarvisCore}, in
 * order to avoid useless calls to the underlying DialogFlow API.
 */
public class PrivateMessageListener extends ListenerAdapter {

    /**
     * The {@link JarvisCore} instance used to handle received messages.
     */
    private JarvisCore jarvisCore;

    /**
     * Constructs a new {@link PrivateMessageListener} from the provided {@code jarvisCore}.
     *
     * @param jarvisCore the {@link JarvisCore} instance used to handled received messages
     * @throws NullPointerException if the provided {@code jarvisCore} is {@code null}
     */
    public PrivateMessageListener(JarvisCore jarvisCore) {
        super();
        checkNotNull(jarvisCore, "Cannot construct a {0} from a null {1}", this.getClass().getSimpleName(),
                JarvisCore.class.getSimpleName());
        this.jarvisCore = jarvisCore;
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
}
