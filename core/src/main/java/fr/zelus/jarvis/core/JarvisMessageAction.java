package fr.zelus.jarvis.core;

import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.utils.MessageUtils;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

/**
 * An abstract {@link JarvisAction} processing a message.
 * <p>
 * This class takes a {@link String} message as its constructor parameter, and relies on
 * {@link MessageUtils#fillContextValues(String, JarvisContext)} to pre-process it and replace context variable
 * accesses by their concrete value.
 * <p>
 * This class is only responsible of the pre-processing of the provided message, and does not provide any method to
 * print it to the end user.
 *
 * @param <T> the concrete {@link JarvisModule} subclass type containing the action
 * @see JarvisModule
 * @see MessageUtils
 */
public abstract class JarvisMessageAction<T extends JarvisModule> extends JarvisAction<T> {

    /**
     * The processed message.
     * <p>
     * This attribute is the result of calling {@link MessageUtils#fillContextValues(String, JarvisContext)} on the
     * {@code rawMessage} constructor parameter. Concrete subclasses can use this attribute to print the processed
     * message to the end user.
     *
     * @see MessageUtils#fillContextValues(String, JarvisContext)
     * @see #getMessage()
     */
    protected String message;

    /**
     * Constructs a new {@link JarvisMessageAction} with the provided {@containingModule}, {@code context}, and
     * {@code rawMessage}.
     * <p>
     * This constructor stores the result of calling {@link MessageUtils#fillContextValues(String, JarvisContext)} on
     * the {@code rawMessage} parameter. Concrete subclasses can use this attribute to print the processed message to
     * the end user.
     *
     * @param containingModule the {@link JarvisModule} containing this action
     * @param context          the {@link JarvisContext} associated to this action
     * @param rawMessage       the message to process
     * @throws NullPointerException     if the provided {@code containingModule} or {@code context} is {@code null}
     * @throws IllegalArgumentException if the provided {@code rawMessage} is {@code null} or empty
     */
    public JarvisMessageAction(T containingModule, JarvisContext context, String rawMessage) {
        super(containingModule, context);
        checkArgument(nonNull(rawMessage) && !rawMessage.isEmpty(), "Cannot construct a {0} action with the provided " +
                "message {1}, expected a non-null and not empty String", this.getClass().getSimpleName(), message);
        this.message = MessageUtils.fillContextValues(rawMessage, context);
    }

    /**
     * Returns the processed message.
     *
     * @return the processed message
     */
    protected String getMessage() {
        return message;
    }
}
