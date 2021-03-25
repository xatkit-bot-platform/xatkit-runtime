package com.xatkit.core.server;

import com.google.gson.JsonElement;
import org.apache.http.Header;
import org.apache.http.NameValuePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * A {@link RestHandler} factory providing utility methods to create specific {@link RestHandler} instances.
 */
public final class RestHandlerFactory {

    /**
     * Disables the default constructor, this class only provides static methods and should not be constructed.
     */
    private RestHandlerFactory() {
    }

    /**
     * A functional interface representing the handling function of {@link JsonRestHandler}.
     */
    @FunctionalInterface
    public interface JsonRestHandlerFunction {

        /**
         * The handling function of {@link JsonRestHandler}.
         * <p>
         * This method has the same signature as {@link JsonRestHandler#handleParsedContent(List, List, JsonElement)}
         * , and can be used in {@link #createJsonRestHandler(JsonRestHandlerFunction)} to set its behavior.
         *
         * @param headers the HTTP headers of the received request
         * @param params  the request parameters
         * @param content the {@link JsonElement} representing the content of the HTTP request
         * @return the {@link JsonElement} containing the endpoint response, or {@code null}
         * @throws RestHandlerException if an error occurred when handling the request
         * @see #createJsonRestHandler(JsonRestHandlerFunction)
         */
        @Nullable
        JsonElement handle(@Nonnull List<Header> headers, @Nonnull List<NameValuePair> params,
                           @Nullable JsonElement content) throws RestHandlerException;
    }

    /**
     * A functional interface representing the handling function of {@link EmptyContentRestHandler}.
     */
    @FunctionalInterface
    public interface EmptyContentRestHandlerFunction {

        /**
         * The handling function of {@link EmptyContentRestHandler}.
         * <p>
         * This method has the same signature as
         * {@link EmptyContentRestHandler#handleParsedContent(List, List, Object)}, and can be used in
         * {@link #createEmptyContentRestHandler(EmptyContentRestHandlerFunction)} to set its behavior.
         *
         * @param headers the HTTP headers of the received request
         * @param params  the request parameters
         * @param content {@code null} ({@link EmptyContentRestHandler} does not accept any request content)
         * @return the {@link Object} containing the endpoint result, or {@code null}
         * @throws RestHandlerException if an error occurred when handling the request
         */
        @Nullable
        Object handle(@Nonnull List<Header> headers, @Nonnull List<NameValuePair> params,
                      @Nullable Object content) throws RestHandlerException;
    }

    /**
     * Creates a {@link JsonRestHandler} and sets its handling method with the provided {@code function}.
     *
     * @param function the {@link JsonRestHandlerFunction} used to define the handler behavior
     * @return the created {@link JsonRestHandler}
     */
    public static JsonRestHandler createJsonRestHandler(final JsonRestHandlerFunction function) {
        return new JsonRestHandler() {
            @Nullable
            @Override
            public JsonElement handleParsedContent(@Nonnull List<Header> headers,
                                                   @Nonnull List<NameValuePair> params,
                                                   @Nullable JsonElement content) throws RestHandlerException {
                return function.handle(headers, params, content);
            }
        };
    }

    /**
     * Creates an {@link EmptyContentRestHandler} and sets its handling method with the provided {@code function}.
     *
     * @param function the {@link EmptyContentRestHandlerFunction} used to define the handler behavior
     * @return the created {@link EmptyContentRestHandler}
     */
    public static EmptyContentRestHandler createEmptyContentRestHandler(final EmptyContentRestHandlerFunction function) {
        return new EmptyContentRestHandler() {
            @Nullable
            @Override
            protected Object handleParsedContent(@Nonnull List<Header> headers, @Nonnull List<NameValuePair> params,
                                                 @Nullable Object content) throws RestHandlerException {
                return function.handle(headers, params, content);
            }
        };
    }
}
