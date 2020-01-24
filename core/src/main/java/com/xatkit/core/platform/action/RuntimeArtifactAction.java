package com.xatkit.core.platform.action;

import com.xatkit.core.ExecutionService;
import com.xatkit.core.XatkitException;
import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.io.RuntimeEventProvider;
import com.xatkit.core.session.RuntimeContexts;
import com.xatkit.core.session.XatkitSession;
import fr.inria.atlanmod.commons.log.Log;

import java.io.IOException;

import static java.util.Objects.nonNull;

/**
 * An abstract {@link RuntimeAction}.
 * <p>
 * This class relies on {@link RuntimeContexts#fillContextValues(String)} to replace context
 * variable accesses by their concrete value.
 * <p>
 *
 * @param <T> the concrete {@link RuntimePlatform} subclass type containing the action
 * @see RuntimePlatform
 * @see RuntimeContexts
 */
public abstract class RuntimeArtifactAction<T extends RuntimePlatform> extends RuntimeAction<T> {

    /**
     * The {@link org.apache.commons.configuration2.Configuration} key used to specify the delay (in milliseconds)
     * before sending a message.
     * <p>
     * This property is set by default to {@code 0}, meaning that the bot sends message directly when they are ready.
     *
     * @see #DEFAULT_MESSAGE_DELAY
     */
    public static String MESSAGE_DELAY_KEY = "xatkit.message.delay";

    /**
     * The default value of the {@link #MESSAGE_DELAY_KEY} configuration key ({@code 0}).
     *
     * @see #MESSAGE_DELAY_KEY
     */
    public static int DEFAULT_MESSAGE_DELAY = 0;

    /**
     * The number of times the {@link RuntimeArtifactAction} tries to send the artifact if an {@link IOException}
     * occurred.
     */
    private static int IO_ERROR_RETRIES = 3;

    /**
     * The delay (in ms) to wait before attempting to resend the artifact.
     * <p>
     * If an {@link IOException} occurred while sending the artifact the {@link #compute()} method will attempt to
     * resend it {@link #IO_ERROR_RETRIES} times. Each attempt will first wait for {@code RETRY_WAIT_TIME *
     * <number_of_attempts>} before trying to resend the artifact.
     */
    private static int RETRY_WAIT_TIME = 500;

    /**
     * The message delay to apply for this specific {@link RuntimeArtifactAction}.
     * <p>
     * This value is retrieved from the {@link RuntimePlatform}'s
     * {@link org.apache.commons.configuration2.Configuration}, and can be configured with the
     * {@link #MESSAGE_DELAY_KEY} configuration key.
     *
     * @see #MESSAGE_DELAY_KEY
     * @see #DEFAULT_MESSAGE_DELAY
     */
    private int messageDelay;

    /**
     * Constructs a new {@link RuntimeArtifactAction} with the provided {@code runtimePlatform} and {@code session}.
     * <p>
     *
     * @param runtimePlatform the {@link RuntimePlatform} containing this action
     * @param session         the {@link XatkitSession} associated to this action
     * @throws NullPointerException if the provided {@code runtimePlatform} or {@code session} is {@code null}
     * @see XatkitSession
     * @see RuntimeContexts
     */
    public RuntimeArtifactAction(T runtimePlatform, XatkitSession session) {
        super(runtimePlatform, session);
        this.messageDelay = this.runtimePlatform.getConfiguration().getInt(MESSAGE_DELAY_KEY, DEFAULT_MESSAGE_DELAY);
        Log.debug("{0} message delay: {1}", this.getClass().getSimpleName(), messageDelay);
    }

    /**
     * Retrieve the {@link XatkitSession} associated to the client of the artifact and merges it with the current one.
     * <p>
     * This method relies on {@link #getClientSession()} to retrieve the {@link XatkitSession} associated to the
     * client of the artifact, and merges the current {@code session} with the client one if they are different.
     * This allows to pass client-independent context variables (e.g. from {@link RuntimeEventProvider}s)
     * to new client sessions.
     *
     * @throws XatkitException if the provided {@code session} is different from the {@link #getClientSession()} and
     *                         the merge operation between the contexts failed
     */
    @Override
    public void init() {
        XatkitSession clientSession = getClientSession();
        if (!clientSession.equals(session)) {
            Log.info("Merging {0} session to the client one", this.getClass().getSimpleName());
            try {
                clientSession.getRuntimeContexts().merge(session.getRuntimeContexts());
                clientSession.merge(session);
                /*
                 * The new merge strategy doesn't replace the clientSession.sessionVariables reference with the
                 * provided session.sessionVariables. We need to update the current session to make sure the action
                 * will be computed with the clientSession.
                 */
                session = clientSession;
            } catch (XatkitException e) {
                throw new XatkitException("Cannot construct the action {0}, the action session cannot be merged in " +
                        "the client one", e);
            }
        }
    }

    /**
     * Runs the {@link RuntimeArtifactAction} and returns its result wrapped in a {@link RuntimeActionResult}.
     * <p>
     * This method handles {@link IOException}s by trying to send again the artifact after waiting {@code
     * <number_of_retries>*500} ms, in case the issue is related to network stability. The default number of retries
     * is {@code 3}. If the artifact cannot be sent after {@code 3} retries the thrown {@link IOException} is wrapped
     * in the returned {@link RuntimeActionResult} and handled as a regular exception.
     * <p>
     * The returned {@link RuntimeActionResult#getExecutionTime()} value includes all the attempts to send the artifact.
     * <p>
     * This method should not be called manually, and is handled by the {@link ExecutionService} component that
     * manages and executes {@link RuntimeAction}s.
     * <p>
     * This method does not throw any {@link Exception} if the underlying {@link RuntimeAction}'s computation does not
     * complete. Exceptions thrown during the {@link RuntimeArtifactAction}'s computation can be accessed through the
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
             * Reset the thrownException, if we are retrying to send a artifact the previously stored exception is not
             * required anymore: we can forget it and replace it with the potential new exception.
             */
            thrownException = null;
            attempts++;
            if (attempts > 1) {
                /*
                 * If this is not the first attempt we need to wait before sending again the artifact. The waiting
                 * time is equal to (iteration - 1) * RETRY_TIME: the second iteration will wait for RETRY_TIME, the
                 * third one for 2 * RETRY_TIME, etc.
                 */
                int waitTime = (attempts - 1) * RETRY_WAIT_TIME;
                Log.info("Waiting {0} ms before trying to send the artifact again", waitTime);
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e1) {
                    /*
                     * Ignore the exception, the Thread has been interrupted but we can still compute the action.
                     */
                    Log.warn("An error occurred while waiting to send the artifact, trying to send it right now", e1);
                }
            }
            try {
                waitMessageDelay();
                computationResult = compute();
            } catch (IOException e) {
                if (attempts < IO_ERROR_RETRIES + 1) {
                    Log.error("An {0} occurred when computing the action, trying to send the artifact again ({1}/{2})"
                            , e
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

    private void waitMessageDelay() {
        if (this.messageDelay > 0) {
            try {
                Thread.sleep(messageDelay);
            } catch (InterruptedException e) {
                Log.error("An error occurred when waiting for the message delay, see attached exception", e);
            }
        }
    }

    /**
     * Returns the {@link XatkitSession} associated to the client of the artifact to send.
     * <p>
     * This method is used by the {@link RuntimeArtifactAction} constructor to pass client-independent context
     * variables (e.g. from {@link RuntimeEventProvider}s) to the client session.
     *
     * @return the {@link XatkitSession} associated to the client of the artifact to send
     */
    protected abstract XatkitSession getClientSession();
}
