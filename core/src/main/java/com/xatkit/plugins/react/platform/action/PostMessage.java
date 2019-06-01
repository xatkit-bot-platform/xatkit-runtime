package com.xatkit.plugins.react.platform.action;

import com.xatkit.core.platform.action.RuntimeMessageAction;
import com.xatkit.core.session.JarvisSession;
import com.xatkit.plugins.react.platform.ReactPlatform;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

/**
 * A {@link RuntimeMessageAction} that posts a {@code message} to a given jarvis-react {@code channel}.
 */
public class PostMessage extends RuntimeMessageAction<ReactPlatform> {

    /**
     * The channel to post the message to.
     */
    private String channel;

    /**
     * Constructs a new {@link PostMessage} with the provided {@code runtimePlatform}, {@code session}, {@code
     * message}, and {@code channel}.
     *
     * @param runtimePlatform the {@link ReactPlatform} containing this action
     * @param session         the {@link JarvisSession} associated to this action
     * @param message         the message to post
     * @param channel         the jarvis-react channel to post the message to
     * @throws NullPointerException     if the provided {@code runtimePlatform} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code message} or {@code channel} is {@code null}
     */
    public PostMessage(ReactPlatform runtimePlatform, JarvisSession session, String message, String channel) {
        super(runtimePlatform, session, message);
        checkArgument(nonNull(channel) && !(channel.isEmpty()), "Cannot construct a %s action with the provided " +
                "channel %s, expected a non-null and not empty String", this.getClass().getSimpleName(), channel);
        this.channel = channel;
    }

    /**
     * Posts the provided {@code message} to the given {@code channel}.
     * <p>
     * Posted messages are available through the {@code /react/getAnswer} REST endpoint, and are not pushed directly
     * to the client application.
     *
     * @return {@code null}
     */
    @Override
    protected Object compute() {
        this.runtimePlatform.storeMessage(channel, message);
        return null;
    }

    @Override
    protected JarvisSession getClientSession() {
        return this.runtimePlatform.createSessionFromChannel(channel);
    }
}
