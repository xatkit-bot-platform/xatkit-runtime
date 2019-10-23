package com.xatkit.core.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.xatkit.AbstractXatkitTest;
import com.xatkit.stubs.StubXatkitServer;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpHandlerTest extends AbstractXatkitTest {

    private static StubXatkitServer xatkitServer;

    private static String NOT_REGISTERED_URI = "/test-invalid";

    private static HttpEntityEnclosingRequest NOT_REGISTERED_POST_REQUEST;

    private static String REGISTERED_URI = "/test";

    private static HttpEntityEnclosingRequest REGISTERED_POST_REQUEST;

    private static JsonObject REGISTERED_POST_OBJECT;

    private static String REGISTERED_POST_OBJECT_STRING;

    private HttpHandler handler;

    private HttpResponse response;

    private HttpContext context;

    @BeforeClass
    public static void setUpBeforeClass() {
        xatkitServer = new StubXatkitServer();
        xatkitServer.registerRestEndpoint("/test", RestHandlerFactory.createJsonRestHandler(
                (headers, params, content) -> {
                    Log.info("Test rest handler called");
                    return null;
                }));
        NOT_REGISTERED_POST_REQUEST = new BasicHttpEntityEnclosingRequest("POST", NOT_REGISTERED_URI);
        JsonObject notRegisteredPostRequestObject = new JsonObject();
        notRegisteredPostRequestObject.addProperty("key", "value");
        NOT_REGISTERED_POST_REQUEST.setEntity(HttpEntityHelper.createHttpEntity(notRegisteredPostRequestObject));

        REGISTERED_POST_REQUEST = new BasicHttpEntityEnclosingRequest("POST", REGISTERED_URI);
        REGISTERED_POST_OBJECT = new JsonObject();
        REGISTERED_POST_OBJECT.addProperty("key", "value");
        REGISTERED_POST_REQUEST.setEntity(HttpEntityHelper.createHttpEntity(REGISTERED_POST_OBJECT));
        REGISTERED_POST_OBJECT_STRING = new Gson().toJson(REGISTERED_POST_OBJECT);
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
    public void testHandleUnregisteredHandler() {
        this.handler = getHandler();
        handler.handle(NOT_REGISTERED_POST_REQUEST, response, context);
        assertThat(xatkitServer.getLastIsRestEndpointURI()).as("Valid isRestEndpoint URI").isEqualTo(NOT_REGISTERED_URI);
        assertThat(xatkitServer.getLastNotifyRestHandlerURI()).as("NotifyRestHandler not called").isNull();
    }

    @Test
    public void testRegisteredHandler() {
        this.handler = getHandler();
        handler.handle(REGISTERED_POST_REQUEST, response, context);
        assertThat(xatkitServer.getLastIsRestEndpointURI()).as("Valid isRestEndpoint URI").isEqualTo(REGISTERED_URI);
        assertThat(xatkitServer.getLastNotifyRestHandlerURI()).as("NotifyRestHandler called").isEqualTo(REGISTERED_URI);
        assertThat(xatkitServer.getLastNotifyRestHandlerHeaders()).as("Valid headers").isEqualTo(Arrays.asList(REGISTERED_POST_REQUEST.getAllHeaders()));
        assertThat(xatkitServer.getLastNotifyRestHandlerParams()).as("Valid params").isEmpty();
        assertThat(xatkitServer.getLastNotifyRestHandlerContentType()).as("Valid content type").isEqualTo(ContentType.APPLICATION_JSON.getMimeType());
        assertThat(xatkitServer.getLastNotifyRestHandlerContent()).as("Valid content").isEqualTo(REGISTERED_POST_OBJECT_STRING);
    }

    private HttpHandler getHandler() {
        return new HttpHandler(xatkitServer);
    }
}
