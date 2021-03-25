package com.xatkit.core.server;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xatkit.core.XatkitException;
import org.apache.http.HttpEntity;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;

/**
 * Contains utility methods to manipulate and create {@link HttpEntity} instances.
 */
public final class HttpEntityHelper {

    /**
     * Disables the default constructor, this class only provides static methods and should not be constructed.
     */
    private HttpEntityHelper() { }

    /**
     * The {@link Gson} instance used to translate {@link JsonElement}s to {@link String}s.
     */
    private static Gson gson = new Gson();

    /**
     * The {@link JsonParser} used to parse entity content into {@link JsonElement}s.
     */
    private static JsonParser jsonParser = new JsonParser();

    /**
     * Returns the {@link String} representation of the content of the provided {@code entity}.
     *
     * @param entity the {@link HttpEntity} to return a {@link String} from
     * @return the {@link String} extracted from the content of the provided {@code entity}
     * @throws NullPointerException if the provided {@code entity} is {@code null}
     * @throws XatkitException      if an error occurred when processing the provided {@code entity}
     */
    public static String getStringFromHttpEntity(@Nonnull HttpEntity entity) {
        checkNotNull(entity, "Cannot compute the String representation of the provided %s %s",
                HttpEntity.class.getSimpleName(), entity);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
            StringBuilder contentBuilder = new StringBuilder();
            String currentLine;
            while (nonNull(currentLine = reader.readLine())) {
                contentBuilder.append(currentLine);
            }
            return contentBuilder.toString();
        } catch (IOException e) {
            throw new XatkitException(MessageFormat.format("An error occurred when parsing the entity {0}", entity), e);
        }
    }


    /**
     * Returns a {@link JsonElement} extracted from the content of the provided {@code entity}.
     *
     * @param entity the {@link HttpEntity} to return a {@link JsonElement} from
     * @return the {@link JsonElement} extracted from the content of the provided {@code entity}
     * @throws NullPointerException                if the provided {@code entity} is {@code null}
     * @throws com.google.gson.JsonSyntaxException if the provided {@code entity} does not contain a valid JSON element
     * @throws XatkitException                     if an error occurred when processing the provided {@code entity}
     */
    public static JsonElement getJsonElementFromHttpEntity(@Nonnull HttpEntity entity) {
        String stringEntity = getStringFromHttpEntity(entity);
        return jsonParser.parse(stringEntity);
    }

    /**
     * Creates an {@link HttpEntity} from the provided {@code object}.
     *
     * @param object the {@link Object} to translate to an {@link HttpEntity}
     * @return the created {@link HttpEntity}
     * @throws NullPointerException if the provided {@code object} is {@code null}
     * @throws XatkitException      if the provided {@code object}'s type is not supported
     */
    public static HttpEntity createHttpEntity(@Nonnull Object object) {
        checkNotNull(object, "Cannot create an %s from the provided %s %s", HttpEntity.class,
                Object.class.getSimpleName(), object);
        if (object instanceof JsonElement) {
            return createHttpEntityFromJsonElement((JsonElement) object);
        } else {
            throw new XatkitException(MessageFormat.format("Cannot create an %s from the provided type %s",
                    HttpEntity.class.getSimpleName(), object.getClass().getSimpleName()));
        }
    }

    /**
     * Creates an {@link HttpEntity} representing the error wrapped in the provided {@link RestHandlerException}.
     * <p>
     * The created {@link HttpEntity} contains a Json element with the following content:
     * <pre>
     * {@code
     *  {
     *      "error" : <e.getMessage()>
     *  }
     * }
     * </pre>
     *
     * @param e the {@link RestHandlerException} to transform into an error {@link HttpEntity}
     * @return the created {@link HttpEntity}
     * @throws NullPointerException if the provided {@link RestHandlerException} is {@code null}
     */
    public static HttpEntity createErrorEntity(@Nonnull RestHandlerException e) {
        checkNotNull(e, "Cannot create an error %s from the provided %s %s", HttpEntity.class.getSimpleName(),
                RestHandlerException.class.getSimpleName(), e);
        JsonObject errorObject = new JsonObject();
        errorObject.addProperty("error", e.getMessage());
        return createHttpEntity(errorObject);
    }

    /**
     * Creates a {@link HttpEntity} from the provided {@code element}.
     * <p>
     * This method wraps the {@link String} representation of the provided {@code element} into an {@link HttpEntity}
     * , allowing to embed it in the response.
     *
     * @param element the {@link JsonElement} to embed in a {@link HttpEntity}
     * @return the created {@link HttpEntity}
     * @throws NullPointerException if the provided element is {@code null}
     * @see Gson#toJson(Object)
     */
    private static HttpEntity createHttpEntityFromJsonElement(@Nonnull JsonElement element) {
        checkNotNull(element, "Cannot create an %s from the provided %s %s", HttpEntity.class.getSimpleName(),
                JsonElement.class.getSimpleName(), element);
        String rawJson = gson.toJson(element);
        byte[] jsonBytes = rawJson.getBytes(StandardCharsets.UTF_8);
        InputStream is = new ByteArrayInputStream(jsonBytes);
        /*
         * Use the size of the byte array, it may be longer than the size of the string for special characters.
         */
        return createHttpEntityFromInputStream(is, jsonBytes.length, ContentType.APPLICATION_JSON.getMimeType());
    }

    /**
     * Creates a {@link HttpEntity} with the given {@code contentLength} and {@code contentType}, from the provided
     * {@code is}.
     *
     * @param is            the {@link InputStream} to set in the {@link HttpEntity}
     * @param contentLength the content length of the {@link HttpEntity}
     * @param contentType   the content type of the {@link HttpEntity}
     * @return the created {@link HttpEntity}
     * @see HttpEntity#getContentType()
     * @see HttpEntity#getContentLength()
     */
    private static HttpEntity createHttpEntityFromInputStream(@Nonnull InputStream is, int contentLength,
                                                              String contentType) {
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(is);
        entity.setContentLength(contentLength);
        entity.setContentType(contentType);
        entity.setContentEncoding(HTTP.UTF_8);
        return entity;
    }
}
