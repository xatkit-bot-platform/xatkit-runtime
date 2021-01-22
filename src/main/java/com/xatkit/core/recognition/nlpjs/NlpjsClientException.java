package com.xatkit.core.recognition.nlpjs;

import javax.annotation.Nullable;

import static java.text.MessageFormat.format;

/**
 * Exception thrown by the {@link NlpjsClient} when an error occurred while performing a REST request.
 */
public class NlpjsClientException extends RuntimeException {

    /**
     * Creates a {@link NlpjsClientException} representing a NLP.js REST request error.
     * <p>
     * This constructor uses the provided parameters to create a human-readable exception message (see
     * {@link #getMessage()}). This constructor does not throw an exception if a string parameter is {@code null}
     * (the corresponding template parameter is not set in the message).
     *
     * @param method       the REST method of the request
     * @param url          the URL of the REST endpoint
     * @param status       the status value returned by the REST API
     * @param errorMessage the error message returned by the REST API
     */
    public NlpjsClientException(@Nullable String method, @Nullable String url, int status,
                                @Nullable String errorMessage) {
        super(format("Unsuccessful {0} on {1}. The API responded with the status {2} and the error message \"{3}\"",
                method, url, status, errorMessage));
    }

    /**
     * Creates a {@link NlpjsClientException} representing a NLP.js REST request error.
     * <p>
     * This constructor is typically used to wrap other exceptions thrown by the library that sends the requests to
     * the NLP.js server. See {@link #NlpjsClientException(String, String, int, String)} to create an instance of
     * this class with a specific error message.
     *
     * @param cause the cause of this exception
     */
    public NlpjsClientException(Throwable cause) {
        super(cause);
    }
}
