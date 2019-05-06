package edu.uoc.som.jarvis.core.server;

import com.google.gson.JsonElement;
import edu.uoc.som.jarvis.core.JarvisException;
import edu.uoc.som.jarvis.core.platform.io.WebhookEventProvider;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.BindException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;

/**
 * The REST server used to receive external webhooks.
 * <p>
 * The {@link JarvisServer} provides a simple REST API that accepts POST methods on port {@code 5000}. Incoming
 * requests are parsed and sent to the registered {@link WebhookEventProvider}s, that transform the
 * original request into {@link edu.uoc.som.jarvis.intent.EventInstance}s that can be used to trigger actions.
 *
 * @see #registerWebhookEventProvider(WebhookEventProvider)
 */
public class JarvisServer {

    /**
     * The {@link Configuration} key to store the server port to use.
     */
    public static String SERVER_PORT_KEY = "jarvis.server.port";

    /**
     * The default port to use.
     * <p>
     * The server port can be customized in the constructor's {@link Configuration} using the {@link #SERVER_PORT_KEY}
     * key.
     */
    protected static int DEFAULT_SERVER_PORT = 5000;

    /**
     * The {@link HttpServer} used to receive input requests.
     */
    private HttpServer server;

    /**
     * A boolean flag representing whether the {@link JarvisServer} is started.
     *
     * @see #isStarted()
     */
    private boolean isStarted;

    /**
     * The port the {@link JarvisServer} should listen to.
     * <p>
     * The {@link JarvisServer} port can be specified in the constructor's {@link Configuration} with the key
     * {@link #SERVER_PORT_KEY}. The default value is {@code 5000}.
     */
    private int port;

    /**
     * The {@link WebhookEventProvider}s to notify when a request is received.
     * <p>
     * These {@link WebhookEventProvider}s are used to parse the input requests and create the corresponding
     * {@link edu.uoc.som.jarvis.intent.EventInstance}s that can be used to trigger actions.
     */
    private Set<WebhookEventProvider> webhookEventProviders;

    /**
     * Stores the REST endpoint to notify when a request is received.
     * <p>
     * This {@link Map} maps an URI (e.g. {@code /myEndpoint}) to a {@link JsonRestHandler} that takes care of the
     * REST service computation.
     *
     * @see #notifyRestHandler(String, List, List, JsonElement)
     */
    private Map<String, JsonRestHandler> restEndpoints;

    /**
     * Constructs a new {@link JarvisServer} with the given {@link Configuration}.
     * <p>
     * The provided {@link Configuration} is used to specify the port the server should listen to (see
     * {@link #SERVER_PORT_KEY}). If the {@link Configuration} does not specify a port the default value ({@code
     * 5000}) is used.
     * <p>
     * <b>Note:</b> this method does not start the underlying {@link HttpServer}. Use {@link #start()} to start the
     * {@link HttpServer} in a dedicated thread.
     *
     * @param configuration the {@link Configuration} used to initialize the {@link JarvisServer}
     * @throws NullPointerException if the provided {@code configuration} is {@code null}
     * @see #start()
     * @see #stop()
     */
    public JarvisServer(Configuration configuration) {
        checkNotNull(configuration, "Cannot start the %s with the provided %s: %s", this.getClass().getSimpleName
                (), Configuration.class.getSimpleName(), configuration);
        Log.info("Creating {0}", this.getClass().getSimpleName());
        this.isStarted = false;
        this.port = configuration.getInt(SERVER_PORT_KEY, DEFAULT_SERVER_PORT);
        Log.info("{0} listening to port {1}", this.getClass().getSimpleName(), port);
        webhookEventProviders = new HashSet<>();
        this.restEndpoints = new HashMap<>();
        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(15000)
                .setTcpNoDelay(true)
                .build();

        server = ServerBootstrap.bootstrap()
                .setListenerPort(port)
                .setServerInfo("Jarvis/1.1")
                .setSocketConfig(socketConfig)
                .setExceptionLogger(e -> Log.error(e))
                .registerHandler("*", new HttpHandler(this))
                .create();
    }

