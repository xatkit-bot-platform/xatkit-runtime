package fr.zelus.jarvis.core;

/**
 * Jarvis top-level exception.
 * <p>
 * This exception is used to wrap internal {@link Exception}s and return them in a unified way to client applications.
 */
public class JarvisException extends RuntimeException {

    /**
     * Constructs a new {@link JarvisException}.
     *
     * @see RuntimeException#RuntimeException()
     */
    public JarvisException() {
        super();
    }

    /**
     * Constructs a new {@link JarvisException} from the provided {@code message}.
     *
     * @param message the exception's message
     * @see RuntimeException#RuntimeException(String)
     */
    public JarvisException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@link JarvisException} from provided {@code message} and {@code cause}.
     *
     * @param message the exception's message
     * @param cause   the exception's cause
     * @see RuntimeException#RuntimeException(String, Throwable)
     */
    public JarvisException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@link JarvisException} from the provided {@code cause}.
     *
     * @param cause the exception's cause
     * @see RuntimeException#RuntimeException(Throwable)
     */
    public JarvisException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@link JarvisException} from the provided {@code message}, {@code cause}, {@code
     * enableSuppression}, and {@code writableStackTrace}.
     *
     * @param message            the exception's message
     * @param cause              the exception's cause
     * @param enableSuppression  whether or not suppression is enabled
     *                           *                          or disabled
     * @param writableStackTrace whether or not the stack trace should be writable
     * @see RuntimeException#RuntimeException(String, Throwable, boolean, boolean)
     */
    protected JarvisException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
