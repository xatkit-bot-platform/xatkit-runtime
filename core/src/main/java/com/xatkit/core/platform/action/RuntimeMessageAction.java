package com.xatkit.core.platform.action;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.core.session.RuntimeContexts;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

/**
 * An abstract {@link RuntimeAction} processing a message.
 * <p>
 * This class takes a {@link String} message as its constructor parameter, and relies on
 * {@link RuntimeContexts#fillContextValues(String)} to pre-process it and replace context variable accesses by their
 * concrete value.
 * <p>
 * This class is only responsible of the pre-processing of the provided message, and does not provide any method to
 * print it to the end user.
 *
 * @param <T> the concrete {@link RuntimePlatform} subclass type containing the action
 * @see RuntimePlatform
 * @see RuntimeContexts
 */
public abstract class RuntimeMessageAction<T extends RuntimePlatform> extends RuntimeArtifactAction<T> {

    /**
     * The processed message.
     * <p>
     * This attribute is the result of calling {@link RuntimeContexts#fillContextValues(String)} on the
     * {@code rawMessage} constructor parameter. Concrete subclasses can use this attribute to print the processed
     * message to the end user.
     *
     * @see RuntimeContexts#fillContextValues(String)
     * @see #getMessage()
     */
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
     * @param session          the {@link XatkitSession} associated to this action
     * @param rawMessage       the message to process
     * @throws NullPointerException     if the provided {@code runtimePlatform} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code rawMessage} is {@code null} or empty
     * @see XatkitSession
     * @see RuntimeContexts
     */
    public RuntimeMessageAction(T runtimePlatform, XatkitSession session, String rawMessage) {
        super(runtimePlatform, session);
        checkArgument(nonNull(rawMessage) && !rawMessage.isEmpty(), "Cannot construct a %s action with the provided " +
                "message %s, expected a non-null and not empty String", this.getClass().getSimpleName(), message);
        this.message = rawMessage;
    }

    /**
     * Returns the processed message.
     *
     * @return the processed message
     */
    protected String getMessage() {
        return message;
    }
}
