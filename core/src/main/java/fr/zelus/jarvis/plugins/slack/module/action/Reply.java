package fr.zelus.jarvis.plugins.slack.module.action;

import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.plugins.slack.JarvisSlackUtils;
import fr.zelus.jarvis.plugins.slack.module.SlackModule;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * A {@link fr.zelus.jarvis.core.JarvisAction} that replies to a message using the input Slack channel.
 * <p>
 * This action relies on the provided {@link JarvisSession} to retrieve the Slack {@code channel} associated to the
 * user input.
 * <p>
 * This class relies on the {@link SlackModule}'s {@link com.github.seratch.jslack.Slack} client and Slack bot API
 * token to connect to the Slack API and post the reply message.
 * <p>
 * <b>Note:</b> this class requires that its containing {@link SlackModule} has been loaded with a valid Slack bot API
 * token in order to authenticate the bot and post messages.
 */
public class Reply extends PostMessage {

    /**
     * Returns the Slack channel associated to the user input.
     * <p>
     * This method searches in the provided {@link JarvisContext} for the value stored with the key
     * {@link JarvisSlackUtils#SLACK_CONTEXT_KEY}.{@link JarvisSlackUtils#SLACK_CHANNEL_CONTEXT_KEY}. Note that if
     * the provided {@link JarvisContext} does not contain the requested value a {@link NullPointerException} is thrown.
     *
     * @param context the {@link JarvisContext} to retrieve the Slack channel from
     * @return the Slack channel associated to the user input
     * @throws NullPointerException     if the provided {@code context} is {@code null}, or if it does not contain the
     *                                  channel information
     * @throws IllegalArgumentException if the retrieved channel is not a {@link String}
     * @see JarvisSlackUtils
     */
    private static String getChannel(JarvisContext context) {
        checkNotNull(context, "Cannot retrieve the channel from the provided {0}: null", JarvisContext.class
                .getSimpleName());
        Object channelValue = context.getContextValue(JarvisSlackUtils.SLACK_CONTEXT_KEY, JarvisSlackUtils
                .SLACK_CHANNEL_CONTEXT_KEY);
        checkNotNull(channelValue, "Cannot retrieve the Slack channel from the context");
        checkArgument(channelValue instanceof String, "Invalid Slack channel type, expected %s, found %s", String
                .class.getSimpleName(), channelValue.getClass().getSimpleName());
        return (String) channelValue;
    }

    /**
     * Constructs a new {@link Reply} with the provided {@code containingModule}, {@code session}, and {@code message}.
     *
     * @param containingModule the {@link SlackModule} containing this action
     * @param session          the {@link JarvisSession} associated to this action
     * @param message          the message to post
     * @throws NullPointerException     if the provided {@code containingModule} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code message} is {@code null} or empty
     * @see #getChannel(JarvisContext)
     */
    public Reply(SlackModule containingModule, JarvisSession session, String message) {
        super(containingModule, session, message, getChannel(session.getJarvisContext()));
    }
}
