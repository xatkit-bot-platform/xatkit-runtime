package fr.zelus.jarvis.discord.module.action;

import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.discord.module.DiscordModule;
import net.dv8tion.jda.core.entities.MessageChannel;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

/**
 * A {@link JarvisAction} that posts a {@code message} to a given Discord {@code channel}.
 * <p>
 * This class relies on the {@link DiscordModule}'s {@link net.dv8tion.jda.core.JDA} client to connect to the Discord
 * API and post messages.
 * <p>
 * <b>Note:</b> this class requires that its containing {@link DiscordModule} has been loaded with a valid Discord
 * bot token in order to authenticate the bot and post messages.
 */
public class PostMessage extends JarvisAction<DiscordModule> {

    /**
     * The message to post.
     */
    private String message;

    /**
     * The Discord channel to post the message to.
     */
    private String channel;

    /**
     * Constructs a new {@link PostMessage} with the provided {@code message} and {@code channel}.
     *
     * @param containingModule the {@link DiscordModule} containing this action
     * @param context          the {@link JarvisContext} associated to this action
     * @param message          the message to post
     * @param channel          the Discord channel to post the message to
     * @throws IllegalArgumentException if the provided {@code message} or {@code channel} is {@code null} or empty
     */
    public PostMessage(DiscordModule containingModule, JarvisContext context, String message, String channel) {
        super(containingModule, context);
        checkArgument(nonNull(message) && !message.isEmpty(), "Cannot construct a {0} action with the provided " +
                "message {1}, expected a non-null and not empty String", this.getClass().getSimpleName(), message);
        checkArgument(nonNull(channel) && !channel.isEmpty(), "Cannot construct a {0} action with the provided " +
                "channel {1}, expected a non-null and not empty String", this.getClass().getSimpleName(), channel);
        this.message = message;
        this.channel = channel;
    }

    /**
     * Posts the provided {@code message} to the given {@code channel}.
     * <p>
     * This method relies on the containing {@link DiscordModule}'s Discord {@link net.dv8tion.jda.core.JDA} client
     * to authenticate the bot and post the {@code message} to the given {@code channel}.
     */
    @Override
    public void run() {
        MessageChannel messageChannel = module.getJdaClient().getPrivateChannelById(channel);
        messageChannel.sendMessage(message).queue();
    }
}
