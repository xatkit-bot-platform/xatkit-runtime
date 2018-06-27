package fr.zelus.jarvis.core.module.log.action;

import fr.inria.atlanmod.commons.log.Level;
import fr.zelus.jarvis.core.module.log.LogModule;

/**
 * A {@link LogAction} that logs the provided message as an info.
 */
public class LogInfo extends LogAction {

    /**
     * Constructs a new {@link LogInfo} action from the provided {@code message}.
     *
     * @param containingModule the {@link LogModule} containing this action
     * @param message          the message to log
     * @throws NullPointerException if the provided {@code message} is {@code null}
     */
    public LogInfo(LogModule containingModule, String message) {
        super(containingModule, message, Level.INFO);
    }

}
