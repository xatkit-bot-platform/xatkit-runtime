package edu.uoc.som.jarvis.core.interpreter.operation;

/**
 * Jarvis interpreter top-level exception.
 * <p>
 * This exception is used to wrap all the {@link Exception}s thrown by the interpreter's internals and return them in
 * an unified way.
 */
public class OperationException extends RuntimeException {

    /**
     * Constructs a new {@link OperationException}.
     *
     * @see RuntimeException#RuntimeException()
     */
    public OperationException() {
        super();
    }

    /**
     * Constructs a new {@link OperationException} from the provided {@code message}.
     *
     * @param message the exception's message
     * @see RuntimeException#RuntimeException(String)
     */
    public OperationException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@link OperationException} from the provided {@code message} and {@code cause}.
     *
     * @param message the exception's message
     * @param cause   the exception's cause
     * @see RuntimeException#RuntimeException(String, Throwable)
     */
    public OperationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@link OperationException} from the provided {@code cause}.
     *
     * @param cause the exception's cause
     * @see RuntimeException#RuntimeException(Throwable)
     */
    public OperationException(Throwable cause) {
        super(cause);
    }

    /**
     * Construcs a new {@link OperationException} from the provided {@code message}, {@code cause}, {@code
     * enableSuppression}, and {@code writableStackTrace}.
     *
     * @param message            the exception's message
     * @param cause              the exception's cause
     * @param enableSuppression  whether or not suppression is enable
     * @param writableStackTrace whether or not the stack trace should be writable
     * @see RuntimeException#RuntimeException(String, Throwable, boolean, boolean)
     */
    protected OperationException(String message, Throwable cause, boolean enableSuppression,
                                 boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
