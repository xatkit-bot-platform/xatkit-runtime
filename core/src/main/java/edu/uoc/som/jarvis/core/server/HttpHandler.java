package edu.uoc.som.jarvis.core.server;

import com.google.gson.*;
import edu.uoc.som.jarvis.core.JarvisException;
import edu.uoc.som.jarvis.core.platform.io.WebhookEventProvider;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.http.*;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;

/**
 * Handles the input requests and notifies the {@link WebhookEventProvider}s.
 */
class HttpHandler implements HttpRequestHandler {

    /**
     * The HTTP header used to specify the CORS attribute.
     * <p>
     * This header is not defined in the {@link HttpHeaders} class.
     */
    private static String CORS_HEADER = "Access-Control-Allow-Origin";

    /**
     * The value of the CORS HTTP header.
     * <p>
     * The CORS attribute accept all queries by default, this is required to allow in-browser calls to the Jarvis
     * server.
     */
    private static String CORS_VALUE = "*";

    /**
     * The {@link JarvisServer} managing this handler.
     * <p>
     * The {@link JarvisServer} is used to notify the {@link WebhookEventProvider}s when a new
     * request is received.
     *
     * @see JarvisServer#notifyWebhookEventProviders(String, Object, Header[])
     */
    private JarvisServer jarvisServer;

    /**
     * The {@link JsonParser} used to pretty print {@code application/json} contents and provide readable debug logs.
     */
    private JsonParser parser;

    /**
     * The {@link Gson} instance used to pretty print {@code application/json} contents and provide readable debut logs.
     */
    private Gson gson;

    /**
     * Constructs a new {@link HttpHandler} managed by the given {@code jarvisServer}.
     *
     * @param jarvisServer the {@link JarvisServer} managing this handler
     * @throws NullPointerException if the provided {@code jarvisServer} is {@code null}
     */
    public HttpHandler(JarvisServer jarvisServer) {
        super();
        checkNotNull(jarvisServer, "Cannot construct a %s with the provided %s: %s", HttpHandler.class.getSimpleName
                (), JarvisServer.class.getSimpleName(), jarvisServer);
        this.jarvisServer = jarvisServer;
        this.parser = new JsonParser();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Handles the received {@code request} and fill the provided {@code response}.
     * <p>
     * This method parses the received {@code request} headers and content and notifies the {@link JarvisServer}'s
     * registered {@link WebhookEventProvider}s.
     *
     * @param request  the received {@link HttpRequest}
     * @param response the {@link HttpResponse} to send to the caller
     * @param context  the {@link HttpContext} associated to the received {@link HttpRequest}
     * @see JarvisServer#notifyWebhookEventProviders(String, Object, Header[])
     */
    public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context) {

        String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
        String target = request.getRequestLine().getUri();

        Log.info("Received a {0} query on {1}", method, target);

        List<NameValuePair> parameters = null;
        try {
            parameters = URLEncodedUtils.parse(new URI(target), HTTP.UTF_8);
        } catch (URISyntaxException e) {
            String errorMessage = MessageFormat.format("Cannot parse the requested URI {0}", target);
            throw new JarvisException(errorMessage);
        }

        for (NameValuePair parameter : parameters) {
            Log.info("Query parameter: {0} = {1}", parameter.getName(), parameter.getValue());
        }

        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
            Header[] headers = request.getAllHeaders();
            Log.info("Request Headers");
            logHeaders(headers);
            String contentEncoding = null;
            Header encodingHeader = entity.getContentEncoding();
            if (nonNull(encodingHeader) && encodingHeader.getElements().length > 0) {
                contentEncoding = encodingHeader.getElements()[0].getName();
                Log.info("Query content encoding: {0}", contentEncoding);
            } else {
                Log.warn("Unknown query content encoding");
            }
            String contentType = null;
            Header contentTypeHeader = entity.getContentType();
            if (nonNull(contentTypeHeader) && contentTypeHeader.getElements().length > 0) {
                contentType = contentTypeHeader.getElements()[0].getName();
                Log.info("Query content type: {0}", contentType);
            } else {
                Log.warn("Unknown query content type");
            }
            Long contentLength = entity.getContentLength();
            Log.info("Query content length: {0}", contentLength);

            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
                StringBuilder contentBuilder = new StringBuilder();
                while (reader.ready()) {
                    contentBuilder.append(reader.readLine());
                }
                String content = contentBuilder.toString();
                if (content.isEmpty()) {
                    Log.warn("Empty query content");
                } else {
                    if (ContentType.APPLICATION_JSON.getMimeType().equals(contentType)) {
                        Log.info("Parsing {0} content", ContentType.APPLICATION_JSON.getMimeType());
                        try {
                            JsonElement jsonElement = parser.parse(content);
                            Log.info("Query content: \n {0}", gson.toJson(jsonElement));
                        } catch (JsonSyntaxException e) {
                            Log.error(e, "Cannot parse the {0} content {1}", ContentType.APPLICATION_JSON.getMimeType
                                    (), content);
                        }
                    } else {
                        Log.info("No parser for the provided content type {0}, returning the raw content: \n {1}",
                                contentType, content);
                    }
                    this.jarvisServer.notifyWebhookEventProviders(contentType, content, headers);
                }
            } catch (IOException e) {
                throw new JarvisException("An error occurred when handling the request content", e);
            }
        }
        response.setHeader(CORS_HEADER, CORS_VALUE);
        response.setStatusCode(HttpStatus.SC_OK);
    }

    /**
     * An utility method that logs the names and values of the provided {@link Header} array.
     *
     * @param headers the array containing the {@link Header}s to log
     */
    private void logHeaders(Header[] headers) {
        for (int i = 0; i < headers.length; i++) {
            Log.info("{0} : {1}", headers[i].getName(), headers[i].getValue());
        }
    }

}
