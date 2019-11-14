package com.xatkit.core.server;

import com.google.gson.JsonElement;
import com.xatkit.core.XatkitException;
import com.xatkit.core.platform.io.WebhookEventProvider;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.util.FileUtils;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.SocketTimeoutException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;

/**
 * The REST server used to receive external webhooks.
 * <p>
 * The {@link XatkitServer} provides a simple REST API that accepts POST methods on port {@code 5000}. Incoming
 * requests are parsed and sent to the registered {@link WebhookEventProvider}s, that transform the
 * original request into {@link com.xatkit.intent.EventInstance}s that can be used to trigger actions.
 *
 * @see #registerWebhookEventProvider(WebhookEventProvider)
 */
public class XatkitServer {

    /**
     * The base URL of the {@link XatkitServer}.
     * <p>
     * This URL contains the base address and port ({@code <address:port>}) to access the {@link XatkitServer} handlers.
     */
    private String baseURL;

    /**
     * The {@link HttpServer} used to receive input requests.
     */
    private HttpServer server;

    /**
     * A boolean flag representing whether the {@link XatkitServer} is started.
     *
     * @see #isStarted()
     */
    private boolean isStarted;

    /**
     * The port the {@link XatkitServer} should listen to.
     * <p>
     * The {@link XatkitServer} port can be specified in the constructor's {@link Configuration} with the key
     * {@link XatkitServerUtils#SERVER_PORT_KEY}. The default value is {@code 5000}.
     */
    private int port;

    /**
     * Stores the REST endpoint to notify when a request is received.
     * <p>
     * This {@link Map} maps an URI (e.g. {@code /myEndpoint}) to a {@link RestHandler} that takes care of the
     * REST service computation.
     *
     * @see #notifyRestHandler(HttpMethod, String, List, List, Object, String)
     */
    private Map<EndpointEntry, RestHandler> restEndpoints;

    /**
     * The directory used to store public content that can be accessed through the {@link ContentHttpHandler}.
     */
    private File contentDirectory;

    /**
     * The {@link Path} of the directory used to store public content that can be accessed through the
     * {@link ContentHttpHandler}.
     *
     * @see #contentDirectory
     */
    private Path contentDirectoryPath;

