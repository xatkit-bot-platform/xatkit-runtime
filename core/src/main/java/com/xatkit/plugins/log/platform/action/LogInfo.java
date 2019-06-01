package com.xatkit.plugins.log.platform.action;

import com.xatkit.core.session.XatkitSession;
import fr.inria.atlanmod.commons.log.Level;
import com.xatkit.plugins.log.platform.LogPlatform;

/**
 * A {@link LogAction} that logs the provided message as an info.
 */
public class LogInfo extends LogAction {

    /**
     * Constructs a new {@link LogInfo} action from the provided {@code runtimePlatform}, {@code session}, and {@code
     * message}.
     *
     * @param runtimePlatform the {@link LogPlatform} containing this action
     * @param session          the {@link XatkitSession} associated to this action
     * @param message          the message to log
     * @throws NullPointerException if the provided {@code runtimePlatform}, {@code session}, or {@code message} is
     *                              {@code null}
     */
    public LogInfo(LogPlatform runtimePlatform, XatkitSession session, String message) {
        super(runtimePlatform, session, message, Level.INFO);
    }

}
