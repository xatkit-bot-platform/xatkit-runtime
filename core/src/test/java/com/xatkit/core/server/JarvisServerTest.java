package com.xatkit.core.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.xatkit.core.JarvisException;
import com.xatkit.stubs.EmptyRuntimePlatform;
import com.xatkit.stubs.StubJarvisCore;
import com.xatkit.stubs.io.StubJsonWebhookEventProvider;
import com.xatkit.AbstractJarvisTest;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.http.Header;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class JarvisServerTest extends AbstractJarvisTest {

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

    private JarvisServer server;

    /**
     * A second server used to check multiple starts on the same port.
     * <p>
     * This variable is defined as a class attribute to allow to {@link JarvisServer#stop()} the server if an exception
     * occurred during the test cases it is involved in.
     */
    private JarvisServer server2;

    private StubJarvisCore stubJarvisCore;

    @BeforeClass
    public static void setUpBeforeClass() {
        VALID_REST_HANDLER = (headers, params, content) -> {
            JsonObject result = new JsonObject();
            result.add("called", new JsonPrimitive(true));
            return result;
        };
    }

    @After
    public void tearDown() {
        if (nonNull(server) && server.isStarted()) {
            server.stop();
        }
        if (nonNull(stubJarvisCore)) {
            stubJarvisCore.shutdown();
        }
        if (nonNull(server2) && server2.isStarted()) {
            server2.stop();
        }
    }

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        this.server = new JarvisServer(null);
    }

    @Test
    public void constructEmptyConfiguration() {
        this.server = new JarvisServer(new BaseConfiguration());
        checkJarvisServer(server);
    }

    @Test
    public void constructConfigurationWithPort() {
        Configuration configuration = new BaseConfiguration();
        configuration.setProperty(JarvisServer.SERVER_PORT_KEY, 1234);
        this.server = new JarvisServer(configuration);
    }

    @Test
    public void startEmptyConfiguration() {
        this.server = new JarvisServer(new BaseConfiguration());
        this.server.start();
        softly.assertThat(server.getHttpServer().getLocalPort()).as("Valid port number").isEqualTo(JarvisServer
                .DEFAULT_SERVER_PORT);
        softly.assertThat(server.isStarted()).as("Server started").isTrue();
    }

    @Test
    public void startConfigurationWithPort() {
        Configuration configuration = new BaseConfiguration();
        configuration.setProperty(JarvisServer.SERVER_PORT_KEY, 1234);
        this.server = new JarvisServer(configuration);
        this.server.start();
        softly.assertThat(server.getHttpServer().getLocalPort()).as("Valid port number").isEqualTo(1234);
        softly.assertThat(server.isStarted()).as("Server started").isTrue();
    }

    @Test(expected = JarvisException.class)
    public void startTwoServersSamePort() {
        this.server = new JarvisServer(new BaseConfiguration());
        this.server.start();
        this.server2 = new JarvisServer(new BaseConfiguration());
        this.server2.start();
    }

    @Test
    public void stopNotStartedServer() {
        // Should only log a message, not throw an exception
        this.server = getValidJarvisServer();
        this.server.stop();
    }

    @Test
    public void stopStartedServer() {
        this.server = getValidJarvisServer();
        this.server.start();
        this.server.stop();
        assertThat(server.isStarted()).as("Server not started").isFalse();
    }

    @Test(expected = NullPointerException.class)
    public void registerNullWebhookEventProvider() {
        this.server = getValidJarvisServer();
        server.registerWebhookEventProvider(null);
    }

    @Test
    public void registerValidWebhookEventProvider() {
        this.server = getValidJarvisServer();
        StubJsonWebhookEventProvider stubJsonWebhookEventProvider = getStubWebhookEventProvider();
        this.server.registerWebhookEventProvider(stubJsonWebhookEventProvider);
        Assertions.assertThat(server.getRegisteredWebhookEventProviders()).as("WebhookEventProvider collection size is " +
                "1").hasSize(1);
        Assertions.assertThat(server.getRegisteredWebhookEventProviders().iterator().next()).as("Valid " +
                "WebhookEventProvider").isEqualTo(stubJsonWebhookEventProvider);
    }

    @Test(expected = NullPointerException.class)
    public void unregisterNullWebhookEventProvider() {
        this.server = getValidJarvisServer();
        server.unregisterWebhookEventProvider(null);
    }

    @Test
    public void unregisterValidWebhookEventProvider() {
        this.server = getValidJarvisServer();
        StubJsonWebhookEventProvider stubJsonWebhookEventProvider = getStubWebhookEventProvider();
        this.server.registerWebhookEventProvider(stubJsonWebhookEventProvider);
        this.server.unregisterWebhookEventProvider(stubJsonWebhookEventProvider);
        Assertions.assertThat(server.getRegisteredWebhookEventProviders()).as("WebhookEventProvider collection is empty")
                .isEmpty();
    }

    @Test
    public void notifyAcceptedContentType() {
        this.server = getValidJarvisServer();
        StubJsonWebhookEventProvider stubJsonWebhookEventProvider = getStubWebhookEventProvider();
        this.server.registerWebhookEventProvider(stubJsonWebhookEventProvider);
        this.server.notifyWebhookEventProviders("application/json", "{field: value}", new Header[0]);
        assertThat(stubJsonWebhookEventProvider.hasReceivedEvent()).as("WebhookEventProvider has received an event")
                .isTrue();
    }

    @Test
    public void notifyNotAcceptedContentType() {
        this.server = getValidJarvisServer();
        StubJsonWebhookEventProvider stubJsonWebhookEventProvider = getStubWebhookEventProvider();
        this.server.registerWebhookEventProvider(stubJsonWebhookEventProvider);
        this.server.notifyWebhookEventProviders("not valid", "test", new Header[0]);
        assertThat(stubJsonWebhookEventProvider.hasReceivedEvent()).as("WebhookEventProvider hasn't received an " +
                "event").isFalse();
    }

    @Test(expected = NullPointerException.class)
    public void registerRestEndpointNullUri() {
        this.server = getValidJarvisServer();
        this.server.registerRestEndpoint(null, VALID_REST_HANDLER);
    }

    @Test(expected = NullPointerException.class)
    public void registerRestEndpointNullHandler() {
        this.server = getValidJarvisServer();
        this.server.registerRestEndpoint(VALID_REST_URI, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void registerRestEndpointNotRootUri() {
        this.server = getValidJarvisServer();
        this.server.registerRestEndpoint("test", VALID_REST_HANDLER);
    }

    @Test
    public void registerRestEndpoint() {
        this.server = getValidJarvisServer();
        this.server.registerRestEndpoint(VALID_REST_URI, VALID_REST_HANDLER);
        /*
         * Don't test the method to access the endpoint, they have their own test methods. Here we only check that
         * the method doesn't throw an exception.
         */
    }

    @Test(expected = NullPointerException.class)
    public void isRestEndpointNullUri() {
        this.server = getValidJarvisServer();
        this.server.registerRestEndpoint(VALID_REST_URI, VALID_REST_HANDLER);
        this.server.isRestEndpoint(null);
    }

    @Test
    public void isRestEndpointRegisteredUri() {
        this.server = getValidJarvisServer();
        this.server.registerRestEndpoint(VALID_REST_URI, VALID_REST_HANDLER);
        boolean result = this.server.isRestEndpoint(VALID_REST_URI);
        assertThat(result).as("Provided URI is a rest endpoint").isTrue();
    }

    @Test
    public void isRestEndpointNotRegisteredUri() {
        this.server = getValidJarvisServer();
        boolean result = this.server.isRestEndpoint(VALID_REST_URI);
        assertThat(result).as("Provided URI is not a rest endpoint").isFalse();
    }

    @Test(expected = NullPointerException.class)
    public void notifyRestHandlerNullUri() {
        this.server = getValidJarvisServer();
        this.server.registerRestEndpoint(VALID_REST_URI, VALID_REST_HANDLER);
        this.server.notifyRestHandler(null, Collections.emptyList(), Collections.emptyList(), new JsonObject());
    }

    @Test(expected = NullPointerException.class)
    public void notifyRestHandlerNullHeaders() {
        this.server = getValidJarvisServer();
        this.server.registerRestEndpoint(VALID_REST_URI, VALID_REST_HANDLER);
        this.server.notifyRestHandler(VALID_REST_URI, null, Collections.emptyList(), new JsonObject());
    }

    @Test(expected = NullPointerException.class)
    public void notifyRestHandlerNullParams() {
        this.server = getValidJarvisServer();
        this.server.registerRestEndpoint(VALID_REST_URI, VALID_REST_HANDLER);
        this.server.notifyRestHandler(VALID_REST_URI, Collections.emptyList(), null, new JsonObject());
    }

    @Test
    public void notifyRestHandlerNullJsonObject() {
        this.server = getValidJarvisServer();
        this.server.registerRestEndpoint(VALID_REST_URI, VALID_REST_HANDLER);
        JsonElement result = this.server.notifyRestHandler(VALID_REST_URI, Collections.emptyList(),
                Collections.emptyList(),
                null);
        checkRestHandlerResult(result);
    }

    @Test
    public void notifyRestHandler() {
        this.server = getValidJarvisServer();
        this.server.registerRestEndpoint(VALID_REST_URI, VALID_REST_HANDLER);
        JsonElement result = this.server.notifyRestHandler(VALID_REST_URI, Collections.emptyList(),
                Collections.emptyList(), new JsonObject());
        checkRestHandlerResult(result);
    }

    @Test(expected = JarvisException.class)
    public void notifyRestHandlerNotRegisteredUri() {
        this.server = getValidJarvisServer();
        this.server.notifyRestHandler(VALID_REST_URI, Collections.emptyList(), Collections.emptyList(), null);
    }

    /**
     * Returns a valid {@link JarvisServer} instance listening to port {@code 1234}.
     * <p>
     * This method avoid port conflicts when starting the {@link StubJarvisCore}, and should be used in tests that do
     * not require specific {@link JarvisServer} ports.
     *
     * @return a valid {@link JarvisServer} instance listening to port {@code 1234}
     */
    private JarvisServer getValidJarvisServer() {
        Configuration configuration = new BaseConfiguration();
        configuration.setProperty(JarvisServer.SERVER_PORT_KEY, 1234);
        this.server = new JarvisServer(configuration);
        return this.server;
    }

    /**
     * Returns a valid {@link StubJsonWebhookEventProvider}.
     * <p>
     * This method starts a {@link StubJarvisCore} instance to create the {@link StubJsonWebhookEventProvider}, since
     * the {@link StubJarvisCore} instance listens to port {@code 5000} it is recommended to get a valid
     * {@link JarvisServer} instance with {@link #getValidJarvisServer()} to avoid port conflicts.
     *
     * @return a valid {@link StubJsonWebhookEventProvider}
     */
    private StubJsonWebhookEventProvider getStubWebhookEventProvider() {
        stubJarvisCore = new StubJarvisCore();
        EmptyRuntimePlatform emptyRuntimePlatform = new EmptyRuntimePlatform(stubJarvisCore);
        return new StubJsonWebhookEventProvider(emptyRuntimePlatform);
    }

    private void checkJarvisServer(JarvisServer server) {
        softly.assertThat(server.getRegisteredWebhookEventProviders()).as("Empty registered WebhookEventProvider " +
                "collection").isEmpty();
        assertThat(server.getHttpServer()).as("Not null HttpServer").isNotNull();
        assertThat(server.isStarted()).as("Server not started").isFalse();
        /*
         * Do not check the HttpServer port, it returns -1 until the server is started.
         */
    }

    private void checkRestHandlerResult(JsonElement result) {
        assertThat(result).as("Result is not null").isNotNull();
        assertThat(result).as("Result is a JsonObject").isInstanceOf(JsonObject.class);
        JsonObject resultObject = (JsonObject) result;
        assertThat(resultObject.get("called").getAsBoolean()).as("Result contains the handler value").isTrue();
    }
}
