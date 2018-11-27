package fr.zelus.jarvis.io;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import fr.zelus.jarvis.core.JarvisException;
import fr.zelus.jarvis.core.platform.RuntimePlatform;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.http.Header;
import org.apache.http.entity.ContentType;

import java.io.Reader;
import java.text.MessageFormat;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;

/**
 * A Json {@link WebhookEventProvider} that provides utility methods to parse and manipulate Json HTTP requests.
 * <p>
 * This {@link WebhookEventProvider} should be extended by concrete providers that expect Json content in the HTTP
 * requests sent by the {@link fr.zelus.jarvis.server.JarvisServer}.
 *
 * @param <T> the concrete {@link RuntimePlatform} subclass type containing the provider
 */
public abstract class JsonWebhookEventProvider<T extends RuntimePlatform> extends WebhookEventProvider<T, JsonElement> {

    /**
     * The {@link JsonParser} used to parse the raw HTTP request content.
     *
     * @see #parseContent(Object)
     * @see #handleParsedContent(JsonElement, Header[])
     */
    private JsonParser jsonParser;

    /**
     * Constructs a new {@link JsonWebhookEventProvider} with the provided {@code runtimePlatform}.
     * <p>
     * <b>Note:</b> this constructor should be used by {@link JsonWebhookEventProvider}s that do not require
     * additional parameters to be initialized. In that case see
     * {@link #JsonWebhookEventProvider(RuntimePlatform, Configuration)}.
     *
     * @param runtimePlatform the {@link RuntimePlatform} containing this {@link JsonWebhookEventProvider}
     * @throws NullPointerException if the provided {@code runtimePlatform} is {@code null}
     */
    public JsonWebhookEventProvider(T runtimePlatform) {
        this(runtimePlatform, new BaseConfiguration());
    }

    /**
     * Constructs a new {@link JsonWebhookEventProvider} with the provided {@code runtimePlatform} and
     * {@code configuration}.
     * <p>
     * <b>Note</b>: this constructor will be called by jarvis internal engine when initializing the
     * {@link fr.zelus.jarvis.core.JarvisCore} component. Subclasses implementing this constructor typically
     * need additional parameters to be initialized, that can be provided in the {@code configuration}.
     *
     * @param runtimePlatform the {@link RuntimePlatform} containing this {@link JsonWebhookEventProvider}
     * @param configuration    the {@link Configuration} used to initialize the {@link JsonWebhookEventProvider}
     * @throws NullPointerException if the provided {@code runtimePlatform} is {@code null}
     */
    public JsonWebhookEventProvider(T runtimePlatform, Configuration configuration) {
        super(runtimePlatform, configuration);
        this.jsonParser = new JsonParser();
    }

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
     * This method is internally used to fill the {@link #handleParsedContent(JsonElement, Header[])} parameter with the
     * {@link JsonElement} constructed from the raw request content.
     *
     * @param content the raw HTTP request content to parse
     * @return a {@link JsonElement} representing the raw request content
     * @throws JarvisException if the provided {@code content} is cannot be parsed by the {@link JsonParser}.
     * @see #handleParsedContent(JsonElement, Header[])
     */
    @Override
    protected final JsonElement parseContent(Object content) {
        checkNotNull(content, "Cannot parse the provided content %s", content);
        if (content instanceof String) {
            return jsonParser.parse((String) content);
        }
        if (content instanceof Reader) {
            return jsonParser.parse((Reader) content);
        }
        if (content instanceof JsonReader) {
            return jsonParser.parse((JsonReader) content);
        }
        throw new JarvisException(MessageFormat.format("Cannot parse the provided content {0}, expected a {1}, {2}, " +
                "or {3}, found {4}", content, String.class.getName(), Reader.class.getName(), JsonReader.class
                .getName(), content.getClass().getName()));
    }

    /**
     * Handles the {@link JsonElement} representing the request content.
     * <p>
     * This method embeds the request content management that creates the associated
     * {@link fr.zelus.jarvis.intent.EventInstance}. The {@code parsedContent} parameter is set by an internal call
     * to {@link #parseContent(Object)}.
     *
     * @param parsedContent the {@link JsonElement} representing the request content to handle
     * @param headers       the HTTP headers of the received request
     * @see #parseContent(Object)
     */
    @Override
    protected abstract void handleParsedContent(JsonElement parsedContent, Header[] headers);

    /*
     * A static class containing utility methods to manipulate Json contents.
     */
    public static class JsonHelper {

        /**
         * Returns the {@link JsonElement} associated to the given {@code key} in the provided {@code object}.
         * <p>
         * This method throws a {@link JarvisException} if the {@code key} field of the provided {@link JsonObject}
         * is {@code null}. The thrown exception can be globally caught to avoid multiple {@code null} checks
         * when manipulating {@link JsonObject}s.
         *
         * @param object the {@link JsonObject} to retrieve the field of
         * @param key    the identifier of the field in the provided {@code object} to retrieve
         * @return the {@link JsonElement} associated to the given {@code key} in the provided {@code object}
         * @throws JarvisException if the {@code key} field is {@code null}
         */
        public static JsonElement getJsonElementFromJsonObject(JsonObject object, String key) {
            JsonElement element = object.get(key);
            if (nonNull(element)) {
                return element;
            } else {
                throw new JarvisException(MessageFormat.format("The Json object does not contain the field {0}", key));
            }
        }

    }

}
