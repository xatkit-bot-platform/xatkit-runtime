package com.xatkit.core.server;

/**
 * An exception thrown by {@link RestHandler}s to notify the {@link XatkitServer}.
 * <p>
 * This exception is used to set the HTTP response's status code in the {@link XatkitServer}: by default the server
 * returns a status code {@code 200}, but it will return a {@code 404} if the {@link RestHandler} threw a
 * {@link RestHandlerException}.
 */
public class RestHandlerException extends Exception {

    /**
     * Constructs a {@link RestHandlerException}.
     */
    public RestHandlerException() {
        super();
    }

    /**
     * Constructs a {@link RestHandlerException} from the provided {@code message}
     *
     * @param message the exception's message
     */
    public RestHandlerException(String message) {
        super(message);
    }

    /**
     * Constructs a {@link RestHandlerException} from the provided {@code message} and {@code cause}.
     *
     * @param message the exception's message
     * @param cause   the exception's cause
     */
    public RestHandlerException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a {@link RestHandlerException} from the provided {@code cause}.
     *
     * @param cause the exception's cause
     */
    public RestHandlerException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a {@link RestHandlerException} from the provided {@code message}, {@code cause}, {@code
     * enableSuppression}, and {@code writeableStackTrace}.
     *
     * @param message             the exception's message
     * @param cause               the exception's cause
     * @param enableSuppression   whether or not suppression is enabled
     * @param writeableStackTrace whether or not the stack trace should be writable
     */
    protected RestHandlerException(String message, Throwable cause, boolean enableSuppression,
                                   boolean writeableStackTrace) {
        super(message, cause, enableSuppression, writeableStackTrace);
    }

}
