package com.xatkit.core.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.xatkit.core.XatkitException;
import com.xatkit.core.platform.io.WebhookEventProvider;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
     * The CORS attribute accept all queries by default, this is required to allow in-browser calls to the Xatkit
     * server.
     */
    private static String CORS_VALUE = "*";

    /**
     * The HTTP header used to specify allowed headers.
     * <p>
     * This header is set to allow cross-origin calls from applications using the browser. It is initialized with the
     * {@link RestHandler#getAccessControlAllowHeaders()} method.
     */
    private static String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

    /**
     * The {@link XatkitServer} managing this handler.
     * <p>
     * The {@link XatkitServer} is used to notify the {@link WebhookEventProvider}s when a new
     * request is received.
     *
     * @see XatkitServer#notifyRestHandler(HttpMethod, String, List, List, Object, String)
     */
    private XatkitServer xatkitServer;

    /**
     * The {@link JsonParser} used to pretty print {@code application/json} contents and provide readable debug logs.
     */
    private JsonParser parser;

    /**
     * The {@link Gson} instance used to pretty print {@code application/json} contents and provide readable debut logs.
     */
    private Gson gsonPrinter;

    /**
     * Constructs a new {@link HttpHandler} managed by the given {@code xatkitServer}.
     *
     * @param xatkitServer the {@link XatkitServer} managing this handler
     * @throws NullPointerException if the provided {@code xatkitServer} is {@code null}
     */
    public HttpHandler(XatkitServer xatkitServer) {
        super();
        checkNotNull(xatkitServer, "Cannot construct a %s with the provided %s: %s", HttpHandler.class.getSimpleName
                (), XatkitServer.class.getSimpleName(), xatkitServer);
        this.xatkitServer = xatkitServer;
        this.parser = new JsonParser();
        this.gsonPrinter = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Handles the received {@code request} and fill the provided {@code response}.
     * <p>
     * This method parses the received {@code request} headers and content and notifies the {@link XatkitServer}'s
     * registered {@link WebhookEventProvider}s.
     *
     * @param request  the received {@link HttpRequest}
     * @param response the {@link HttpResponse} to send to the caller
     * @param context  the {@link HttpContext} associated to the received {@link HttpRequest}
     * @see XatkitServer#notifyRestHandler(HttpMethod, String, List, List, Object, String)
     */
    public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context) {

        String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
        List<NameValuePair> parameters = null;
        String path;
        try {
            URIBuilder uriBuilder = new URIBuilder(request.getRequestLine().getUri());
            path = uriBuilder.getPath();
            parameters = uriBuilder.getQueryParams();
        } catch (URISyntaxException e) {
            throw new XatkitException(MessageFormat.format("Cannot parse the provided URI {0}, see attached exception",
                    request.getRequestLine().getUri()), e);
        }

        Log.info("Received a {0} query on {1}", method, path);

        parameters.forEach(p -> Log.info("Query parameter: {0}={1}", p.getName(), p.getValue()));

        List<Header> headers = Arrays.asList(request.getAllHeaders());
        headers.forEach(h -> Log.info("Header {0}={1}", h.getName(), h.getValue()));

        Object content = null;
        String contentType = null;

        Log.info("Request type {0}", request.getClass().getSimpleName());

        /*
         * The access control headers that will be returned in the response.
         */
        Set<String> accessControlAllowHeaders = new HashSet<>();
        accessControlAllowHeaders.add("content-type");


        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
            String contentEncoding = null;
            Header encodingHeader = entity.getContentEncoding();
            if (nonNull(encodingHeader) && encodingHeader.getElements().length > 0) {
                contentEncoding = encodingHeader.getElements()[0].getName();
                Log.info("Query content encoding: {0}", contentEncoding);
            } else {
                Log.warn("Unknown query content encoding");
            }
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
                String currentLine;
                while (nonNull(currentLine = reader.readLine())) {
                    contentBuilder.append(currentLine);
                }
                content = contentBuilder.toString();
//                if (stringContent.isEmpty()) {
//                    Log.warn("Empty query content");
//                } else {
//                    // TODO check this, it shouldn't exist
//                    if (ContentType.APPLICATION_JSON.getMimeType().equals(contentType)) {
//                        Log.info("Parsing {0} content", ContentType.APPLICATION_JSON.getMimeType());
//                        try {
//                            JsonElement jsonElement = parser.parse(stringContent);
//                            Log.info("Query content: \n {0}", gsonPrinter.toJson(jsonElement));
//                        } catch (JsonSyntaxException e) {
//                            Log.error(e, "Cannot parse the {0} content {1}", ContentType.APPLICATION_JSON.getMimeType
//                                    (), stringContent);
//                        }
//                    } else {
//                        Log.info("No parser for the provided content type {0}, returning the raw content: \n {1}",
//                                contentType, content);
//                    }
//                }


            } catch (IOException e) {
                throw new XatkitException("An error occurred when handling the request content", e);
            }
        }

        HttpMethod httpMethod = HttpMethod.valueOf(method);
        if (this.xatkitServer.isRestEndpoint(httpMethod, path)) {
            Object result = xatkitServer.notifyRestHandler(httpMethod, path, headers, parameters,
                    content, contentType);
            if (nonNull(result)) {
                HttpEntity resultEntity = HttpEntityHelper.createHttpEntity(result);
                response.setEntity(resultEntity);
            } else {
                Log.warn("Cannot embed the provided json element {0}", result);
            }
            response.setHeader("Access-Control-Allow-Headers", "content-type");
        }


        response.setHeader(CORS_HEADER, CORS_VALUE);
        for (RestHandler handler : xatkitServer.getRegisteredRestHandlers()) {
            accessControlAllowHeaders.addAll(handler.getAccessControlAllowHeaders());
        }
        String stringAccessControlAllowHeaders = String.join(",", accessControlAllowHeaders);
        response.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, stringAccessControlAllowHeaders);
        response.setStatusCode(HttpStatus.SC_OK);
    }
}
