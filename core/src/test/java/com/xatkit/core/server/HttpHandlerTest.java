package com.xatkit.core.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.xatkit.AbstractXatkitTest;
import com.xatkit.stubs.StubXatkitServer;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpHandlerTest extends AbstractXatkitTest {

    private static StubXatkitServer xatkitServer;

    private static String NOT_REGISTERED_URI = "/test-invalid";

    private static HttpEntityEnclosingRequest NOT_REGISTERED_POST_REQUEST;

    private static String REGISTERED_POST_URI = "/test";

    private static HttpEntityEnclosingRequest REGISTERED_POST_REQUEST;

    private static JsonObject REGISTERED_POST_OBJECT;

    private static String REGISTERED_POST_OBJECT_STRING;


    private static String REGISTERED_GET_URI = "/test-get";

    private static HttpRequest REGISTERED_GET_REQUEST;

    private static HttpRequest REGISTERED_GET_REQUEST_WITH_PARAMETERS;

    private HttpHandler handler;

    private HttpResponse response;

    private HttpContext context;

    @BeforeClass
    public static void setUpBeforeClass() {
        xatkitServer = new StubXatkitServer();
        xatkitServer.registerRestEndpoint(HttpMethod.POST, REGISTERED_POST_URI,
                RestHandlerFactory.createJsonRestHandler((headers, params, content) -> {
                    Log.info("Test REST POST handler called");
                    return null;
                }));
        xatkitServer.registerRestEndpoint(HttpMethod.GET, REGISTERED_GET_URI, RestHandlerFactory.createJsonRestHandler(
                (headers, params, content) -> {
                    Log.info("Test REST GET handler called");
                    return null;
                }
        ));
        NOT_REGISTERED_POST_REQUEST = new BasicHttpEntityEnclosingRequest("POST", NOT_REGISTERED_URI);
        JsonObject notRegisteredPostRequestObject = new JsonObject();
        notRegisteredPostRequestObject.addProperty("key", "value");
        NOT_REGISTERED_POST_REQUEST.setEntity(HttpEntityHelper.createHttpEntity(notRegisteredPostRequestObject));

        REGISTERED_POST_REQUEST = new BasicHttpEntityEnclosingRequest("POST", REGISTERED_POST_URI);
        REGISTERED_POST_OBJECT = new JsonObject();
        REGISTERED_POST_OBJECT.addProperty("key", "value");
        REGISTERED_POST_REQUEST.setEntity(HttpEntityHelper.createHttpEntity(REGISTERED_POST_OBJECT));
        REGISTERED_POST_OBJECT_STRING = new Gson().toJson(REGISTERED_POST_OBJECT);

        REGISTERED_GET_REQUEST = new BasicHttpRequest("GET", REGISTERED_GET_URI);

        REGISTERED_GET_REQUEST_WITH_PARAMETERS = new BasicHttpRequest("GET", REGISTERED_GET_URI + "?param=value");
    }

    @AfterClass
    public static void tearDownAfterClass() {
        if (xatkitServer.isStarted()) {
            /*
             * Shouldn't be the case, but close it just in case.
             */
            Log.warn("The {0} has been started by a test from {1}", XatkitServer.class.getSimpleName(),
                    HttpHandler.class.getSimpleName());
            xatkitServer.stop();
        }
    }

    @Before
    public void setUp() {
        xatkitServer.clean();
        this.context = new BasicHttpContext();
        this.response = DefaultHttpResponseFactory.INSTANCE.newHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK,
                this.context);
    }

    @Test(expected = NullPointerException.class)
    public void constructNullXatkitServer() {
        this.handler = new HttpHandler(null);
    }

    @Test
    public void constructValidXatkitServer() {
        this.handler = new HttpHandler(xatkitServer);
    }

    @Test
    public void handleUnregisteredHandler() {
        this.handler = getHandler();
        handler.handle(NOT_REGISTERED_POST_REQUEST, response, context);
        assertThat(xatkitServer.getLastIsRestEndpointURI()).as("Valid isRestEndpoint URI").isEqualTo(NOT_REGISTERED_URI);
        assertThat(xatkitServer.getLastNotifyRestHandlerURI()).as("NotifyRestHandler not called").isNull();
    }

    @Test
    public void handlePostRequestRegisteredHandler() {
        this.handler = getHandler();
        handler.handle(REGISTERED_POST_REQUEST, response, context);
        assertIsRestEndpointCallMatches(HttpMethod.POST, REGISTERED_POST_URI);
        assertNotifyRestHandlerCallMatches(HttpMethod.POST,
                REGISTERED_POST_URI,
                Arrays.asList(REGISTERED_POST_REQUEST.getAllHeaders()),
                Collections.emptyList(),
                ContentType.APPLICATION_JSON.getMimeType(),
                REGISTERED_POST_OBJECT_STRING);
    }

    @Test
    public void handleGetRequestRegisteredHandler() {
        this.handler = getHandler();
        handler.handle(REGISTERED_GET_REQUEST, response, context);
        assertIsRestEndpointCallMatches(HttpMethod.GET, REGISTERED_GET_URI);
        assertNotifyRestHandlerCallMatches(HttpMethod.GET,
                REGISTERED_GET_URI,
                Arrays.asList(REGISTERED_GET_REQUEST.getAllHeaders()),
                Collections.emptyList(),
                null,
                null);
    }

    @Test
    public void handleGetRequestWithParametersRegisteredHandler() throws URISyntaxException {
        this.handler = getHandler();
        handler.handle(REGISTERED_GET_REQUEST_WITH_PARAMETERS, response, context);
        assertIsRestEndpointCallMatches(HttpMethod.GET, REGISTERED_GET_URI);
        assertNotifyRestHandlerCallMatches(HttpMethod.GET,
                REGISTERED_GET_URI,
                Arrays.asList(REGISTERED_GET_REQUEST_WITH_PARAMETERS.getAllHeaders()),
                URLEncodedUtils.parse(new URI(REGISTERED_GET_REQUEST_WITH_PARAMETERS.getRequestLine().getUri()), StandardCharsets.UTF_8.name()),
                null,
                null);
    }

    private HttpHandler getHandler() {
        return new HttpHandler(xatkitServer);
    }

    private void assertIsRestEndpointCallMatches(HttpMethod expectedMethod, String expectedUri) {
        assertThat(xatkitServer.getLastIsRestEndpointMethod()).as("Valid isRestEndpoint method").isEqualTo(expectedMethod);
        assertThat(xatkitServer.getLastIsRestEndpointURI()).as("Valid isRestEndpoint URI").isEqualTo(expectedUri);
    }

    private void assertNotifyRestHandlerCallMatches(@Nonnull HttpMethod expectedMethod,
                                                    @Nonnull String expectedURI,
                                                    @Nonnull List<Header> expectedHeaders,
                                                    @Nonnull List<NameValuePair> expectedParameters,
                                                    @Nullable String expectedContentType,
                                                    @Nullable Object expectedContent) {
        assertThat(xatkitServer.getLastNotifyRestHandlerMethod()).as("Valid notifyRestHandler method").isEqualTo(expectedMethod);
        assertThat(xatkitServer.getLastNotifyRestHandlerURI()).as("Valid notifyRestHandler URI").isEqualTo(expectedURI);
        assertThat(xatkitServer.getLastNotifyRestHandlerHeaders()).as("Valid notifyRestHandler headers").isEqualTo(expectedHeaders);
        assertThat(xatkitServer.getLastNotifyRestHandlerParams()).as("Valid notifyRestHandler parameters").isEqualTo(expectedParameters);
        assertThat(xatkitServer.getLastNotifyRestHandlerContentType()).as("Valid notifyRestHandler content type").isEqualTo(expectedContentType);
        assertThat(xatkitServer.getLastNotifyRestHandlerContent()).as("Valid notifyRestHandler content").isEqualTo(expectedContent);
    }
}
