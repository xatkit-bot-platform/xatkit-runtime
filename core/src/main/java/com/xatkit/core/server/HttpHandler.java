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
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

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
            path = HttpUtils.getPath(request);
            parameters = HttpUtils.getParameters(request);
        } catch (URISyntaxException e) {
            throw new XatkitException(MessageFormat.format("Cannot parse the provided URI {0}, see attached exception",
                    request.getRequestLine().getUri()), e);
        }

        Log.debug("Received a {0} query on {1}", method, path);

        parameters.forEach(p -> Log.debug("Query parameter: {0}={1}", p.getName(), p.getValue()));

        List<Header> headers = Arrays.asList(request.getAllHeaders());
        headers.forEach(h -> Log.debug("Header {0}={1}", h.getName(), h.getValue()));

        Object content = null;
        String contentType = null;

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
                Log.debug("Query content encoding: {0}", contentEncoding);
            } else {
                Log.warn("Unknown query content encoding");
            }
            Header contentTypeHeader = entity.getContentType();
            if (nonNull(contentTypeHeader) && contentTypeHeader.getElements().length > 0) {
                contentType = contentTypeHeader.getElements()[0].getName();
                Log.debug("Query content type: {0}", contentType);
            } else {
                Log.warn("Unknown query content type");
            }
            Long contentLength = entity.getContentLength();
            Log.debug("Query content length: {0}", contentLength);

            content = HttpEntityHelper.getStringFromHttpEntity(entity);
        }

        HttpMethod httpMethod = HttpMethod.valueOf(method);
        if (this.xatkitServer.isRestEndpoint(httpMethod, path)) {
            Object result = null;
            try {
                result = xatkitServer.notifyRestHandler(httpMethod, path, headers, parameters,
                        content, contentType);
            } catch(RestHandlerException e) {
                Log.error(e, "An error occurred when notifying the Rest handler, see attached exception");
                response.setStatusCode(HttpStatus.SC_NOT_FOUND);
                /*
                 * We can return here, no need to process the returned value.
                 */
                return;
            }
            if (nonNull(result)) {
                if(result instanceof HttpEntity) {
                    /*
                     * Handle RestHandlers that directly return a HttpEntity. This is for example the case for
                     * RestHandlers that return plain HTML, JS, or CSS.
                     */
                    response.setEntity((HttpEntity) result);
                } else {
                    /*
                     * Otherwise try to create an entity from the returned result. This is for example how
                     * JsonElements are handled.
                     */
                    HttpEntity resultEntity = HttpEntityHelper.createHttpEntity(result);
                    response.setEntity(resultEntity);
                }
            } else {
                Log.warn("Cannot embed the handler's result {0}", result);
            }
            response.setHeader("Access-Control-Allow-Headers", "content-type");
            response.setStatusCode(HttpStatus.SC_OK);
        } else {
            /*
             * There is no handler for this URI.
             */
            Log.error("No endpoint registered for {0}: {1}", method, path);
            response.setStatusCode(HttpStatus.SC_NOT_FOUND);
            return;
        }


        response.setHeader(CORS_HEADER, CORS_VALUE);
        for (RestHandler handler : xatkitServer.getRegisteredRestHandlers()) {
            accessControlAllowHeaders.addAll(handler.getAccessControlAllowHeaders());
        }
        String stringAccessControlAllowHeaders = String.join(",", accessControlAllowHeaders);
        response.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, stringAccessControlAllowHeaders);
        /*
         * TODO check if setting the status code is required here.
         */
        response.setStatusCode(HttpStatus.SC_OK);
    }
}
