package fr.zelus.jarvis.plugins.log.module.action;

import fr.inria.atlanmod.commons.log.Level;
import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.plugins.log.module.LogModule;
import fr.zelus.jarvis.core.session.JarvisContext;

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
     * Constructs a new {@link LogAction} with the provided {@code containingModule}, {@code context}, {@code
     * message} and {@code LogLevel}.
     *
     * @param containingModule the {@link LogModule} containing this action
     * @param context          the {@link JarvisContext} associated to this action
     * @param message          the message to log
     * @param logLevel         the severity {@link Level} of the message to log
     * @throws NullPointerException if the provided {@code containingModule}, {@code context}, {@code message} or
     *                              {@code logLevel} is {@code null}
     */
    public LogAction(LogModule containingModule, JarvisContext context, String message, Level logLevel) {
        super(containingModule, context);
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

    /**
     * Logs the provided {@code message} with the given {@code logLevel}.
     * <p>
     * <b>Note</b>: this method always returns {@code null}, and it's result should not be stored in
     * orchestration model variables.
     *
     * @return
     */
    @Override
    public Object call() {
        Log.log(this.logLevel, this.message);
        return null;
    }
}
