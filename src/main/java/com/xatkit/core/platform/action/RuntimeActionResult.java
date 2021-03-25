package com.xatkit.core.platform.action;

import lombok.Getter;

import javax.annotation.Nullable;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

/**
 * Stores the result of a {@link RuntimeAction} computation and provides utility methods to manipulate it.
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
    @Getter
    private Object result;

    /**
     * The execution time (in milliseconds).
     *
     * @see #getExecutionTime()
     */
    @Getter
    private long executionTime;

    /**
     * Represents an error result with an {@link Exception}.
     * <p>
     * Setting this value makes the {@link #isError()} method return {@code true}.
     *
     * @see #getThrowable()
     * @see #isError()
     */
    @Getter
    private Throwable throwable;

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
     * has been computed before the provided {@link Exception} was thrown.
     * <p>
     * This constructor sets the {@code executionTime} attribute, that can be accessed through the
     * {@link #getExecutionTime()} method.
     *
     * @param partialResult the partial raw result that has been computed before failing
     * @param throwable     the {@link Throwable} that has been thrown during the computation
     * @param executionTime the execution time (in milliseconds)
     * @throws IllegalArgumentException if the provided {@code executionTime < 0}
     */
    public RuntimeActionResult(@Nullable Object partialResult, @Nullable Throwable throwable, long executionTime) {
        checkArgument(executionTime >= 0, "Cannot construct a %s from the provided execution time: expected a "
                + "positive value (in ms), found %s", this.getClass().getSimpleName(), executionTime);
        this.result = partialResult;
        this.throwable = throwable;
        this.executionTime = executionTime;
    }

    /**
     * Returns whether the {@link RuntimeActionResult} represents an errored computation.
     * <p>
     * If this method returns {@code true} the {@link Exception} that has been thrown during the errored computation
     * can be retrieved by calling {@link #getThrowable()}.
     *
     * @return {@code true} if the {@link RuntimeActionResult} represents an errored computation, {@code false}
     * otherwise
     * @see #getThrowable()
     */
    public boolean isError() {
        return nonNull(this.throwable);
    }
}
