package com.xatkit.core.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.XatkitException;
import com.xatkit.stubs.EmptyRuntimePlatform;
import com.xatkit.stubs.StubXatkitCore;
import com.xatkit.stubs.io.StubJsonWebhookEventProvider;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.http.entity.ContentType;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class XatkitServerTest extends AbstractXatkitTest {

    /**
     * A valid {@link JsonRestHandler} used to test handler registration.
     * <p>
     * This handler returns a {@link JsonObject} with a field {@code called} set to {@code true}.
     */
    private static JsonRestHandler VALID_REST_HANDLER;

    /**
     * The test URI used to register {@link JsonRestHandler}s.
     */
    private static String VALID_REST_URI = "/test";

    private XatkitServer server;

    /**
     * A second server used to check multiple starts on the same port.
     * <p>
     * This variable is defined as a class attribute to allow to {@link XatkitServer#stop()} the server if an exception
     * occurred during the test cases it is involved in.
     */
    private XatkitServer server2;

    private StubXatkitCore stubXatkitCore;

    @BeforeClass
    public static void setUpBeforeClass() {
        VALID_REST_HANDLER = RestHandlerFactory.createJsonRestHandler((headers, params, content) -> {
            JsonObject result = new JsonObject();
            result.add("called", new JsonPrimitive(true));
            return result;
        });
    }

    @After
    public void tearDown() {
        if (nonNull(server) && server.isStarted()) {
            server.stop();
        }
        if (nonNull(stubXatkitCore)) {
            stubXatkitCore.shutdown();
        }
        if (nonNull(server2) && server2.isStarted()) {
            server2.stop();
        }
    }

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        this.server = new XatkitServer(null);
    }

    @Test
    public void constructEmptyConfiguration() {
        this.server = new XatkitServer(new BaseConfiguration());
        checkXatkitServer(server);
    }

    @Test
    public void constructConfigurationWithPort() {
        Configuration configuration = new BaseConfiguration();
        configuration.setProperty(XatkitServer.SERVER_PORT_KEY, 1234);
        this.server = new XatkitServer(configuration);
    }

    @Test
    public void startEmptyConfiguration() {
        this.server = new XatkitServer(new BaseConfiguration());
        this.server.start();
        softly.assertThat(server.getHttpServer().getLocalPort()).as("Valid port number").isEqualTo(XatkitServer
                .DEFAULT_SERVER_PORT);
        softly.assertThat(server.isStarted()).as("Server started").isTrue();
    }

    @Test
    public void startConfigurationWithPort() {
        Configuration configuration = new BaseConfiguration();
        configuration.setProperty(XatkitServer.SERVER_PORT_KEY, 1234);
        this.server = new XatkitServer(configuration);
        this.server.start();
        softly.assertThat(server.getHttpServer().getLocalPort()).as("Valid port number").isEqualTo(1234);
        softly.assertThat(server.isStarted()).as("Server started").isTrue();
    }

    @Test(expected = XatkitException.class)
    public void startTwoServersSamePort() {
        this.server = new XatkitServer(new BaseConfiguration());
        this.server.start();
        this.server2 = new XatkitServer(new BaseConfiguration());
        this.server2.start();
    }

    @Test
    public void stopNotStartedServer() {
        // Should only log a message, not throw an exception
        this.server = getValidXatkitServer();
        this.server.stop();
    }

    @Test
    public void stopStartedServer() {
        this.server = getValidXatkitServer();
        this.server.start();
        this.server.stop();
        assertThat(server.isStarted()).as("Server not started").isFalse();
    }

    @Test(expected = NullPointerException.class)
    public void registerNullWebhookEventProvider() {
        this.server = getValidXatkitServer();
        server.registerWebhookEventProvider(null);
    }

    @Test
    public void registerValidWebhookEventProvider() {
        this.server = getValidXatkitServer();
        StubJsonWebhookEventProvider stubJsonWebhookEventProvider = getStubWebhookEventProvider();
        this.server.registerWebhookEventProvider(stubJsonWebhookEventProvider);
        Assertions.assertThat(server.getRegisteredRestHandlers()).as("RestHandler collection size is 1").hasSize(1);
        Assertions.assertThat(server.getRegisteredRestHandlers().iterator().next()).as("Valid " +
                "RestHandler").isEqualTo(stubJsonWebhookEventProvider.getRestHandler());
    }

    @Test(expected = NullPointerException.class)
    public void unregisterNullWebhookEventProvider() {
        this.server = getValidXatkitServer();
        server.unregisterWebhookEventProvider(null);
    }

    @Test
    public void unregisterValidWebhookEventProvider() {
        this.server = getValidXatkitServer();
        StubJsonWebhookEventProvider stubJsonWebhookEventProvider = getStubWebhookEventProvider();
        this.server.registerWebhookEventProvider(stubJsonWebhookEventProvider);
        this.server.unregisterWebhookEventProvider(stubJsonWebhookEventProvider);
        Assertions.assertThat(server.getRegisteredRestHandlers()).as("RestHandler collection is empty").isEmpty();
    }

    @Test
    public void notifyAcceptedContentType() {
        this.server = getValidXatkitServer();
        StubJsonWebhookEventProvider stubJsonWebhookEventProvider = getStubWebhookEventProvider();
        this.server.registerWebhookEventProvider(stubJsonWebhookEventProvider);
        this.server.notifyRestHandler(stubJsonWebhookEventProvider.getEndpointURI(), Collections.emptyList(),
                Collections.emptyList(), "{field: value}", ContentType.APPLICATION_JSON.getMimeType());
        assertThat(stubJsonWebhookEventProvider.hasReceivedEvent()).as("WebhookEventProvider has received an event")
                .isTrue();
    }

    @Test
    public void notifyNotAcceptedContentType() {
        this.server = getValidXatkitServer();
        StubJsonWebhookEventProvider stubJsonWebhookEventProvider = getStubWebhookEventProvider();
        this.server.registerWebhookEventProvider(stubJsonWebhookEventProvider);
        this.server.notifyRestHandler(stubJsonWebhookEventProvider.getEndpointURI(), Collections.emptyList(),
                Collections.emptyList(), "test", "not valid");
        assertThat(stubJsonWebhookEventProvider.hasReceivedEvent()).as("WebhookEventProvider hasn't received an " +
                "event").isFalse();
    }

    @Test(expected = NullPointerException.class)
    public void registerRestEndpointNullUri() {
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(null, VALID_REST_HANDLER);
    }

    @Test(expected = NullPointerException.class)
    public void registerRestEndpointNullHandler() {
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(VALID_REST_URI, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void registerRestEndpointNotRootUri() {
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint("test", VALID_REST_HANDLER);
    }

    @Test
    public void registerRestEndpoint() {
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(VALID_REST_URI, VALID_REST_HANDLER);
        /*
         * Don't test the method to access the endpoint, they have their own test methods. Here we only check that
         * the method doesn't throw an exception.
         */
    }

    @Test(expected = NullPointerException.class)
    public void isRestEndpointNullUri() {
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(VALID_REST_URI, VALID_REST_HANDLER);
        this.server.isRestEndpoint(null);
    }

    @Test
    public void isRestEndpointRegisteredUri() {
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(VALID_REST_URI, VALID_REST_HANDLER);
        boolean result = this.server.isRestEndpoint(VALID_REST_URI);
        assertThat(result).as("Provided URI is a rest endpoint").isTrue();
    }

    @Test
    public void isRestEndpointNotRegisteredUri() {
        this.server = getValidXatkitServer();
        boolean result = this.server.isRestEndpoint(VALID_REST_URI);
        assertThat(result).as("Provided URI is not a rest endpoint").isFalse();
    }

    @Test(expected = NullPointerException.class)
    public void notifyRestHandlerNullUri() {
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(VALID_REST_URI, VALID_REST_HANDLER);
        this.server.notifyRestHandler(null, Collections.emptyList(), Collections.emptyList(), new JsonObject(),
                ContentType.APPLICATION_JSON.getMimeType());
    }

    @Test(expected = NullPointerException.class)
    public void notifyRestHandlerNullHeaders() {
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(VALID_REST_URI, VALID_REST_HANDLER);
        this.server.notifyRestHandler(VALID_REST_URI, null, Collections.emptyList(), new JsonObject(),
                ContentType.APPLICATION_JSON.getMimeType());
    }

    @Test(expected = NullPointerException.class)
    public void notifyRestHandlerNullParams() {
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(VALID_REST_URI, VALID_REST_HANDLER);
        this.server.notifyRestHandler(VALID_REST_URI, Collections.emptyList(), null, new JsonObject(),
                ContentType.APPLICATION_JSON.getMimeType());
    }

    @Test
    public void notifyRestHandlerNullJsonObject() {
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(VALID_REST_URI, VALID_REST_HANDLER);
        Object result = this.server.notifyRestHandler(VALID_REST_URI, Collections.emptyList(),
                Collections.emptyList(), null, ContentType.APPLICATION_JSON.getMimeType());
        checkRestHandlerResult(result);
    }

    @Test
    public void notifyRestHandler() {
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(VALID_REST_URI, VALID_REST_HANDLER);
        Object result = this.server.notifyRestHandler(VALID_REST_URI, Collections.emptyList(),
                Collections.emptyList(), "{}", ContentType.APPLICATION_JSON.getMimeType());
        checkRestHandlerResult(result);
    }

    @Test(expected = XatkitException.class)
    public void notifyRestHandlerNotRegisteredUri() {
        this.server = getValidXatkitServer();
        this.server.notifyRestHandler(VALID_REST_URI, Collections.emptyList(), Collections.emptyList(), null,
                ContentType.APPLICATION_JSON.getMimeType());
    }

    /**
     * Returns a valid {@link XatkitServer} instance listening to port {@code 1234}.
     * <p>
     * This method avoid port conflicts when starting the {@link StubXatkitCore}, and should be used in tests that do
     * not require specific {@link XatkitServer} ports.
     *
     * @return a valid {@link XatkitServer} instance listening to port {@code 1234}
     */
    private XatkitServer getValidXatkitServer() {
        Configuration configuration = new BaseConfiguration();
        configuration.setProperty(XatkitServer.SERVER_PORT_KEY, 1234);
        this.server = new XatkitServer(configuration);
        return this.server;
    }

    /**
     * Returns a valid {@link StubJsonWebhookEventProvider}.
     * <p>
     * This method starts a {@link StubXatkitCore} instance to create the {@link StubJsonWebhookEventProvider}, since
     * the {@link StubXatkitCore} instance listens to port {@code 5000} it is recommended to get a valid
     * {@link XatkitServer} instance with {@link #getValidXatkitServer()} to avoid port conflicts.
     *
     * @return a valid {@link StubJsonWebhookEventProvider}
     */
    private StubJsonWebhookEventProvider getStubWebhookEventProvider() {
        stubXatkitCore = new StubXatkitCore();
        EmptyRuntimePlatform emptyRuntimePlatform = new EmptyRuntimePlatform(stubXatkitCore);
        return new StubJsonWebhookEventProvider(emptyRuntimePlatform);
    }

    private void checkXatkitServer(XatkitServer server) {
        softly.assertThat(server.getRegisteredRestHandlers()).as("Empty registered RestHandler collection").isEmpty();
        assertThat(server.getHttpServer()).as("Not null HttpServer").isNotNull();
        assertThat(server.isStarted()).as("Server not started").isFalse();
        /*
         * Do not check the HttpServer port, it returns -1 until the server is started.
         */
    }

    private void checkRestHandlerResult(Object result) {
        assertThat(result).as("Result is not null").isNotNull();
        assertThat(result).as("Result is a JsonObject").isInstanceOf(JsonObject.class);
        JsonObject resultObject = (JsonObject) result;
        assertThat(resultObject.get("called").getAsBoolean()).as("Result contains the handler value").isTrue();
    }
}
