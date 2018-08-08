package fr.zelus.jarvis.core;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.core.session.JarvisSession;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

/**
 * An abstract {@link JarvisAction} processing a message.
 * <p>
 * This class takes a {@link String} message as its constructor parameter, and relies on
 * {@link JarvisContext#(String)} to pre-process it and replace context variable
 * accesses by their concrete value.
 * <p>
 * This class is only responsible of the pre-processing of the provided message, and does not provide any method to
 * print it to the end user.
 *
 * @param <T> the concrete {@link JarvisModule} subclass type containing the action
 * @see JarvisModule
 * @see JarvisContext
 */
public abstract class JarvisMessageAction<T extends JarvisModule> extends JarvisAction<T> {

    /**
     * The processed message.
     * <p>
     * This attribute is the result of calling {@link JarvisContext#fillContextValues(String)} on the
     * {@code rawMessage} constructor parameter. Concrete subclasses can use this attribute to print the processed
     * message to the end user.
     *
     * @see JarvisContext#fillContextValues(String)
     * @see #getMessage()
     */
    protected String message;

    /**
     * Constructs a new {@link JarvisMessageAction} with the provided {@code containingModule}, {@code session}, and
     * {@code rawMessage}.
     * <p>
     * This constructor stores the result of calling {@link JarvisContext#fillContextValues(String)} on
     * the {@code rawMessage} parameter. Concreted subclasses can use this attribute to print the processed message to
     * the end user.
     *
     * @param containingModule the {@link JarvisModule} containing this action
     * @param session          the {@link JarvisSession} associated to this action
     * @param rawMessage       the message to process
     * @throws NullPointerException     if the provided {@code containingModule} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code rawMessage} is {@code null} or empty
     * @see JarvisSession
     * @see JarvisContext
     */
    public JarvisMessageAction(T containingModule, JarvisSession session, String rawMessage) {
        super(containingModule, session);
        checkArgument(nonNull(rawMessage) && !rawMessage.isEmpty(), "Cannot construct a {0} action with the provided " +
                "message {1}, expected a non-null and not empty String", this.getClass().getSimpleName(), message);
        this.message = session.getJarvisContext().fillContextValues(rawMessage);
    }

    /**
     * Retrieve the {@link JarvisSession} associated to the client of the message and merges it with the current one.
     * <p>
     * This method relies on {@link #getClientSession()} to retrieve the {@link JarvisSession} associated to the
     * client of the message, and merges the current {@code session} with the client one if they are different.
     * This allows to pass client-independent context variables (e.g. from {@link fr.zelus.jarvis.io.EventProvider}s)
     * to new client sessions.
     *
     * @throws JarvisException if the provided {@code session} is different from the {@link #getClientSession()} and
     *                         the merge operation between the contexts failed
     */
    @Override
    public void init() {
        JarvisSession clientSession = getClientSession();
        if (!clientSession.equals(session)) {
            Log.info("Merging %s session to the client one", this.getClass().getSimpleName());
            try {
                clientSession.getJarvisContext().merge(session.getJarvisContext());
            } catch (JarvisException e) {
                throw new JarvisException("Cannot construct the action {0}, the action session cannot be merged in " +
                        "the client one", e);
            }
        }
    }

    /**
     * Returns the processed message.
     *
     * @return the processed message
     */
    protected String getMessage() {
        return message;
    }

    /**
     * Returns the {@link JarvisSession} associated to the client of the message to send.
     * <p>
     * This method is used by the {@link JarvisMessageAction} constructor to pass client-independent context
     * variables (e.g. from {@link fr.zelus.jarvis.io.EventProvider}s) to the client session.
     *
     * @return the {@link JarvisSession} associated to the client of the message to send
     */
    protected abstract JarvisSession getClientSession();
}
