package edu.uoc.som.jarvis.plugins.log.platform.action;

import edu.uoc.som.jarvis.core.session.JarvisSession;
import fr.inria.atlanmod.commons.log.Level;
import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.platform.action.RuntimeAction;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.plugins.log.platform.LogPlatform;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * An abstract class representing logging actions.
 * <p>
 * This class relies on the slf4j logger bound with jarvis to print messages. The default implementation embeds a
 * <i>log4j2</i> instance, and a default configuration that can be extended using a custom <i>log4j2.xml</i>
 * configuration file.
 */
public abstract class LogAction extends RuntimeAction<LogPlatform> {

    /**
     * The message to log.
     */
    protected String message;

    /**
     * The severity {@link Level} of the {@link #message} to log.
     */
    protected Level logLevel;

    /**
     * Constructs a new {@link LogAction} with the provided {@code runtimePlatform}, {@code session}, {@code
     * message} and {@code LogLevel}.
     *
     * @param runtimePlatform the {@link LogPlatform} containing this action
     * @param session          the {@link JarvisSession} associated to this action
     * @param message          the message to log
     * @param logLevel         the severity {@link Level} of the message to log
     * @throws NullPointerException if the provided {@code runtimePlatform}, {@code session}, {@code message} or
     *                              {@code logLevel} is {@code null}
     */
    public LogAction(LogPlatform runtimePlatform, JarvisSession session, String message, Level logLevel) {
        super(runtimePlatform, session);
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
     * execution model variables.
     *
     * @return {@code null}
     */
    @Override
    public Object compute() {
        Log.log(this.logLevel, this.message);
        return null;
    }
}
