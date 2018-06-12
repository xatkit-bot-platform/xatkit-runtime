package fr.zelus.jarvis.core.module.log.action;

import fr.inria.atlanmod.commons.log.Level;

/**
 * A {@link LogAction} that logs the provided message as an info.
 */
public class LogInfo extends LogAction {

    /**
     * Constructs a new {@link LogInfo} action from the provided {@code message}.
     *
     * @param message the message to log
     * @throws NullPointerException if the provided {@code message} is {@code null}
     */
    public LogInfo(String message) {
        super(message, Level.INFO);
    }

}
