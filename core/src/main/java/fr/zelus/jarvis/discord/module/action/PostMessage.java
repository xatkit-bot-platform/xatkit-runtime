package fr.zelus.jarvis.discord.module.action;

import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.discord.module.DiscordModule;
import net.dv8tion.jda.core.entities.MessageChannel;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

public class PostMessage extends JarvisAction {

    private String message;

    private String channel;

    public PostMessage(String message, String channel) {
        super();
        checkArgument(nonNull(message) && !message.isEmpty(), "Cannot construct a {0} action with the provided " +
                "message {1}, expected a non-null and not empty String", this.getClass().getSimpleName(), message);
        checkArgument(nonNull(channel) && !channel.isEmpty(), "Cannot construct a {0} action with the provided " +
                "channel {1}, expected a non-null and not empty String", this.getClass().getSimpleName(), channel);
        this.message = message;
        this.channel = channel;
    }

    @Override
    public void run() {
        MessageChannel messageChannel = DiscordModule.jda.getPrivateChannelById(channel);
        messageChannel.sendMessage(message).queue();
    }
}
