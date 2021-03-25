package com.xatkit.core.platform.action;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.execution.StateContext;
import lombok.Getter;
import lombok.NonNull;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;

/**
 * An abstract {@link RuntimeAction} processing a message.
 * <p>
 *
 * @param <T> the concrete {@link RuntimePlatform} subclass type containing the action
 * @see RuntimePlatform
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
     * Constructs a new {@link RuntimeMessageAction} with the provided {@code platform}, {@code context}, and
     * {@code rawMessage}.
     * <p>
     *
     * @param platform   the {@link RuntimePlatform} containing this action
     * @param context    the {@link StateContext} associated to this action
     * @param rawMessage the message to process
     * @throws NullPointerException     if the provided {@code platform}, {@code context}, or {@code
     *                                  rawMessage} is {@code null}
     * @throws IllegalArgumentException if the provided {@code rawMessage} is {@code null} or empty
     * @see StateContext
     */
    public RuntimeMessageAction(@NonNull T platform, @NonNull StateContext context, @NonNull String rawMessage) {
        super(platform, context);
        checkArgument(!rawMessage.isEmpty(), "Cannot construct a %s action with the provided message %s, expected a "
                + "non-null and not empty String", this.getClass().getSimpleName(), message);
        this.message = rawMessage;
    }
}
