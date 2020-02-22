package com.xatkit.core.server;

import org.apache.http.HttpStatus;

/**
 * An exception thrown by {@link RestHandler}s to notify the {@link XatkitServer}.
 * <p>
 * This exception is used to set the HTTP response's status code in the {@link XatkitServer}: by default the server
 * returns a status code {@code 200}, but it will return a {@code 404} if the {@link RestHandler} threw a
 * {@link RestHandlerException}.
 */
public class RestHandlerException extends Exception {

    /**
     * The HTTP error code associated to the exception.
     * <p>
     * This error code defaults to {@link HttpStatus#SC_NOT_FOUND} when not provided ({@code 404}).
     */
    private int errorCode;

    /**
     * Constructs a {@link RestHandlerException}.
     * <p>
     * The {@code errorCode} of the created exception is set to {@link HttpStatus#SC_NOT_FOUND}.
     */
    public RestHandlerException() {
        this(HttpStatus.SC_NOT_FOUND, "", null);
    }

    /**
     * Constructs a {@link RestHandlerException} with the provided HTTP {@code errorCode} and {@code message}.
     *
     * @param errorCode the HTTP error code
     * @param message   the exception's message
     */
    public RestHandlerException(int errorCode, String message) {
        this(errorCode, message, null);
    }

    /**
     * Constructs a {@link RestHandlerException} from the provided {@code message}.
     * <p>
     * The {@code errorCode} of the created exception is set to {@link HttpStatus#SC_NOT_FOUND}.
     *
     * @param message the exception's message
     */
    public RestHandlerException(String message) {
        this(HttpStatus.SC_NOT_FOUND, message, null);
    }

    /**
     * Constructs a {@link RestHandlerException} from the provided {@code message} and {@code cause}.
     * <p>
     * The {@code errorCode} of the created exception is set to {@link HttpStatus#SC_NOT_FOUND}.
     *
     * @param message the exception's message
     * @param cause   the exception's cause
     */
    public RestHandlerException(String message, Throwable cause) {
        this(HttpStatus.SC_NOT_FOUND, message, cause);
    }

    /**
     * Constructs a {@link RestHandlerException} from the provided HTTP {@code errorCode}, {@code message}, and {@code
     * cause}.
     *
     * @param errorCode errorCode the HTTP error code
     * @param message   the exception's message
     * @param cause     the exception's cause
     */
    public RestHandlerException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a {@link RestHandlerException} from the provided {@code cause}.
     * <p>
     * The {@code errorCode} of the created exception is set to {@link HttpStatus#SC_NOT_FOUND}.
     *
     * @param cause the exception's cause
     */
    public RestHandlerException(Throwable cause) {
        this(HttpStatus.SC_NOT_FOUND, "", cause);
    }

    /**
     * Constructs a {@link RestHandlerException} from the provided HTTP {@code errorCode} and {@code cause}.
     *
     * @param errorCode the HTTP error code
     * @param cause     the exception's cause
     */
    public RestHandlerException(int errorCode, Throwable cause) {
        this(HttpStatus.SC_NOT_FOUND, "", cause);
    }

    /**
     * Returns the HTTP error code associated to this exception.
     *
     * @return the HTTP error code associated to this exception
     */
    public int getErrorCode() {
        return this.errorCode;
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
