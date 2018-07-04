package fr.zelus.jarvis.plugins.log.module.action;

import fr.inria.atlanmod.commons.log.Level;
import fr.zelus.jarvis.plugins.log.module.LogModule;
import fr.zelus.jarvis.core.session.JarvisContext;

/**
 * A {@link LogAction} that logs the provided message as an error.
 */
public class LogError extends LogAction {

    /**
     * Constructs a new {@link LogError} action from the provided {@code message}.
     *
     * @param containingModule the {@link LogModule} containing this action
     * @param context          the {@link JarvisContext} associated to this action
     * @param message          the message to log
     * @throws NullPointerException if the provided {@code message} is {@code null}
     */
    public LogError(LogModule containingModule, JarvisContext context, String message) {
        super(containingModule, context, message, Level.ERROR);
    }
}
