package fr.zelus.jarvis.slack.module.action;

import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.response.chat.ChatPostMessageResponse;
import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.JarvisException;
import fr.zelus.jarvis.core.JarvisMessageAction;
import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.slack.module.SlackModule;

import java.io.IOException;
import java.text.MessageFormat;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

/**
 * A {@link JarvisAction} that posts a {@code message} to a given Slack {@code channel}.
 * <p>
 * This class relies on the {@link SlackModule}'s {@link com.github.seratch.jslack.Slack} client and Slack bot API token
 * to connect to the Slack API and post messages.
 * <p>
 * <b>Note:</b> this class requires that its containing {@link SlackModule} has been loaded with a valid Slack bot API
 * token in order to authenticate the bot and post messages.
 */
public class PostMessage extends JarvisMessageAction<SlackModule> {

    /**
     * The Slack channel to post the message to.
     */
    private String channel;

    /**
     * Constructs a new {@link PostMessage} with the provided {@code message} and {@code channel}.
     *
     * @param containingModule the {@link SlackModule} containing this action
     * @param context          the {@link JarvisContext} associated to this action
     * @param message          the message to post
     * @param channel          the Slack channel to post the message to
     * @throws IllegalArgumentException if the provided {@code message} or {@code channel} is {@code null} or empty.
     */
    public PostMessage(SlackModule containingModule, JarvisContext context, String message, String channel) {
        super(containingModule, context, message);

        checkArgument(nonNull(channel) && !channel.isEmpty(), "Cannot construct a {0} action with the provided " +
                "channel {1}, expected a non-null and not empty String", this.getClass().getSimpleName(), channel);
        this.channel = channel;
    }



    /**
     * Posts the provided {@code message} to the given {@code channel}.
     * <p>
     * This method relies on the containing {@link SlackModule}'s Slack bot API token to authenticate the bot and
     * post the {@code message} to the given {@code channel}.
     *
     * @throws IOException       if an error occurs when connecting to the Slack API
     * @throws SlackApiException if the provided token does not authenticate the bot
     */
    @Override
    public Object call() {
        ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                .token(module.getSlackToken())
                .channel(channel)
                .text(message)
                .build();
        try {
            ChatPostMessageResponse response = module.getSlack().methods().chatPostMessage(request);
            if (response.isOk()) {
                Log.trace("Message {0} successfully sent to the Slack API", request);
            } else {
                Log.error("Cannot send the message {0} to the Slack API, received response {1}", request, response);
            }
        } catch (IOException | SlackApiException e) {
            String errorMessage = MessageFormat.format("Cannot send the message {0} to the Slack API", request);
            Log.error(errorMessage);
            throw new JarvisException(errorMessage, e);
        }
        return null;
    }
}
