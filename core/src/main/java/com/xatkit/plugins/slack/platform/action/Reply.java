package com.xatkit.plugins.slack.platform.action;

import com.xatkit.core.session.XatkitSession;
import com.xatkit.plugins.slack.SlackUtils;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.session.RuntimeContexts;
import com.xatkit.plugins.slack.platform.SlackPlatform;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * A {@link RuntimeAction} that replies to a message using the input Slack channel.
 * <p>
 * This action relies on the provided {@link XatkitSession} to retrieve the Slack {@code channel} associated to the
 * user input.
 * <p>
 * This class relies on the {@link SlackPlatform}'s {@link com.github.seratch.jslack.Slack} client and Slack bot API
 * token to connect to the Slack API and post the reply message.
 * <p>
 * <b>Note:</b> this class requires that its containing {@link SlackPlatform} has been loaded with a valid Slack bot API
 * token in order to authenticate the bot and post messages.
 *
 * @see PostMessage
 */
public class Reply extends PostMessage {

    /**
     * Returns the Slack channel associated to the user input.
     * <p>
     * This method searches in the provided {@link RuntimeContexts} for the value stored with the key
     * {@link SlackUtils#SLACK_CONTEXT_KEY}.{@link SlackUtils#CHAT_CHANNEL_CONTEXT_KEY}. Note that if
     * the provided {@link RuntimeContexts} does not contain the requested value a {@link NullPointerException} is thrown.
     *
     * @param context the {@link RuntimeContexts} to retrieve the Slack channel from
     * @return the Slack channel associated to the user input
     * @throws NullPointerException     if the provided {@code context} is {@code null}, or if it does not contain the
     *                                  channel information
     * @throws IllegalArgumentException if the retrieved channel is not a {@link String}
     * @see SlackUtils
     */
    public static String getChannel(RuntimeContexts context) {
        checkNotNull(context, "Cannot retrieve the channel from the provided %s %s", RuntimeContexts.class
                .getSimpleName(), context);
        Object channelValue = context.getContextValue(SlackUtils.SLACK_CONTEXT_KEY, SlackUtils
                .CHAT_CHANNEL_CONTEXT_KEY);
        checkNotNull(channelValue, "Cannot retrieve the Slack channel from the context");
        checkArgument(channelValue instanceof String, "Invalid Slack channel type, expected %s, found %s", String
                .class.getSimpleName(), channelValue.getClass().getSimpleName());
        return (String) channelValue;
    }

    /**
     * Constructs a new {@link Reply} with the provided {@code runtimePlatform}, {@code session}, and {@code message}.
     *
     * @param runtimePlatform the {@link SlackPlatform} containing this action
     * @param session          the {@link XatkitSession} associated to this action
     * @param message          the message to post
     * @throws NullPointerException     if the provided {@code runtimePlatform} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code message} is {@code null} or empty
     * @see #getChannel(RuntimeContexts)
     * @see PostMessage#PostMessage(SlackPlatform, XatkitSession, String, String)
     */
    public Reply(SlackPlatform runtimePlatform, XatkitSession session, String message) {
        super(runtimePlatform, session, message, getChannel(session.getRuntimeContexts()));
    }
}
