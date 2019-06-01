package edu.uoc.som.jarvis.plugins.react.platform.action;

import edu.uoc.som.jarvis.core.session.JarvisSession;
import edu.uoc.som.jarvis.core.session.RuntimeContexts;
import edu.uoc.som.jarvis.plugins.chat.ChatUtils;
import edu.uoc.som.jarvis.plugins.react.platform.ReactPlatform;
import edu.uoc.som.jarvis.plugins.react.platform.ReactUtils;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * A {@link edu.uoc.som.jarvis.core.platform.action.RuntimeMessageAction} that replies to a message using the input
 * jarvis-react channel.
 * <p>
 * This action relies on the provided {@link JarvisSession} to retrieve the jarvis-react {@code channel} associated
 * to the user input.
 *
 * @see PostMessage
 */
public class Reply extends PostMessage {

    /**
     * Returns the jarvis-react channel associated to the user input.
     * <p>
     * This method searches in the provided {@link RuntimeContexts} for the value stored with the key
     * {@link ReactUtils#REACT_CONTEXT_KEY}.{@link ReactUtils#CHAT_CHANNEL_CONTEXT_KEY}. Note that if the provided
     * {@link RuntimeContexts} does not contain the requested value a {@link NullPointerException} is thrown.
     *
     * @param context the {@link RuntimeContexts} to retrieve the jarvis-react channel from
     * @return the jarvis-react channel associated to the user input
     * @throws NullPointerException     if the provided {@code context} is {@code null}, or if it does not contain the
     *                                  channel information
     * @throws IllegalArgumentException if the retrieved channel is not a {@link String}
     */
    private static String getChannel(RuntimeContexts context) {
        checkNotNull(context, "Cannot retrieve the channel from the provided context %s", context);
        Object channelValue = context.getContextValue(ReactUtils.REACT_CONTEXT_KEY,
                ChatUtils.CHAT_CHANNEL_CONTEXT_KEY);
        checkNotNull(channelValue, "Cannot retrieve the React channel from the context, expected a non null " +
                ChatUtils.CHAT_CHANNEL_CONTEXT_KEY + " value, found %s", channelValue);
        checkArgument(channelValue instanceof String, "Invalid React channel type, expected %s, found %s",
                String.class.getSimpleName(), channelValue.getClass().getSimpleName());
        return (String) channelValue;
    }

    /**
     * Constructs a new {@link Reply} with the provided {@code reactPlatform}, {@code session}, and {@code message}.
     *
     * @param reactPlatform the {@link ReactPlatform} containing this action
     * @param session       the {@link JarvisSession} associated to this action
     * @param message       the message to post
     * @throws NullPointerException     if the provided {@code reactPlatform} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code message} is {@code null} or empty
     * @see #getChannel(RuntimeContexts)
     * @see PostMessage
     */
    public Reply(ReactPlatform reactPlatform, JarvisSession session, String message) {
        super(reactPlatform, session, message, getChannel(session.getRuntimeContexts()));
    }

}
