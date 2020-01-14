package com.xatkit.core.server;

import fr.inria.atlanmod.commons.log.Log;
import org.apache.http.Header;
import org.apache.http.NameValuePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * A generic Rest request handler.
 * <p>
 * This class provide utility methods to parse the received payload, handle its content, and define the content type
 * that are accepted by the handler.
 *
 * @param <C> the type of the parsed payload
 */
public abstract class RestHandler<C> {

    /**
     * Returns the {@code Access-Control-Allow-Headers} HTTP header values that must be set by the server when
     * calling this handler.
     * <p>
     * This method is used to ensure that cross-origin requests are accepted by client applications. Note that this
     * headers can be empty if this provider is not intended to be accessed in the browser.
     *
     * @return the HTTP headers to set
     */
    public Collection<String> getAccessControlAllowHeaders() {
        return Collections.emptyList();
    }

    /**
     * Returns whether the {@link RestHandler} accepts the provided {@code contentType}.
     *
     * @param contentType the content type to check
     * @return {@code true} if the {@link RestHandler} accepts the provided {@code contentType}, {@code
     * false} otherwise
     */
    public abstract boolean acceptContentType(String contentType);

    /**
     * Parses the provided raw HTTP request content.
     * <p>
     * This method is internally used to fill the {@link #handleParsedContent(List, List, Object)} parameter with a
     * parsed representation of the raw request content.
     *
     * @param content the raw HTTP request content to parse
     * @return a parsed representation of the raw request content
     * @see #handleParsedContent(List, List, Object)
     */
    protected abstract @Nullable
    C parseContent(@Nullable Object content);

    /**
     * Handles the received {@code headers}, {@code params}, and parsed payload.
     * <p>
     * The value returned by this method is embedded in the sever's response. If the return value is an intance of
     * {@link org.apache.http.HttpEntity} it is embedded as is, otherwise the server attempts to convert it into a
     * valid {@link org.apache.http.HttpEntity} using {@link HttpEntityHelper#createHttpEntity(Object)}.
     * <p>
     * The exception thrown by this method is used to determine the HTTP response's status. By default the server
     * returns a status code {@code 200}, but it will return a {@code 404} if this method threw an exception.
     *
     * @param headers the HTTP headers of the received request
     * @param params  the request parameters
     * @param content the parsed request payload to handle
     * @return an {@link Object} to embed in the server's response
     * @throws RestHandlerException if an error occurred when handling the request
     * @see #parseContent(Object)
     * @see HttpEntityHelper#createHttpEntity(Object)
     */
    protected abstract @Nullable
    Object handleParsedContent(@Nonnull List<Header> headers,
                               @Nonnull List<NameValuePair> params,
                               @Nullable C content) throws RestHandlerException;

    /**
     * Handles the raw HTTP request.
     * <p>
     * This method parses the provided {@code content} and internally calls
     * {@link #handleParsedContent(List, List, Object)} to handle the request.
     * <p>
     * This method is part of the core API and cannot be reimplemented by concrete subclasses. Use
     * {@link #handleParsedContent(List, List, Object)} to tune the request content processing.
     *
     * @param headers the HTTP headers of the received request
     * @param params  the request parameters
     * @param content the raw HTTP request content to handle
     * @return an {@link Object} to embed in the server's response
     * @throws RestHandlerException TODO
     * @see #parseContent(Object)
     * @see #handleParsedContent(List, List, Object)
     */
    public final @Nullable
    Object handleContent(@Nonnull List<Header> headers, @Nonnull List<NameValuePair> params,
                         @Nullable Object content) throws RestHandlerException {
        C parsedContent = parseContent(content);
        return handleParsedContent(headers, params, parsedContent);
    }

    /**
     * Returns the {@link Header} value associated to the provided {@code headerKey}.
     * <p>
     * This method is an utility method that can be called by subclasses'
     * {@link #handleParsedContent(List, List, Object)} implementation to retrieve specific values from the request
     * {@link Header}s.
     *
     * @param headers   the list of {@link Header} to retrieve the value from
     * @param headerKey the {@link Header} key to retrieve the value of
     * @return the {@link Header} value associated to the provided {@code headerKey} if it exists, {@code null}
     * otherwise
     */
    protected String getHeaderValue(List<Header> headers, String headerKey) {
        checkNotNull(headerKey, "Cannot retrieve the header value %s", headerKey);
        checkNotNull(headers, "Cannot retrieve the header value %s from the provided %s Array %s", headerKey, Header
                .class.getSimpleName(), headers);
        for (Header header : headers) {
            if (headerKey.equals(header.getName())) {
                return header.getValue();
            }
        }
        Log.warn("Unable to retrieve the value {0} from the provided {1} Array", headerKey, Header.class
                .getSimpleName());
        return null;
    }
}

