package edu.uoc.som.jarvis.plugins.log.platform.action;

import edu.uoc.som.jarvis.core.session.JarvisSession;
import fr.inria.atlanmod.commons.log.Level;
import edu.uoc.som.jarvis.core.session.JarvisSession;
import edu.uoc.som.jarvis.plugins.log.platform.LogPlatform;

/**
 * A {@link LogAction} that logs the provided message as an info.
 */
public class LogInfo extends LogAction {

    /**
     * Constructs a new {@link LogInfo} action from the provided {@code runtimePlatform}, {@code session}, and {@code
     * message}.
     *
     * @param runtimePlatform the {@link LogPlatform} containing this action
     * @param session          the {@link JarvisSession} associated to this action
     * @param message          the message to log
     * @throws NullPointerException if the provided {@code runtimePlatform}, {@code session}, or {@code message} is
     *                              {@code null}
     */
    public LogInfo(LogPlatform runtimePlatform, JarvisSession session, String message) {
        super(runtimePlatform, session, message, Level.INFO);
    }

}
