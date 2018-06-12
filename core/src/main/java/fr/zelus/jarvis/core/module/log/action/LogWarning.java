package fr.zelus.jarvis.core.module.log.action;

import fr.inria.atlanmod.commons.log.Level;

/**
 * A {@link LogAction} that logs the provided message as a warning.
 */
public class LogWarning extends LogAction {

    /**
     * Constructs a new {@link LogWarning} action from the provided {@code message}.
     *
     * @param message the message to log
     * @throws NullPointerException if the provided {@code message} is {@code null}
     */
    public LogWarning(String message) {
        super(message, Level.WARN);
    }
}
