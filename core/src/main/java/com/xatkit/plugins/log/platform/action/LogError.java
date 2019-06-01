package com.xatkit.plugins.log.platform.action;

import com.xatkit.core.session.JarvisSession;
import fr.inria.atlanmod.commons.log.Level;
import com.xatkit.plugins.log.platform.LogPlatform;

/**
 * A {@link LogAction} that logs the provided message as an error.
 */
public class LogError extends LogAction {

    /**
     * Constructs a new {@link LogError} action from the provided {@code runtimePlatform}, {@code session}, and {@code
     * message}.
     *
     * @param runtimePlatform the {@link LogPlatform} containing this action
     * @param session          the {@link JarvisSession} associated to this action
     * @param message          the message to log
     * @throws NullPointerException if the provided {@code runtimePlatform}, {@code session}, or {@code message} is
     *                              {@code null}
     */
    public LogError(LogPlatform runtimePlatform, JarvisSession session, String message) {
        super(runtimePlatform, session, message, Level.ERROR);
    }
}
