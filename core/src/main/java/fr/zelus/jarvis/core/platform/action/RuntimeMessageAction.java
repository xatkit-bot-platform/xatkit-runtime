package fr.zelus.jarvis.core.platform.action;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.ExecutionService;
import fr.zelus.jarvis.core.JarvisException;
import fr.zelus.jarvis.core.platform.RuntimePlatform;
import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.io.RuntimeEventProvider;

import java.io.IOException;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

/**
 * An abstract {@link RuntimeAction} processing a message.
 * <p>
 * This class takes a {@link String} message as its constructor parameter, and relies on
 * {@link JarvisContext#(String)} to pre-process it and replace context variable
 * accesses by their concrete value.
 * <p>
 * This class is only responsible of the pre-processing of the provided message, and does not provide any method to
 * print it to the end user.
 *
 * @param <T> the concrete {@link RuntimePlatform} subclass type containing the action
 * @see RuntimePlatform
 * @see JarvisContext
 */
public abstract class RuntimeMessageAction<T extends RuntimePlatform> extends RuntimeAction<T> {

    /**
     * The number of times the {@link RuntimeMessageAction} tries to send the message if an {@link IOException} occurred.
     */
    private static int IO_ERROR_RETRIES = 3;

    /**
     * The delay (in ms) to wait before attempting to resend the message.
     * <p>
     * If an {@link IOException} occurred while sending the message the {@link #compute()} method will attempt to
     * resend it {@link #IO_ERROR_RETRIES} times. Each attempt will first wait for {@code RETRY_WAIT_TIME *
     * <number_of_attempts>} before trying to resend the message.
     */
    private static int RETRY_WAIT_TIME = 500;

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
     * Constructs a new {@link RuntimeMessageAction} with the provided {@code runtimePlatform}, {@code session}, and
     * {@code rawMessage}.
     * <p>
     * This constructor stores the result of calling {@link JarvisContext#fillContextValues(String)} on
     * the {@code rawMessage} parameter. Concreted subclasses can use this attribute to print the processed message to
     * the end user.
     *
     * @param runtimePlatform the {@link RuntimePlatform} containing this action
     * @param session          the {@link JarvisSession} associated to this action
     * @param rawMessage       the message to process
     * @throws NullPointerException     if the provided {@code runtimePlatform} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code rawMessage} is {@code null} or empty
     * @see JarvisSession
     * @see JarvisContext
     */
    public RuntimeMessageAction(T runtimePlatform, JarvisSession session, String rawMessage) {
        super(runtimePlatform, session);
        checkArgument(nonNull(rawMessage) && !rawMessage.isEmpty(), "Cannot construct a %s action with the provided " +
                "message %s, expected a non-null and not empty String", this.getClass().getSimpleName(), message);
        this.message = session.getJarvisContext().fillContextValues(rawMessage);
    }

    /**
     * Retrieve the {@link JarvisSession} associated to the client of the message and merges it with the current one.
     * <p>
     * This method relies on {@link #getClientSession()} to retrieve the {@link JarvisSession} associated to the
     * client of the message, and merges the current {@code session} with the client one if they are different.
     * This allows to pass client-independent context variables (e.g. from {@link RuntimeEventProvider}s)
     * to new client sessions.
     *
     * @throws JarvisException if the provided {@code session} is different from the {@link #getClientSession()} and
     *                         the merge operation between the contexts failed
     */
    @Override
    public void init() {
        JarvisSession clientSession = getClientSession();
        if (!clientSession.equals(session)) {
            Log.info("Merging {0} session to the client one", this.getClass().getSimpleName());
            try {
                clientSession.getJarvisContext().merge(session.getJarvisContext());
            } catch (JarvisException e) {
                throw new JarvisException("Cannot construct the action {0}, the action session cannot be merged in " +
                        "the client one", e);
            }
        }
    }

