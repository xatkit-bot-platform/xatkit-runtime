package com.xatkit.core.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.xatkit.core.XatkitException;
import com.xatkit.core.platform.RuntimePlatform;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.entity.ContentType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A handler that receives HTTP requests containing JSON and process them.
 * <p>
 * This class can be used to define REST endpoints from {@link RuntimePlatform}s. The endpoint can be registered
 * using the following code:
 * <pre>
 * {@code
 * XatkitServer xatkitServer = [...]
 * String restEndpointURI = "/myEndpoint";
 * xatkitServer.registerRestEndpoint(HttpMethod.GET, restEndpointURI, RestHandlerFactory.createJsonRestHandler(
 *  (headers, params, content) -> {
 *     // Handle the request
 *     // return an Object that will be embedded in the HTTP response
 * }
 * }
 * </pre>
 *
 * @see XatkitServer
 */
public abstract class JsonRestHandler extends RestHandler<JsonElement> {

    /**
     * The {@link JsonParser} used to parse the raw HTTP request content.
     *
     * @see #parseContent(Object)
     * @see #handleParsedContent(List, List, JsonElement)
     */
    private static JsonParser jsonParser = new JsonParser();

    /**
     * Returns {@code true} if the provided {@code contentType} represents a Json content, {@code false} otherwise.
     *
     * @param contentType the content type to check
     * @return {@code true} if the provided {@code contentType} represents a Json content, {@code false} otherwise
     */
    @Override
    public final boolean acceptContentType(String contentType) {
        return ContentType.APPLICATION_JSON.getMimeType().equals(contentType);
    }

    /**
     * Parses the provided raw HTTP request content into a {@link JsonElement}.
     * <p>
     * This method is internally used to fill the {@link #handleParsedContent(List, List, JsonElement)} parameter
     * with the {@link JsonElement} constructed from the raw request content.
     *
     * @param content the raw HTTP request content to parse
     * @return a {@link JsonElement} representing the raw request content
     * @throws XatkitException if the provided {@code content} is cannot be parsed by the {@link JsonParser}.
     * @see #handleParsedContent(List, List, JsonElement)
     */
    @Override
    protected final JsonElement parseContent(Object content) {
        if (isNull(content)) {
            return null;
        }
        if (content instanceof String) {
            return jsonParser.parse((String) content);
        }
        if (content instanceof Reader) {
            return jsonParser.parse((Reader) content);
        }
        if (content instanceof JsonReader) {
            return jsonParser.parse((JsonReader) content);
        }
        throw new XatkitException(MessageFormat.format("Cannot parse the provided content {0}, expected a {1}, {2}, " +
                "or {3}, found {4}", content, String.class.getName(), Reader.class.getName(), JsonReader.class
                .getName(), content.getClass().getName()));
    }

    /**
     * Handles the received {@code headers}, {@code params}, and {@link JsonElement} parsed from the request payload.
     *
     * @param headers the HTTP headers of the received request
     * @param params  the request parameters
     * @param content the {@link JsonElement} representing the content of the HTTP request
     * @return the {@link JsonElement} containing the endpoint response, or {@code null}
     */
    @Override
    public abstract JsonElement handleParsedContent(@Nonnull List<Header> headers, @Nonnull List<NameValuePair> params,
                                                    @Nullable JsonElement content) throws RestHandlerException;

    /**
     * A static class containing utility methods to manipulate Json contents.
     */
    public static class JsonHelper {

        /**
         * Returns the {@link JsonElement} associated to the given {@code key} in the provided {@code object}.
         * <p>
         * This method throws a {@link XatkitException} if the {@code key} field of the provided {@link JsonObject}
         * is {@code null}. The thrown exception can be globally caught to avoid multiple {@code null} checks
         * when manipulating {@link JsonObject}s.
         *
         * @param object the {@link JsonObject} to retrieve the field of
         * @param key    the identifier of the field in the provided {@code object} to retrieve
         * @return the {@link JsonElement} associated to the given {@code key} in the provided {@code object}
         * @throws XatkitException if the {@code key} field is {@code null}
         */
        public static JsonElement getJsonElementFromJsonObject(JsonObject object, String key) {
            JsonElement element = object.get(key);
            if (nonNull(element)) {
                return element;
            } else {
                throw new XatkitException(MessageFormat.format("The Json object does not contain the field {0}", key));
            }
        }

    }

}
