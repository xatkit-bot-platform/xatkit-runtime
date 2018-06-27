package fr.zelus.jarvis.core.module.log.action;

import fr.inria.atlanmod.commons.log.Level;
import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.module.log.LogModule;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * An abstract class representing logging actions.
 * <p>
 * This class relies on the slf4j logger bound with jarvis to print messages. The default implementation embeds a
 * <i>log4j2</i> instance, and a default configuration that can be extended using a custom <i>log4j2.xml</i>
 * configuration file.
 */
public abstract class LogAction extends JarvisAction<LogModule> {

    /**
     * The message to log.
     */
    protected String message;

    /**
     * The severity {@link Level} of the {@link #message} to log.
     */
    protected Level logLevel;

    /**
     * Constructs a new {@link LogAction} with the provided {@code message} and {@code logLevel}.
     *
     * @param containingModule the {@link LogModule} containing this action
     * @param message          the message to log
     * @param logLevel         the severity {@link Level} of the message to log
     * @throws NullPointerException if the provided {@code message} or {@code logLevel} is {@code null}
     */
    public LogAction(LogModule containingModule, String message, Level logLevel) {
        super(containingModule);
        checkNotNull(message, "Cannot construct a {0} action with null as its message", this.getClass().getSimpleName
                ());
        checkNotNull(logLevel, "Cannot construct a {0} action with null as its level", this.getClass().getSimpleName());
        this.message = message;
        this.logLevel = logLevel;
    }

    /**
     * Returns the {@link #message} to log.
     *
     * @return the {@link #message} to log
     */
    public final String getMessage() {
        return this.message;
    }

    @Override
    public void run() {
        Log.log(this.logLevel, this.message);
    }
}
