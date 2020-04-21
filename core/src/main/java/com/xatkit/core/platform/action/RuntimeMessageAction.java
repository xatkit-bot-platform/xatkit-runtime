package com.xatkit.core.platform.action;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.session.RuntimeContexts;
import com.xatkit.core.session.XatkitSession;
import lombok.Getter;
import lombok.NonNull;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;

/**
 * An abstract {@link RuntimeAction} processing a message.
 * <p>
 *
 * @param <T> the concrete {@link RuntimePlatform} subclass type containing the action
 * @see RuntimePlatform
 * @see RuntimeContexts
 */
public abstract class RuntimeMessageAction<T extends RuntimePlatform> extends RuntimeArtifactAction<T> {

    /**
     * The processed message.
     *
     * @see #getMessage()
     */
    @Getter
    protected String message;

    /**
     * Constructs a new {@link RuntimeMessageAction} with the provided {@code runtimePlatform}, {@code session}, and
     * {@code rawMessage}.
     * <p>
     * This constructor stores the result of calling {@link RuntimeContexts#fillContextValues(String)} on
     * the {@code rawMessage} parameter. Concreted subclasses can use this attribute to print the processed message to
     * the end user.
     *
     * @param runtimePlatform the {@link RuntimePlatform} containing this action
     * @param session         the {@link XatkitSession} associated to this action
     * @param rawMessage      the message to process
     * @throws NullPointerException     if the provided {@code runtimePlatform}, {@code session}, or {@code
     * rawMessage} is {@code null}
     * @throws IllegalArgumentException if the provided {@code rawMessage} is {@code null} or empty
     * @see XatkitSession
     * @see RuntimeContexts
     */
    public RuntimeMessageAction(@NonNull T runtimePlatform, @NonNull XatkitSession session,
                                @NonNull String rawMessage) {
        super(runtimePlatform, session);
        checkArgument(!rawMessage.isEmpty(), "Cannot construct a %s action with the provided " +
                "message %s, expected a non-null and not empty String", this.getClass().getSimpleName(), message);
        this.message = rawMessage;
    }
}
