package fr.zelus.jarvis.io;

import fr.zelus.jarvis.core.JarvisCore;
import org.apache.commons.configuration2.Configuration;
import org.apache.http.Header;

/**
 * A specialised {@link EventProvider} that handles HTTP requests sent by the
 * {@link fr.zelus.jarvis.server.JarvisServer}.
 * <p>
 * This class defines primitives to handle raw HTTP request contents, manipulate the parsed content, and provides an
 * utility method that checks if the {@link WebhookEventProvider} accepts a given {@code contentType}.
 *
 * @param <T> the type of the parsed HTTP request content
 */
public abstract class WebhookEventProvider<T> extends EventProvider {

    /**
     * Constructs a new {@link WebhookEventProvider} from the provided {@code jarvisCore}.
     * <p>
     * <b>Note</b>: this constructor should be used by {@link WebhookEventProvider}s that do not require additional
     * parameters to be initialized. In that case see {@link #WebhookEventProvider(JarvisCore, Configuration)}.
     *
     * @param jarvisCore the {@link JarvisCore} instance used to handle {@link fr.zelus.jarvis.intent.EventInstance}s.
     */
    public WebhookEventProvider(JarvisCore jarvisCore) {
        super(jarvisCore);
    }

    /**
     * Constructs a new {@link WebhookEventProvider} from the provided {@link JarvisCore} and {@link Configuration}.
     * <p>
     * <b>Note</b>: this constructor will be called by jarvis internal engine when initializing the
     * {@link fr.zelus.jarvis.core.JarvisCore} component. Subclasses implementing this constructor typically
     * need additional parameters to be initialized, that can be provided in the {@code configuration}.
     *
     * @param jarvisCore    the {@link JarvisCore} instance used to handle input messages
     * @param configuration the {@link Configuration} used to initialize the {@link WebhookEventProvider}
     */
    public WebhookEventProvider(JarvisCore jarvisCore, Configuration configuration) {
        super(jarvisCore, configuration);
    }

    /**
     * Returns whether the {@link WebhookEventProvider} accepts the provided {@code contentType}.
     *
     * @param contentType the content type to check
     * @return {@code true} if the {@link WebhookEventProvider} accepts the provided {@code contentType}, {@code
     * false} otherwise
     */
    public abstract boolean acceptContentType(String contentType);

    /**
     * Parses the provided raw HTTP request content.
     * <p>
     * This method is internally used to fill the {@link #handleParsedContent(Object, Header[])} parameter with a parsed
     * representation of the raw request content.
     *
     * @param content the raw HTTP request content to parse
     * @return a parsed representation of the raw request content
     * @see #handleParsedContent(Object, Header[])
     */
    protected abstract T parseContent(Object content);

    /**
     * Handles the parsed request content and headers.
     * <p>
     * This method embeds the request content management that creates the associated
     * {@link fr.zelus.jarvis.intent.EventInstance}. The {@code parsedContent} parameter is set by an internal call
     * to {@link #parseContent(Object)}.
     *
     * @param parsedContent the parsed request content to handle
     * @param headers the HTTP headers of the received request
     * @see #parseContent(Object)
     */
    protected abstract void handleParsedContent(T parsedContent, Header[] headers);

    /**
     * Handles the raw HTTP request content and headers.
     * <p>
     * This method parses the provided {@code content} and internally calls
     * {@link #handleParsedContent(Object, Header[])} to
     * create the associated {@link fr.zelus.jarvis.intent.EventInstance}s.
     * <p>
     * This method is part of the core API and cannot be reimplemented by concrete subclasses. Use
     * {@link #handleParsedContent(Object, Header[])} to tune the request content processing.
     *
     * @param content the raw HTTP request content to handle
     * @param headers the HTTP headers of the received request
     * @see #parseContent(Object)
     * @see #handleParsedContent(Object, Header[])
     */
    public final void handleContent(Object content, Header[] headers) {
        T parsedContent = parseContent(content);
        handleParsedContent(parsedContent, headers);
    }

}