    /**
     * Returns the underlying {@link HttpServer} used to receive requests.
     * <p>
     * <b>Note:</b> this method is protected for testing purposes, and should not be called by client code.
     *
     * @return the {@link HttpServer} used to receive requests
     */
    protected HttpServer getHttpServer() {
        return this.server;
    }

    /**
     * Returns {@code true} if the {@link JarvisServer} is started, {@code false} otherwise.
     *
     * @return {@code true} if the {@link JarvisServer} is started, {@code false} otherwise
     */
    public boolean isStarted() {
        return isStarted;
    }

    /**
     * Starts the underlying {@link HttpServer}.
     * <p>
     * This method registered a shutdown hook that is used to close the {@link HttpServer} when the application
     * terminates. To manually close the underlying {@link HttpServer} see {@link #stop()}.
     */
    public void start() {
        Log.info("Starting {0}", this.getClass().getSimpleName());
        try {
            this.server.start();
        } catch (BindException e) {
            throw new JarvisException(MessageFormat.format("Cannot start the {0}, the port {1} cannot be bound. This " +
                    "may happen if another bot is started on the same port, if a previously started bot was not shut " +
                    "down properly, or if another application is already using the port", this.getClass()
                    .getSimpleName(), port), e);
        } catch (IOException e) {
            throw new JarvisException(MessageFormat.format("Cannot start the {0}, see attached exception", this
                    .getClass().getSimpleName()), e);
        }
        isStarted = true;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown(5, TimeUnit.SECONDS);
            isStarted = false;
        }));
        Log.info("JarvisServer started, listening on {0}:{1}", server.getInetAddress().toString(), server
                .getLocalPort());
    }

    /**
     * Stops the underlying {@link HttpServer}.
     */
    public void stop() {
        Log.info("Stopping JarvisServer");
        if (!isStarted) {
            Log.warn("Cannot stop the {0}, the server is not started", this.getClass().getSimpleName());
        }
        server.shutdown(5, TimeUnit.SECONDS);
        isStarted = false;
    }

    /**
     * Registers the provided {@code handler} as a REST endpoint for the provided {@code uri}.
     * <p>
     * The provided {@code handler} will receive the HTTP requests that are sent to the provided {@code uri} through
     * the {@link #notifyRestHandler(String, List, List, JsonElement)} method.
     * <p>
     * <b>Note</b>: the provided {@code uri} must start with a leading {@code /}.
     *
     * @param uri     the URI of the REST endpoint
     * @param handler the {@link JsonRestHandler} to associate to the REST endpoint
     * @throws NullPointerException     if the provided {@code uri} or {@code handler} is {@code null}
     * @throws IllegalArgumentException if the provided {@code uri} does not start with a leading {@code /}
     */
    public void registerRestEndpoint(String uri, JsonRestHandler handler) {
        checkNotNull(uri, "Cannot register a REST endpoint for the provided URI %s", uri);
        checkArgument(uri.startsWith("/"), "Cannot register a REST endpoint for the provided URI %s, the URI must " +
                "start with a \"/\"", uri);
        checkNotNull(handler, "Cannot register the provided %s %s (uri=%s)", JsonRestHandler.class.getSimpleName(),
                handler, uri);
        this.restEndpoints.put(uri, handler);
    }

    /**
     * Returns whether the provided {@code uri} is associated to a REST endpoint.
     *
     * @param uri the URI of the REST endpoint to check
     * @return {@code true} if there is a REST endpoint associated to the provided {@code uri}, {@code false} otherwise
     * @throws NullPointerException if the provided {@code uri} is {@code null}
     */
    public boolean isRestEndpoint(String uri) {
        checkNotNull(uri, "Cannot check if the provided URI (%s) corresponds to a REST endpoint, please provide a " +
                "non-null URI", uri);
        return this.restEndpoints.containsKey(uri);
    }

    /**
     * Notifies the REST endpoint associated with the provided {@code uri}.
     *
     * @param uri     the URI of the REST endpoint to notify
     * @param header  the HTTP {@link Header}s of the request sent to the endpoint
     * @param params  the HTTP parameters of the request sent to the endpoint
     * @param content the {@link JsonElement} representing the content of the request sent to the endpoint
     * @return the {@link JsonElement} returned by the endpoint, or {@code null} if the endpoint does not return
     * anything
     * @throws NullPointerException if the provided {@code uri}, {@code header}, or {@code params} is {@code null}
     * @throws JarvisException      if there is no REST endpoint registered for the provided {@code uri}
     * @see #registerRestEndpoint(String, JsonRestHandler)
     */
    public JsonElement notifyRestHandler(String uri, List<Header> header, List<NameValuePair> params,
                                         @Nullable JsonElement content) {
        checkNotNull(uri, "Cannot notify the REST endpoint %s, please provide a non-null URI", uri);
        checkNotNull(header, "Cannot notify the REST endpoint %s, the headers list is null", uri);
        checkNotNull(params, "Cannot notify the REST endpoint %s, the parameters list is null", uri);
        JsonRestHandler handler = this.restEndpoints.get(uri);
        if (isNull(handler)) {
            throw new JarvisException(MessageFormat.format("Cannot notify the REST endpoint {0}, there is no handler " +
                    "registered for this URI", uri));
        }
        return handler.handle(header, params, content);
    }

    /**
     * Register a {@link WebhookEventProvider}.
     * <p>
     * The registered {@code webhookEventProvider} will be notified when a new request is received. If the provider
     * supports the request content type (see {@link WebhookEventProvider#acceptContentType(String)}, it will receive
     * the request content that will be used to create the associated {@link edu.uoc.som.jarvis.intent.EventInstance}.
     *
     * @param webhookEventProvider the {@link WebhookEventProvider} to register
     * @throws NullPointerException if the provided {@code webhookEventProvider} is {@code null}
     * @see #notifyWebhookEventProviders(String, Object, Header[])
     * @see WebhookEventProvider#acceptContentType(String)
     * @see WebhookEventProvider#handleContent(Object, Header[])
     */
    public void registerWebhookEventProvider(WebhookEventProvider webhookEventProvider) {
        checkNotNull(webhookEventProvider, "Cannot register the provided %s: %s", WebhookEventProvider.class
                .getSimpleName(), webhookEventProvider);
        this.webhookEventProviders.add(webhookEventProvider);
    }

    /**
     * Unregistered a {@link WebhookEventProvider}.
     * <p>
     * The provided {@code webhookEventProvider} will not be notified when new request are received, and cannot be
     * used to create {@link edu.uoc.som.jarvis.intent.EventInstance}s.
     *
     * @param webhookEventProvider the {@link WebhookEventProvider} to unregister
     * @throws NullPointerException if the provided {@code webhookEventProvider} is {@code null}
     */
    public void unregisterWebhookEventProvider(WebhookEventProvider webhookEventProvider) {
        checkNotNull(webhookEventProvider, "Cannot unregister the provided %s: %s", WebhookEventProvider.class
                .getSimpleName(), webhookEventProvider);
        this.webhookEventProviders.remove(webhookEventProvider);
    }

    /**
     * Returns an unmodifiable {@link Collection} containing the registered {@link WebhookEventProvider}s.
     *
     * @return an unmodifiable {@link Collection} containiong the registered {@link WebhookEventProvider}
     */
    public Collection<WebhookEventProvider> getRegisteredWebhookEventProviders() {
        return Collections.unmodifiableSet(this.webhookEventProviders);
    }

    /**
     * Notifies the registered {@link WebhookEventProvider}s that a new request has been handled.
     * <p>
     * This method asks each registered {@link WebhookEventProvider} if it accepts the given {@code contentType}. If
     * so, the provided {@code content} is sent to the {@link WebhookEventProvider} that will create the associated
     * {@link edu.uoc.som.jarvis.intent.EventInstance}.
     *
     * @param contentType the content type of the received request
     * @param content     the content of the received request
     * @see #registerWebhookEventProvider(WebhookEventProvider)
     * @see WebhookEventProvider#acceptContentType(String)
     * @see WebhookEventProvider#handleContent(Object, Header[])
     */
    public void notifyWebhookEventProviders(String contentType, Object content, Header[] headers) {
        for (WebhookEventProvider webhookEventProvider : webhookEventProviders) {
            if (webhookEventProvider.acceptContentType(contentType)) {
                webhookEventProvider.handleContent(content, headers);
            }
        }
    }
}