    /**
     * Runs the {@link RuntimeMessageAction} and returns its result wrapped in a {@link RuntimeActionResult}.
     * <p>
     * This method handles {@link IOException}s by trying to send again the message after waiting {@code
     * <number_of_retries>*500} ms, in case the issue is related to network stability. The default number of retries
     * is {@code 3}. If the message cannot be sent after {@code 3} retries the thrown {@link IOException} is wrapped
     * in the returned {@link RuntimeActionResult} and handled as a regular exception.
     * <p>
     * The returned {@link RuntimeActionResult#getExecutionTime()} value includes all the attempts to send the message.
     * <p>
     * This method should not be called manually, and is handled by the {@link ExecutionService} component that
     * manages and executes {@link RuntimeAction}s.
     * <p>
     * This method does not throw any {@link Exception} if the underlying {@link RuntimeAction}'s computation does not
     * complete. Exceptions thrown during the {@link RuntimeMessageAction}'s computation can be accessed through the
     * {@link RuntimeActionResult#getThrownException()} method.
     *
     * @return the {@link RuntimeActionResult} containing the raw result of the computation and monitoring information
     * @see ExecutionService
     * @see RuntimeActionResult
     */
    @Override
    public RuntimeActionResult call() {
        Object computationResult = null;
        Exception thrownException;
        int attempts = 0;
        long before = System.currentTimeMillis();


        /*
         * We use a do-while here because the thrownException value is initialized with null, and we want to perform
         * at least one iteration of the loop. If the thrownException value is still null after an iteration we can
         * exit the loop: the underlying action computation finished without any exception.
         */
        do {
            /*
             * Reset the thrownException, if we are retrying to send a message the previously stored exception is not
             * required anymore: we can forget it and replace it with the potential new exception.
             */
            thrownException = null;
            attempts++;
            if (attempts > 1) {
                /*
                 * If this is not the first attempt we need to wait before sending again the message. The waiting
                 * time is equal to (iteration - 1) * RETRY_TIME: the second iteration will wait for RETRY_TIME, the
                 * third one for 2 * RETRY_TIME, etc.
                 */
                int waitTime = (attempts - 1) * RETRY_WAIT_TIME;
                Log.info("Waiting {0} ms before trying to send the message again", waitTime);
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e1) {
                    /*
                     * Ignore the exception, the Thread has been interrupted but we can still compute the action.
                     */
                    Log.warn("An error occurred while waiting to send the message, trying to send it right now", e1);
                }
            }
            try {
                computationResult = compute();
            } catch (IOException e) {
                if(attempts < IO_ERROR_RETRIES + 1) {
                    Log.error("An {0} occurred when computing the action, trying to send the message again ({1}/{2})", e
                            .getClass().getSimpleName(), attempts, IO_ERROR_RETRIES);
                } else {
                    Log.error("Could not compute the action: {0}", e.getClass().getSimpleName());
                }
                /*
                 * Set the thrownException value, if the compute() method fails with an IOException every time we
                 * need to return an error message with it.
                 */
                thrownException = e;
            } catch (Exception e) {
                thrownException = e;
                /*
                 * We caught a non-IO exception: an internal error occurred when computing the action. We assume that
                 * internal errors cannot be solved be recomputing the action, so we break and return the
                 * RuntimeActionResult directly.
                 */
                break;
            }
            /*
             * Exit on IO_ERROR_RETRIES + 1: the first one is the standard execution, then we can retry
             * IO_ERROR_RETRIES times.
             */
        } while (nonNull(thrownException) && attempts < IO_ERROR_RETRIES + 1);
        long after = System.currentTimeMillis();
        return new RuntimeActionResult(computationResult, thrownException, (after - before));
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
     * This method is used by the {@link RuntimeMessageAction} constructor to pass client-independent context
     * variables (e.g. from {@link RuntimeEventProvider}s) to the client session.
     *
     * @return the {@link JarvisSession} associated to the client of the message to send
     */
    protected abstract JarvisSession getClientSession();
}
