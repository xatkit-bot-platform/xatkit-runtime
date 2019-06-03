package com.xatkit.core.recognition;


import com.xatkit.core.XatkitException;

/**
 * Wraps all the exceptions thrown by the {@link IntentRecognitionProvider}s.
 */
public class IntentRecognitionProviderException extends XatkitException {

    /**
     * Constructs a new {@link IntentRecognitionProviderException}.
     *
     * @see RuntimeException#RuntimeException()
     */
    public IntentRecognitionProviderException() {
        super();
    }

    /**
     * Constructs a new {@link IntentRecognitionProviderException} from the provided {@code message}.
     *
     * @param message the exception's message
     * @see RuntimeException#RuntimeException(String)
     */
    public IntentRecognitionProviderException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@link IntentRecognitionProviderException} from the provided {@code message} and {@code cause}.
     *
     * @param message the exception's message
     * @param cause   the exception's cause
     * @see RuntimeException#RuntimeException(String, Throwable)
     */
    public IntentRecognitionProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@link IntentRecognitionProviderException} from the provided {@code cause}.
     *
     * @param cause the exception's cause
     * @see RuntimeException#RuntimeException(Throwable)
     */
    public IntentRecognitionProviderException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@link IntentRecognitionProviderException} from the provided {@code message}, {@code cause},
     * {@code enableSuppression}, and {@code writableStackTrace}.
     *
     * @param message            the exception's message
     * @param cause              the exception's cause
     * @param enableSuppression  whether or not suppression is enabled or disabled
     * @param writableStackTrace whether or not stack trace should be writable
     * @see RuntimeException#RuntimeException(String, Throwable, boolean, boolean)
     */
    public IntentRecognitionProviderException(String message, Throwable cause, boolean enableSuppression, boolean
            writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
