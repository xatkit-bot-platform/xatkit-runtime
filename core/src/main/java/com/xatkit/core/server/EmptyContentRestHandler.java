package com.xatkit.core.server;

import org.apache.http.Header;
import org.apache.http.NameValuePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.isNull;

/**
 * A handler that receives HTTP requests with empty content and process them.
 * <p>
 * This class can be used to define REST endpoints from {@link com.xatkit.core.platform.RuntimePlatform}s. The
 * endpoint can be registered using the following code:
 * <pre>
 * {@code
 * XatkitServer xatkitServer = [...]
 * String restEndpointURI = "/myEndpoint";
 * xatkitServer.registerRestEndpoint(HttpMethod.GET, restEndpointURI, RestHandlerFactory.createEmptyContentRestHandler(
 *  (headers, params, content) -> {
 *      // Handle the request
 *      // return an Object that will be embedded in the HTTP response
 *  }
 * }
 * </pre>
 */
public abstract class EmptyContentRestHandler extends RestHandler<Object> {

    /**
     * Returns {@code true}.
     * <p>
     * This handler accepts any content type, but will throw an exception if the request's content is not empty.
     *
     * @param contentType the content type to check
     * @return {@code true}
     */
    @Override
    public boolean acceptContentType(String contentType) {
        return true;
    }

    /**
     * Checks that the provided {@code content} object is {@code null} and returns it.
     * <p>
     * This method throws an exception if the provided {@code content} object is not {@code null}. The handler does
     * not support request's content, see other handlers to deal with specific content type (e.g.
     * {@link JsonRestHandler}).
     *
     * @param content the raw HTTP request content to parse
     * @return {@code null}
     * @throws IllegalArgumentException if the provided {@code content} is not null
     */
    @Nullable
    @Override
    protected Object parseContent(@Nullable Object content) {
        checkArgument(isNull(content), "Cannot parse the provided content %s, %s expects empty requests (with null " +
                "content)", content, this.getClass().getSimpleName());
        return null;
    }

    /**
     * Handles the request.
     * <p>
     * <b>Note</b>: the {@code content} object is always {@code null} (this handler does not support request's content).
     *
     * @param headers the HTTP headers of the received request
     * @param params  the request parameters
     * @param content the parsed request payload to handle
     * @return the handler's result
     * @throws RestHandlerException if an error occurred when handling the request
     */
    @Nullable
    @Override
    protected abstract Object handleParsedContent(@Nonnull List<Header> headers, @Nonnull List<NameValuePair> params,
                                                  @Nullable Object content) throws RestHandlerException;
}
