package fr.zelus.jarvis.io;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.core.JarvisException;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.http.entity.ContentType;

import java.io.Reader;
import java.text.MessageFormat;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * A Json {@link WebhookEventProvider} that provides utility methods to parse and manipulate Json HTTP requests.
 * <p>
 * This {@link WebhookEventProvider} should be extended by concrete providers that expect Json content in the HTTP
 * requests sent by the {@link fr.zelus.jarvis.server.JarvisServer}.
 */
public abstract class JsonWebhookEventProvider extends WebhookEventProvider<JsonElement> {

    /**
     * The {@link JsonParser} used to parse the raw HTTP request content.
     *
     * @see #parseContent(Object)
     * @see #handleParsedContent(JsonElement)
     */
    private JsonParser jsonParser;

    /**
     * Constructs a new {@link JsonWebhookEventProvider} from the provided {@code jarvisCore}.
     * <p>
     * <b>Note:</b> this constructor should be used by {@link JsonWebhookEventProvider}s that do not require
     * additional parameters to be initialized. In that case see
     * {@link #JsonWebhookEventProvider(JarvisCore, Configuration)}.
     *
     * @param jarvisCore the {@link JarvisCore} instance used to handle {@link fr.zelus.jarvis.intent.EventInstance}s.
     */
    public JsonWebhookEventProvider(JarvisCore jarvisCore) {
        this(jarvisCore, new BaseConfiguration());
    }

    /**
     * Constructs a new {@link JsonWebhookEventProvider} from the provided {@link JarvisCore} and {@link Configuration}.
     * <p>
     * <b>Note</b>: this constructor will be called by jarvis internal engine when initializing the
     * {@link fr.zelus.jarvis.core.JarvisCore} component. Subclasses implementing this constructor typically
     * need additional parameters to be initialized, that can be provided in the {@code configuration}.
     *
     * @param jarvisCore    the {@link JarvisCore} instance used to handle input messages
     * @param configuration the {@link Configuration} used to initialize the {@link JsonWebhookEventProvider}
     */
    public JsonWebhookEventProvider(JarvisCore jarvisCore, Configuration configuration) {
        super(jarvisCore, configuration);
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
     * This method is internally used to fill the {@link #handleParsedContent(JsonElement)} parameter with the
     * {@link JsonElement} constructed from the raw request content.
     *
     * @param content the raw HTTP request content to parse
     * @return a {@link JsonElement} representing the raw request content
     * @throws JarvisException if the provided {@code content} is cannot be parsed by the {@link JsonParser}.
     * @see #handleParsedContent(JsonElement)
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
     * @param parsedContent the {@link JsonElement} representing therequest content to handle
     * @see #parseContent(Object)
     */
    @Override
    protected abstract void handleParsedContent(JsonElement parsedContent);

}