    /**
     * Constructs a new {@link XatkitServer} with the given {@link Configuration}.
     * <p>
     * The provided {@link Configuration} is used to specify the port the server should listen to (see
     * {@link XatkitServerUtils#SERVER_PORT_KEY}). If the {@link Configuration} does not specify a port the default
     * value ({@code
     * 5000}) is used.
     * <p>
     * <b>Note:</b> this method does not start the underlying {@link HttpServer}. Use {@link #start()} to start the
     * {@link HttpServer} in a dedicated thread.
     *
     * @param configuration the {@link Configuration} used to initialize the {@link XatkitServer}
     * @throws NullPointerException if the provided {@code configuration} is {@code null}
     * @see #start()
     * @see #stop()
     */
    public XatkitServer(Configuration configuration) {
        checkNotNull(configuration, "Cannot start the %s with the provided %s: %s", this.getClass().getSimpleName
                (), Configuration.class.getSimpleName(), configuration);
        Log.info("Creating {0}", this.getClass().getSimpleName());
        this.isStarted = false;
        String publicUrl = configuration.getString(XatkitServerUtils.SERVER_PUBLIC_URL_KEY,
                XatkitServerUtils.DEFAULT_SERVER_LOCATION);
        this.port = configuration.getInt(XatkitServerUtils.SERVER_PORT_KEY, XatkitServerUtils.DEFAULT_SERVER_PORT);
        this.baseURL = publicUrl + ":" + Integer.toString(this.port);
        Log.info("{0} started on {1}", this.getClass().getSimpleName(), this.getBaseURL());
        this.restEndpoints = new HashMap<>();
        this.contentDirectory = FileUtils.getFile(XatkitServerUtils.PUBLIC_DIRECTORY_NAME, configuration);
        this.contentDirectory.mkdirs();
        try {
            this.contentDirectoryPath =
                    Paths.get(contentDirectory.getAbsolutePath()).toRealPath(LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            throw new XatkitException("Cannot initialize the Xatkit server, see the attached exception", e);
        }
        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(15000)
                .setTcpNoDelay(true)
                .build();

        server = ServerBootstrap.bootstrap()
                .setListenerPort(port)
                .setServerInfo("Xatkit/1.1")
                .setSocketConfig(socketConfig)
                .setExceptionLogger(e -> {
                    if (e instanceof SocketTimeoutException) {
                        /*
                         * SocketTimeoutExceptions are thrown after each query, we can log them as debug to avoid
                         * polluting the application log.
                         */
                        Log.debug(e);
                    } else {
                        Log.error(e);
                    }
                })
                .registerHandler("/content*", new ContentHttpHandler(this))
                .registerHandler("/admin*", new AdminHttpHandler(configuration))
                .registerHandler("*", new HttpHandler(this))
                .create();
    }

    /**
     * Returns the port the server is listening to.
     *
     * @return the port the server is listening to
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Returns the URL of the {@link XatkitServer}.
     * <p>
     * This URL contains the base address and port ({@code <address:port>}), and can be used to access the
     * {@link XatkitServer} handlers.
     *
     * @return the URL of the {@link XatkitServer}
     */
    public String getBaseURL() {
        return this.baseURL;
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
     * Returns {@code true} if the {@link XatkitServer} is started, {@code false} otherwise.
     *
     * @return {@code true} if the {@link XatkitServer} is started, {@code false} otherwise
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
            throw new XatkitException(MessageFormat.format("Cannot start the {0}, the port {1} cannot be bound. This " +
                    "may happen if another bot is started on the same port, if a previously started bot was not shut " +
                    "down properly, or if another application is already using the port", this.getClass()
                    .getSimpleName(), port), e);
        } catch (IOException e) {
            throw new XatkitException(MessageFormat.format("Cannot start the {0}, see attached exception", this
                    .getClass().getSimpleName()), e);
        }
        isStarted = true;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown(5, TimeUnit.SECONDS);
            isStarted = false;
        }));
        Log.info("XatkitServer started, listening on {0}:{1}", server.getInetAddress().toString(), server
                .getLocalPort());
    }

    /**
     * Stops the underlying {@link HttpServer}.
     */
    public void stop() {
        Log.info("Stopping XatkitServer");
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
     * the {@link #notifyRestHandler(HttpMethod, String, List, List, Object, String)} method.
     * <p>
     * <b>Note</b>: the provided {@code uri} must start with a leading {@code /}.
     *
     * @param httpMethod the Http method of the REST endpoint
     * @param uri        the URI of the REST endpoint
     * @param handler    the {@link JsonRestHandler} to associate to the REST endpoint
     * @throws NullPointerException     if the provided {@code uri} or {@code handler} is {@code null}
     * @throws IllegalArgumentException if the provided {@code uri} does not start with a leading {@code /}
     */
    public void registerRestEndpoint(HttpMethod httpMethod, String uri, RestHandler handler) {
        checkNotNull(httpMethod, "Cannot register a REST endpoint for the provided %s %s",
                HttpMethod.class.getSimpleName(), httpMethod);
        checkNotNull(uri, "Cannot register a REST endpoint for the provided URI %s", uri);
        checkArgument(uri.startsWith("/"), "Cannot register a REST endpoint for the provided URI %s, the URI must " +
                "start with a \"/\"", uri);
        checkNotNull(handler, "Cannot register the provided %s %s (uri=%s)", JsonRestHandler.class.getSimpleName(),
                handler, uri);
        String normalizedUri = normalizeURI(uri);
        this.restEndpoints.put(EndpointEntry.of(httpMethod, normalizeURI(uri)), handler);
        Log.info("Registered REST handler {0} at URI {1} (method={2})", handler.getClass().getSimpleName(),
                normalizedUri, httpMethod.label);
    }

    /**
     * Returns whether the provided {@code httpMethod} on the given {@code uri} is associated to a REST endpoint.
     *
     * @param httpMethod the Http method of the REST endpoint to check
     * @param uri        the URI of the REST endpoint to check
     * @return {@code true} if there is a REST endpoint associated to the provided {@code uri}, {@code false} otherwise
     * @throws NullPointerException if the provided {@code uri} is {@code null}
     */
    public boolean isRestEndpoint(HttpMethod httpMethod, String uri) {
        checkNotNull(uri, "Cannot check if the provided URI (%s) corresponds to a REST endpoint, please provide a " +
                "non-null URI", uri);
        return this.restEndpoints.containsKey(EndpointEntry.of(httpMethod, normalizeURI(uri)));
    }

    /**
     * Returns the {@link RestHandler} associated to the provided {@code uri} with the given {@code httpMethod}.
     *
     * @param httpMethod the Http method to retrieve the handler for
     * @param uri        the URI to retrieve the handler for
     * @return the retrieved {@link RestHandler} if it exists, {@code null} otherwise
     */
    public @Nullable
    RestHandler getRegisteredRestHandler(HttpMethod httpMethod, String uri) {
        return this.restEndpoints.get(EndpointEntry.of(httpMethod, normalizeURI(uri)));
    }

    /**
     * Returns a {@link Collection} containing the registered {@link RestHandler}s.
     *
     * @return a {@link Collection} containing the registered {@link RestHandler}s
     */
    public Collection<RestHandler> getRegisteredRestHandlers() {
        return Collections.unmodifiableCollection(this.restEndpoints.values());
    }

    /**
     * Clears the registered {@link RestHandler}s.
     * <p>
     * This method only removes the {@code uri -> handler} bindings, and does not ensure the the {@link RestHandler}s
     * have been properly stopped.
     */
    public void clearRegisteredRestHandlers() {
        this.restEndpoints.clear();
    }

    /**
     * Notifies the REST endpoint associated with the provided {@code uri}.
     *
     * @param httpMethod the Http method of the REST endpoint to notify
     * @param uri        the URI of the REST endpoint to notify
     * @param headers    the HTTP {@link Header}s of the request sent to the endpoint
     * @param params     the HTTP parameters of the request sent to the endpoint
     * @param content    the {@link JsonElement} representing the content of the request sent to the endpoint
     * @return the {@link JsonElement} returned by the endpoint, or {@code null} if the endpoint does not return
     * anything
     * @throws NullPointerException if the provided {@code uri}, {@code header}, or {@code params} is {@code null}
     * @throws XatkitException      if there is no REST endpoint registered for the provided {@code uri}
     * @see #registerRestEndpoint(HttpMethod, String, RestHandler)
     */
    public Object notifyRestHandler(HttpMethod httpMethod, String uri, List<Header> headers, List<NameValuePair> params,
                                    @Nullable Object content, String contentType) {
        checkNotNull(uri, "Cannot notify the REST endpoint %s, please provide a non-null URI", uri);
        checkNotNull(headers, "Cannot notify the REST endpoint %s, the headers list is null", uri);
        checkNotNull(params, "Cannot notify the REST endpoint %s, the parameters list is null", uri);
        RestHandler handler = this.getRegisteredRestHandler(httpMethod, uri);
        if (isNull(handler)) {
            throw new XatkitException(MessageFormat.format("Cannot notify the REST endpoint {0}, there is no handler " +
                    "registered for this URI", uri));
        }
        /*
         * We can ignore the content type if we are dealing with a GET request, the HTTP/1.1 standard explicitly
         * state that body should be ignored for request methods that do not include defined semantics for an
         * entity-body (see https://tools.ietf.org/html/rfc2616#section-4.3).
         */
        if (httpMethod.equals(HttpMethod.GET) || handler.acceptContentType(contentType)) {
            return handler.handleContent(headers, params, content);
        } else {
            return null;
        }
    }

    /**
     * Register a {@link WebhookEventProvider}.
     * <p>
     * The registered {@code webhookEventProvider}'s handler will be notified when a new request is received. If the
     * provider's handler supports the request content type (see {@link RestHandler#acceptContentType(String)}, it will
     * receive the request content that will be used to create the associated {@link com.xatkit.intent.EventInstance}.
     *
     * @param webhookEventProvider the {@link WebhookEventProvider} to register
     * @throws NullPointerException if the provided {@code webhookEventProvider} is {@code null}
     * @see #notifyRestHandler (HttpMethod, String, List, List, Object, String)
     * @see RestHandler#acceptContentType(String)
     * @see RestHandler#handleContent(List, List, Object)
     */
    public void registerWebhookEventProvider(WebhookEventProvider webhookEventProvider) {
        checkNotNull(webhookEventProvider, "Cannot register the provided %s: %s", WebhookEventProvider.class
                .getSimpleName(), webhookEventProvider);
        this.registerRestEndpoint(webhookEventProvider.getEndpointMethod(), webhookEventProvider.getEndpointURI(),
                webhookEventProvider.getRestHandler());
    }

    /**
     * Unregistered a {@link WebhookEventProvider}.
     * <p>
     * The provided {@code webhookEventProvider} will not be notified when new request are received, and cannot be
     * used to create {@link com.xatkit.intent.EventInstance}s.
     *
     * @param webhookEventProvider the {@link WebhookEventProvider} to unregister
     * @throws NullPointerException if the provided {@code webhookEventProvider} is {@code null}
     */
    public void unregisterWebhookEventProvider(WebhookEventProvider webhookEventProvider) {
        checkNotNull(webhookEventProvider, "Cannot unregister the provided %s: %s", WebhookEventProvider.class
                .getSimpleName(), webhookEventProvider);

        this.restEndpoints.remove(webhookEventProvider.getEndpointURI());
    }

    /**
     * Normalizes the provided URI by removing its trailing {@code /}.
     * <p>
     * This method is used to uniformize the format of registered {@code URIs}, and ensure that {@code URIs}
     * containing a trailing {@code /} are matched to their equivalent without a trailing {@code /}.
     *
     * @param uri the {@code URI} to normalize
     * @return the normalized URI
     */
    private String normalizeURI(String uri) {
        if (uri.length() > 1 && uri.endsWith("/")) {
            return uri.substring(0, uri.length() - 1);
        } else {
            return uri;
        }
    }

    /**
     * Creates a publicly accessible file from the provided {@code session}, {@code path}, and {@code origin}.
     * <p>
     * The created file can be accessed through {@code <baseURL/content/sessionId/path>}, and contains the content of
     * the provided {@code origin} {@link File}.
     *
     * @param session the {@link XatkitSession} used to create the file
     * @param path    the path of the file to create
     * @param origin  the origin file to copy
     * @return the created {@link File}
     * @throws NullPointerException if the provided {@code session}, {@code path}, or {@code origin} is {@code null}
     * @throws XatkitException      if an error occurred when reading the provided {@code origin} {@link File}, or if
     *                              the existing file at the given {@code path} cannot be deleted or if the provided
     *                              {@code path} refers to an illegal location
     * @see #createOrReplacePublicFile(XatkitSession, String, String)
     */
    public File createOrReplacePublicFile(XatkitSession session, String path, File origin) {
        checkNotNull(origin, "Cannot create a public file from the provided origin file %s", origin);
        byte[] fileContent;
        try {
            fileContent = org.apache.commons.io.FileUtils.readFileToByteArray(origin);
        } catch (IOException e) {
            throw new XatkitException(e);
        }
        return this.createOrReplacePublicFile(session, path, fileContent);
    }

    /**
     * Creates a publicly accessible file from the provided {@code session}, {@code path}, and {@code content}.
     * <p>
     * The created file can be accessed through {@code <baseURL/content/sessionId/path}, and contains the provided
     * {@code content}.
     *
     * @param session the {@link XatkitSession} used to create the file
     * @param path    the the path of the file to create
     * @param content the content of the file to create
     * @return the created {@link File}
     * @throws NullPointerException if the provided {@code session} or {@code path} is {@code null}
     * @throws XatkitException      if an error occurred when computing the provided {@code path}, or if the existing
     *                              file at the given {@code path} cannot be deleted, or if the provided {@code path}
     *                              refers to an illegal location
     */
    public File createOrReplacePublicFile(XatkitSession session, String path, String content) {
        checkNotNull(session, "Cannot create a public file from the provided %s %s",
                XatkitSession.class.getSimpleName(), session);
        checkNotNull(path, "Cannot create a public file with the provided path %s", path);
        return this.createOrReplacePublicFile(session, path, content.getBytes());
    }

    public File createOrReplacePublicFile(XatkitSession session, String path, byte[] content) {
        File sessionFile = getOrCreateSessionFile(session);

        File file = new File(sessionFile, path);

        Path filePath = Paths.get(file.getAbsolutePath());
        try {
            filePath = filePath.toRealPath(LinkOption.NOFOLLOW_LINKS);
        } catch (NoSuchFileException e) {
            /*
             * We don't want to check if the file exists yet, we are only interested by the real path (without the
             * potential '../..' in it). This path is the content of the message of the NoSuchFileException.
             */
            filePath = Paths.get(e.getMessage());
        } catch (IOException e) {
            throw new XatkitException(e);
        }

        if (!filePath.startsWith(this.contentDirectoryPath)) {
            throw new XatkitException(MessageFormat.format("Cannot create a public file at the given location {0}, " +
                            "forbidden access: the file is not contained in the public directory {1}", filePath,
                    this.contentDirectoryPath));
        }

        /*
         * We don't know if the file exists yet, because of the trick done when capturing the NoSuchFileException. We
         * need to check it before proceeding.
         */
        if (file.exists()) {
            boolean deleted = file.delete();
            if (!deleted) {
                throw new XatkitException(MessageFormat.format("Cannot delete the existing file {0}",
                        file.getAbsolutePath()));
            }
        }

        try {
            org.apache.commons.io.FileUtils.writeByteArrayToFile(file, content);
        } catch (IOException e) {
            throw new XatkitException(e);
        }
        return file;
    }

    /**
     * Retrieves the public URL associated to the provided {@code file} if it exists.
     * <p>
     * This method is typically called to retrieve the public URL of a file created with
     * {@link #createOrReplacePublicFile(XatkitSession, String, String)}.
     *
     * @param file the {@link File} to retrieve the public URL for
     * @return the retrieved URL if it exists, {@code null} otherwise
     * @throws NullPointerException if the provided {@code file} is {@code null}
     * @throws XatkitException      if the provided {@code file} corresponds to an illegal location
     */
    public @Nullable
    String getPublicURL(File file) {
        checkNotNull(file, "Cannot retrieve the public URL of the provided file %s", file);
        Path filePath;
        try {
            filePath = Paths.get(file.getAbsolutePath()).toRealPath(LinkOption.NOFOLLOW_LINKS);
        } catch (NoSuchFileException e) {
            Log.info("Cannot retrieve the public URL for the provided file {0}, the file does not exist",
                    file.getAbsolutePath());
            return null;
        } catch (IOException e) {
            throw new XatkitException(e);
        }
        if (filePath.startsWith(this.contentDirectoryPath)) {
            Path relativePath = this.contentDirectoryPath.relativize(filePath);
            String relativePathFragment = relativePath.toString();
            if (File.separator.equals("\\")) {
                /*
                 * Fix Path.toString() for windows.
                 */
                relativePathFragment = relativePathFragment.replaceAll("\\\\", "/");
            }
            return this.baseURL + XatkitServerUtils.PUBLIC_CONTENT_URL_FRAGMENT + relativePathFragment;
        } else {
            throw new XatkitException(MessageFormat.format("Cannot retrieve the public URL for the file {0}, " +
                            "forbidden access: the file is not contained in the public directory {1}", filePath,
                    this.contentDirectoryPath));
        }
    }

    /**
     * Retrieves the public {@link File} associated to the provided {@code session} and {@code filePath}.
     *
     * @param session  the {@link XatkitSession} to retrieve the {@link File} from
     * @param filePath the path of the {@link File} to retrieve
     * @return the retrieved {@link File} if it exists, {@code null} otherwise
     * @throws NullPointerException if the provided {@code session} if {@code filePath} is {@code null}
     * @throws XatkitException      if the provided {@code filePath} refers to an illegal location
     * @see #getPublicFile(String)
     */
    public @Nullable
    File getPublicFile(XatkitSession session, String filePath) {
        checkNotNull(session, "Cannot retrieve the public file from the provided %s %s",
                XatkitSession.class.getSimpleName(), session);
        checkNotNull(filePath, "Cannot retrieve the provided file %s", filePath);
        return getPublicFile(session.getSessionId() + "/" + filePath);
    }

    /**
     * Retrieves the public {@link File} associated to the provided {@code filePath}.
     * <p>
     * This method expects a full relative path under the {@code /content/} location. This means that {@link File}
     * associated to {@link XatkitSession}s must be retrieved with the following path: {@code sessionId/path}. Check
     * {@link #getPublicFile(XatkitSession, String)} to easily retrieve a {@link File} associated to a
     * {@link XatkitSession}.
     *
     * @param filePath the path of the {@link File} to retrieve
     * @return the retrieved {@link File} if it exists, {@code null} otherwise
     * @throws NullPointerException if the provided {@code filePath} is {@code null}
     * @throws XatkitException      if the provided {@code filePath} refers to an illegal location
     * @see #getPublicFile(XatkitSession, String)
     */
    public @Nullable
    File getPublicFile(String filePath) {
        checkNotNull(filePath, "Cannot retrieve the public file from the provided path %s", filePath);
        Path p = Paths.get(contentDirectory.getAbsolutePath(), filePath);
        try {
            p = p.toRealPath(LinkOption.NOFOLLOW_LINKS);
        } catch (NoSuchFileException e) {
            Log.warn("The public file {0} does not exist", p);
            return null;
        } catch (IOException e) {
            throw new XatkitException(e);
        }
        if (p.startsWith(this.contentDirectoryPath)) {
            /*
             * The file exists, it is checked when calling toRealPath().
             */
            return p.toFile();
        } else {
            throw new XatkitException(MessageFormat.format("Forbidden access to file {0}, the file is not contained " +
                    "in the public directory {1}", p, this.contentDirectoryPath));
        }
    }

    /**
     * Retrieves or create the public {@link File} associated to the provided {@code session}.
     *
     * @param session the {@link XatkitSession} to retrieve or create a {@link File} from
     * @return the {@link File}
     * @see #getSessionFile(XatkitSession)
     */
    private @Nonnull
    File getOrCreateSessionFile(XatkitSession session) {
        File sessionFile = this.getSessionFile(session);
        if (!sessionFile.exists()) {
            sessionFile.mkdirs();
        }
        return sessionFile;
    }

    /**
     * Retrieves the {@link File} associated to the provided {@code session}.
     * <p>
     * This method always returns a {@link File} instance, that needs to be checked with {@link File#exists()} to
     * know if the session file exists or not.
     *
     * @param session the {@link XatkitSession} to retrieve the {@link File} from
     * @return the retrieved {@link File}
     * @see #getOrCreateSessionFile(XatkitSession)
     */
    private @Nonnull
    File getSessionFile(XatkitSession session) {
        File sessionFile = new File(this.contentDirectory, session.getSessionId());
        return sessionFile;
    }

    /**
     * Uniquely identifies a registered REST endpoint.
     */
    private static class EndpointEntry {

        /**
         * Creates an {@link EndpointEntry} from the provided {@code httpMethod} and {@code uri}.
         *
         * @param httpMethod the Http method of the entry
         * @param uri        the URI of the entry
         * @return the created {@link EndpointEntry}
         */
        private static EndpointEntry of(HttpMethod httpMethod, String uri) {
            return new EndpointEntry(httpMethod, uri);
        }

        /**
         * The {@link HttpMethod} of the entry.
         */
        private HttpMethod httpMethod;

        /**
         * The URI of the entry.
         */
        private String uri;

        /**
         * Constructs an {@link EndpointEntry} with the provided {@code httpMethod} and {@code uri}.
         * @param httpMethod the Http method of the entry
         * @param uri the URI of the entry
         */
        private EndpointEntry(HttpMethod httpMethod, String uri) {
            this.httpMethod = httpMethod;
            this.uri = uri;
        }

        @Override
        public int hashCode() {
            return this.httpMethod.hashCode() + this.uri.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof EndpointEntry) {
                EndpointEntry other = (EndpointEntry) obj;
                return other.httpMethod.equals(this.httpMethod) && other.uri.equals(this.uri);
            } else {
                return super.equals(obj);
            }
        }
    }

}
