package fr.zelus.jarvis.core.platform.action;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

/**
 * Stores the result of a computation and provides utility methods to manipulate it.
 * <p>
 * This class stores the raw result of a given computation, and can be initialized with additional information
 * representing the execution time, and the {@link Exception} thrown by the computation.
 * <p>
 * {@link RuntimeActionResult} provides an {@link #isError()} method that returns {@code true} if it contains an
 * {@link Exception}. This allows to quickly check whether the underlying computation succeeded or failed.
 */
public class RuntimeActionResult {

    /**
     * The raw result.
     * <p>
     * {@link RuntimeActionResult}'s {@code result} value can be set to {@code null} to represent computation results
     * that do not return any value.
     *
     * @see #getResult()
     */
    private Object result;

    /**
     * The execution time (in milliseconds).
     *
     * @see #getExecutionTime()
     */
    private long executionTime;

    /**
     * Represents an error result with an {@link Exception}.
     * <p>
     * Setting this value makes the {@link #isError()} method return {@code true}.
     *
     * @see #getThrownException()
     * @see #isError()
     */
    private Exception thrownException;

    /**
     * Constructs a new {@link RuntimeActionResult} from the provided {@code result} and {@code executionTime}.
     * <p>
     * This constructor does not set any {@code thrownException} value. As a result, calling the {@link #isError()}
     * method will return {@code false}.
     *
     * @param result        the raw result
     * @param executionTime the execution time (in milliseconds)
     * @throws IllegalArgumentException if the provided {@code executionTime < 0}
     */
    public RuntimeActionResult(Object result, long executionTime) {
        this(result, null, executionTime);
    }

    /**
     * Constructs a new {@link RuntimeActionResult} from the provided {@code partialResult}, {@code thrownException},
     * and {@code executionTime}.
     * <p>
     * This constructor sets the {@code thrownException} attribute, meaning that calling {@link #isError()} on this
     * object will return {@code true}. The {@code partialResult} can be used to represent partial information that
     * has been computed before the provided {@link Exception was thrown.
     * <p>
     * This constructor sets the {@code executionTime} attribute, that can be accessed through the
     * {@link #getExecutionTime()} method.
     *
     * @param partialResult   the partial raw result that has been computed before failing
     * @param thrownException the {@link Exception} that has been thrown during the computation
     * @param executionTime   the execution time (in milliseconds)
     * @throws IllegalArgumentException if the provided {@code executionTime < 0}
     */
    public RuntimeActionResult(Object partialResult, Exception thrownException, long executionTime) {
        checkArgument(executionTime >= 0, "Cannot construct a %s from the provided execution time: expected a " +
                "positive value (in ms), found %s", this.getClass().getSimpleName(), executionTime);
        this.result = partialResult;
        this.thrownException = thrownException;
        this.executionTime = executionTime;
    }

    /**
     * Returns the raw result stored in this {@link RuntimeActionResult}.
     *
     * @return the raw result stored in this {@link RuntimeActionResult}
     */
    public Object getResult() {
        return this.result;
    }

    /**
     * Returns the execution time value (in milliseconds) stored in this {@link RuntimeActionResult}.
     *
     * @return the execution time value stored in this {@link RuntimeActionResult}
     */
    public long getExecutionTime() {
        return this.executionTime;
    }

    /**
     * Return the {@link Exception} that has been thrown during the computation.
     * <p>
     * If this value is {@code null}, the {@link #isError()} method returns {@code false}, meaning that this
     * {@link RuntimeActionResult} does not represent an errored computation. If the {@link #isError()} method returns
     * {@code true} that {@link Exception} that has been thrown during the errored computation can be retrieved
     * through this method.
     *
     * @return the {@link Exception} that has been thrown during the computation
     * @see #isError() }
     */
    public Exception getThrownException() {
        return this.thrownException;
    }

    /**
     * Returns whether the {@link RuntimeActionResult} represents an errored computation.
     * <p>
     * If this method returns {@code true} the {@link Exception} that has been thrown during the errored computation
     * can be retrieved by calling {@link #getThrownException()}.
     *
     * @return {@code true} if the {@link RuntimeActionResult} represents an errored computation, {@code false} otherwise
     * @see #getThrownException()
     */
    public boolean isError() {
        return nonNull(this.thrownException);
    }
}
