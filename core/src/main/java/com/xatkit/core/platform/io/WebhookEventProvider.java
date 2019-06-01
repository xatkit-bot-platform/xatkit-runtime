package com.xatkit.core.platform.io;

import com.xatkit.core.JarvisCore;
import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.server.JarvisServer;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;
import org.apache.http.Header;

import java.util.Collections;
import java.util.List;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * A specialised {@link RuntimeEventProvider} that handles HTTP requests sent by the
 * {@link JarvisServer}.
 * <p>
 * This class defines primitives to handle raw HTTP request contents, manipulate the parsed content, and provides an
 * utility method that checks if the {@link WebhookEventProvider} accepts a given {@code contentType}.
 *
 * @param <T> the concrete {@link RuntimePlatform} subclass type containing the provider
 * @param <C> the type of the parsed HTTP request content
 */
public abstract class WebhookEventProvider<T extends RuntimePlatform, C> extends RuntimeEventProvider<T> {

    /**
     * Constructs a new {@link WebhookEventProvider} with the provided {@code runtimePlatform}.
     * <p>
     * <b>Note</b>: this constructor should be used by {@link WebhookEventProvider}s that do not require additional
     * parameters to be initialized. In that case see {@link #WebhookEventProvider(RuntimePlatform, Configuration)}.
     *
     * @param runtimePlatform the {@link RuntimePlatform} containing this {@link WebhookEventProvider}
     * @throws NullPointerException if the provided {@code runtimePlatform} is {@code null}
     */
    public WebhookEventProvider(T runtimePlatform) {
        super(runtimePlatform);
    }

    /**
     * Constructs a new {@link WebhookEventProvider} from the provided {@code runtimePlatform} and
     * {@code configuration}.
     * <p>
     * <b>Note</b>: this constructor will be called by jarvis internal engine when initializing the
     * {@link JarvisCore} component. Subclasses implementing this constructor typically
     * need additional parameters to be initialized, that can be provided in the {@code configuration}.
     *
     * @param runtimePlatform the {@link RuntimePlatform} containing this {@link WebhookEventProvider}
     * @param configuration   the {@link Configuration} used to initialize the {@link WebhookEventProvider}
     * @throws NullPointerException if the provided {@code runtimePlatform} is {@code null}
     */
    public WebhookEventProvider(T runtimePlatform, Configuration configuration) {
        super(runtimePlatform, configuration);
    }

    /**
     * Returns the {@code Access-Control-Allow-Headers} HTTP header values that must be set by the server when
     * calling this provider.
     * <p>
     * This method is used to ensure that cross-origin requests are accepted by client applications. Note that this
     * headers can be empty if this provider is not intended to be accessed in the browser.
     *
     * @return the HTTP headers to set
     */
    public List<String> getAccessControlAllowHeaders() {
        return Collections.emptyList();
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
    protected abstract C parseContent(Object content);

    /**
     * Handles the parsed request content and headers.
     * <p>
     * This method embeds the request content management that creates the associated
     * {@link com.xatkit.intent.EventInstance}. The {@code parsedContent} parameter is set by an internal call
     * to {@link #parseContent(Object)}.
     *
     * @param parsedContent the parsed request content to handle
     * @param headers       the HTTP headers of the received request
     * @see #parseContent(Object)
     */
    protected abstract void handleParsedContent(C parsedContent, Header[] headers);

    /**
     * Handles the raw HTTP request content and headers.
     * <p>
     * This method parses the provided {@code content} and internally calls
     * {@link #handleParsedContent(Object, Header[])} to
     * create the associated {@link com.xatkit.intent.EventInstance}s.
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
        C parsedContent = parseContent(content);
        handleParsedContent(parsedContent, headers);
    }

    /**
     * Returns the {@link Header} value associated to the provided {@code headerKey}.
     * <p>
     * This method is an utility method that can be called by subclasses'
     * {@link #handleParsedContent(Object, Header[])} implementation to retrieve specific values from the request
     * {@link Header}s.
     *
     * @param headers   the array of {@link Header} to retrieve the value from
     * @param headerKey the {@link Header} key to retrieve the value of
     * @return the {@link Header} value associated to the provided {@code headerKey} if it exists, {@code null}
     * otherwise
     */
    protected String getHeaderValue(Header[] headers, String headerKey) {
        checkNotNull(headerKey, "Cannot retrieve the header value %s", headerKey);
        checkNotNull(headers, "Cannot retrieve the header value %s from the provided %s Array %s", headerKey, Header
                .class.getSimpleName(), headers);
        for (int i = 0; i < headers.length; i++) {
            if (headerKey.equals(headers[i].getName())) {
                return headers[i].getValue();
            }
        }
        Log.warn("Unable to retrieve the value {0} from the provided {1} Array", headerKey, Header.class
                .getSimpleName());
        return null;
    }

}
