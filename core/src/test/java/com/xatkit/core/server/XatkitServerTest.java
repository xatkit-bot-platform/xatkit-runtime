package com.xatkit.core.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.XatkitException;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.stubs.EmptyRuntimePlatform;
import com.xatkit.stubs.StubXatkitCore;
import com.xatkit.stubs.io.StubJsonWebhookEventProvider;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.http.entity.ContentType;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
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

    @Before
    public void setUp() throws IOException {
        File publicFile = new File(XatkitServerUtils.PUBLIC_DIRECTORY_NAME);
        if (publicFile.exists()) {
            FileUtils.forceDelete(publicFile);
        }
    }

    @After
    public void tearDown() throws IOException {
        if (nonNull(server) && server.isStarted()) {
            server.stop();
        }
        if (nonNull(stubXatkitCore)) {
            stubXatkitCore.shutdown();
        }
        if (nonNull(server2) && server2.isStarted()) {
            server2.stop();
        }
        File publicFile = new File(XatkitServerUtils.PUBLIC_DIRECTORY_NAME);
        if (publicFile.exists()) {
            FileUtils.forceDelete(publicFile);
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
        assertXatkitServerState(server);
    }

    @Test
    public void constructConfigurationWithPort() {
        Configuration configuration = new BaseConfiguration();
        configuration.setProperty(XatkitServerUtils.SERVER_PORT_KEY, 1234);
        this.server = new XatkitServer(configuration);
    }

    @Test(expected = XatkitException.class)
    public void constructConfigurationWithKeystoreNullPublicURL() {
        Configuration configuration = new BaseConfiguration();
        /*
         * test.jks doesn't exist, but the exception should be thrown anyway.
         */
        configuration.setProperty(XatkitServerUtils.SERVER_KEYSTORE_LOCATION_KEY, "test.jks");
        this.server = new XatkitServer(configuration);
    }

    @Test(expected = XatkitException.class)
    public void constructConfigurationInvalidKeystoreLocation() {
        Configuration configuration = new BaseConfiguration();
        configuration.setProperty(XatkitServerUtils.SERVER_KEYSTORE_LOCATION_KEY, "test.jks");
        configuration.setProperty(XatkitServerUtils.SERVER_PUBLIC_URL_KEY, "https://localhost");
        this.server = new XatkitServer(configuration);
    }

    /*
     * TODO test cases with valid keystore
     */

    @Test
    public void startEmptyConfiguration() {
        this.server = new XatkitServer(new BaseConfiguration());
        this.server.start();
        softly.assertThat(server.getHttpServer().getLocalPort()).as("Valid port number")
                .isEqualTo(XatkitServerUtils.DEFAULT_SERVER_PORT);
        softly.assertThat(server.isStarted()).as("Server started").isTrue();
    }

    @Test
    public void startConfigurationWithPort() {
        Configuration configuration = new BaseConfiguration();
        configuration.setProperty(XatkitServerUtils.SERVER_PORT_KEY, 1234);
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
    public void notifyAcceptedContentType() throws RestHandlerException {
        this.server = getValidXatkitServer();
        StubJsonWebhookEventProvider stubJsonWebhookEventProvider = getStubWebhookEventProvider();
        this.server.registerWebhookEventProvider(stubJsonWebhookEventProvider);
        this.server.notifyRestHandler(HttpMethod.POST, stubJsonWebhookEventProvider.getEndpointURI(),
                Collections.emptyList(),
                Collections.emptyList(), "{field: value}", ContentType.APPLICATION_JSON.getMimeType());
        assertThat(stubJsonWebhookEventProvider.hasReceivedEvent()).as("WebhookEventProvider has received an event")
                .isTrue();
    }

    @Test
    public void notifyNotAcceptedContentType() throws RestHandlerException {
        this.server = getValidXatkitServer();
        StubJsonWebhookEventProvider stubJsonWebhookEventProvider = getStubWebhookEventProvider();
        this.server.registerWebhookEventProvider(stubJsonWebhookEventProvider);
        this.server.notifyRestHandler(HttpMethod.POST, stubJsonWebhookEventProvider.getEndpointURI(),
                Collections.emptyList(),
                Collections.emptyList(), "test", "not valid");
        assertThat(stubJsonWebhookEventProvider.hasReceivedEvent()).as("WebhookEventProvider hasn't received an " +
                "event").isFalse();
    }

    @Test(expected = NullPointerException.class)
    public void registerRestEndpointNullUri() {
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(HttpMethod.POST, null, VALID_REST_HANDLER);
    }

    @Test(expected = NullPointerException.class)
    public void registerRestEndpointNullHandler() {
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(HttpMethod.POST, VALID_REST_URI, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void registerRestEndpointNotRootUri() {
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(HttpMethod.POST, "test", VALID_REST_HANDLER);
    }

    @Test
    public void registerRestEndpoint() {
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(HttpMethod.POST, VALID_REST_URI, VALID_REST_HANDLER);
        assertThat(this.server.getRegisteredRestHandler(HttpMethod.POST, VALID_REST_URI)).as("Handler registered").isEqualTo(VALID_REST_HANDLER);
    }

    @Test(expected = NullPointerException.class)
    public void isRestEndpointNullMethod() {
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(HttpMethod.POST, VALID_REST_URI, VALID_REST_HANDLER);
        this.server.isRestEndpoint(null, VALID_REST_URI);
    }

    @Test(expected = NullPointerException.class)
    public void isRestEndpointNullUri() {
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(HttpMethod.POST, VALID_REST_URI, VALID_REST_HANDLER);
        this.server.isRestEndpoint(HttpMethod.POST, null);
    }

    @Test
    public void isRestEndpointRegisteredUri() {
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(HttpMethod.POST, VALID_REST_URI, VALID_REST_HANDLER);
        boolean result = this.server.isRestEndpoint(HttpMethod.POST, VALID_REST_URI);
        assertThat(result).as("Provided URI is a rest endpoint").isTrue();
    }

    @Test
    public void isRestEndpointRegisteredUriTrailingSlash() {
        // See https://github.com/xatkit-bot-platform/xatkit-runtime/issues/254
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(HttpMethod.POST, VALID_REST_URI, VALID_REST_HANDLER);
        assertThat(this.server.isRestEndpoint(HttpMethod.POST, VALID_REST_URI + "/")).as("The URI is valid even with " +
                "a trailing /").isTrue();
    }

    @Test
    public void isRestEndpointNotRegisteredUri() {
        this.server = getValidXatkitServer();
        boolean result = this.server.isRestEndpoint(HttpMethod.POST, VALID_REST_URI);
        assertThat(result).as("Provided URI is not a rest endpoint").isFalse();
    }

    @Test
    public void isRestEndpointNotRegisteredMethod() {
        this.server = getValidXatkitServer();
        boolean result = this.server.isRestEndpoint(HttpMethod.PUT, VALID_REST_URI);
        assertThat(result).as("Provided URI + Method is not a rest endpoint").isFalse();
    }

    @Test(expected = NullPointerException.class)
    public void notifyRestHandlerNullMethod() throws RestHandlerException {
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(HttpMethod.POST, VALID_REST_URI, VALID_REST_HANDLER);
        this.server.notifyRestHandler(null, VALID_REST_URI, Collections.emptyList(), Collections.emptyList(),
                new JsonObject(), ContentType.APPLICATION_JSON.getMimeType());
    }

    @Test(expected = NullPointerException.class)
    public void notifyRestHandlerNullUri() throws RestHandlerException {
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(HttpMethod.POST, VALID_REST_URI, VALID_REST_HANDLER);
        this.server.notifyRestHandler(HttpMethod.POST, null, Collections.emptyList(), Collections.emptyList(),
                new JsonObject(),
                ContentType.APPLICATION_JSON.getMimeType());
    }

    @Test(expected = NullPointerException.class)
    public void notifyRestHandlerNullHeaders() throws RestHandlerException {
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(HttpMethod.POST, VALID_REST_URI, VALID_REST_HANDLER);
        this.server.notifyRestHandler(HttpMethod.POST, VALID_REST_URI, null, Collections.emptyList(), new JsonObject(),
                ContentType.APPLICATION_JSON.getMimeType());
    }

    @Test(expected = NullPointerException.class)
    public void notifyRestHandlerNullParams() throws RestHandlerException {
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(HttpMethod.POST, VALID_REST_URI, VALID_REST_HANDLER);
        this.server.notifyRestHandler(HttpMethod.POST, VALID_REST_URI, Collections.emptyList(), null, new JsonObject(),
                ContentType.APPLICATION_JSON.getMimeType());
    }

    @Test
    public void notifyRestHandlerNullJsonObject() throws RestHandlerException {
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(HttpMethod.POST, VALID_REST_URI, VALID_REST_HANDLER);
        Object result = this.server.notifyRestHandler(HttpMethod.POST, VALID_REST_URI, Collections.emptyList(),
                Collections.emptyList(), null, ContentType.APPLICATION_JSON.getMimeType());
        assertValidRestHandlerResult(result);
    }

    @Test
    public void notifyRestHandler() throws RestHandlerException {
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(HttpMethod.POST, VALID_REST_URI, VALID_REST_HANDLER);
        Object result = this.server.notifyRestHandler(HttpMethod.POST, VALID_REST_URI, Collections.emptyList(),
                Collections.emptyList(), "{}", ContentType.APPLICATION_JSON.getMimeType());
        assertValidRestHandlerResult(result);
    }

    @Test
    public void notifyRestHandlerTrailingSlash() throws RestHandlerException {
        // See https://github.com/xatkit-bot-platform/xatkit-runtime/issues/254
        this.server = getValidXatkitServer();
        this.server.registerRestEndpoint(HttpMethod.POST, VALID_REST_URI, VALID_REST_HANDLER);
        Object result = this.server.notifyRestHandler(HttpMethod.POST, VALID_REST_URI + "/", Collections.emptyList(),
                Collections.emptyList(), "{}", ContentType.APPLICATION_JSON.getMimeType());
        assertValidRestHandlerResult(result);
    }

    @Test(expected = XatkitException.class)
    public void notifyRestHandlerNotRegisteredUri() throws RestHandlerException {
        this.server = getValidXatkitServer();
        this.server.notifyRestHandler(HttpMethod.POST, VALID_REST_URI, Collections.emptyList(),
                Collections.emptyList(), null,
                ContentType.APPLICATION_JSON.getMimeType());
    }

    @Test(expected = NullPointerException.class)
    public void createOrReplacePublicFileNullSession() {
        this.server = getValidXatkitServer();
        this.server.createOrReplacePublicFile(null, "test.txt", "A test file");
    }

    @Test(expected = NullPointerException.class)
    public void createOrReplacePublicFileNullPath() {
        this.server = getValidXatkitServer();
        this.server.createOrReplacePublicFile(new XatkitSession("test"), null, "A test file");
    }

    @Test(expected = NullPointerException.class)
    public void createOrReplacePublicFileNullOrigin() {
        this.server = getValidXatkitServer();
        this.server.createOrReplacePublicFile(new XatkitSession("test"), "test.txt", (File) null);
    }

    @Test
    public void createOrReplacePublicFileValidFile() throws IOException {
        this.server = getValidXatkitServer();
        File file = this.server.createOrReplacePublicFile(new XatkitSession("test"), "test.txt", "A test file");
        assertThat(file).as("Not null file").isNotNull();
        assertThat(file).as("File exists").exists();
        String readContent = FileUtils.readFileToString(file);
        assertThat(readContent).as("Valid content").isEqualTo("A test file");
    }

    @Test
    public void createOrReplacePublicFileHierarchy() throws IOException {
        this.server = getValidXatkitServer();
        File file = this.server.createOrReplacePublicFile(new XatkitSession("test"), "/test2/test.txt", "A test file");
        assertThat(file).as("Not null file").isNotNull();
        assertThat(file).as("File exists").exists();
        String readContent = FileUtils.readFileToString(file);
        assertThat(readContent).as("Valid content").isEqualTo("A test file");
    }

    @Test
    public void createOrReplacePublicFileReplaceExistingFile() throws IOException {
        this.server = getValidXatkitServer();
        XatkitSession session = new XatkitSession("test");
        File file = this.server.createOrReplacePublicFile(session, "test.txt", "A test file");
        File file2 = this.server.createOrReplacePublicFile(session, "test.txt", "Another test file");
        assertThat(file2).as("File2 is not null").isNotNull();
        assertThat(file2).as("File2 exists").exists();
        String readContent = FileUtils.readFileToString(file2);
        assertThat(readContent).as("Valid content").isEqualTo("Another test file");
    }

    @Test(expected = XatkitException.class)
    public void createOrReplacePublicFileForbiddenPath() {
        this.server = getValidXatkitServer();
        XatkitSession session = new XatkitSession("test");
        File file = this.server.createOrReplacePublicFile(session, "../../test.txt", "A forbidden test file");
    }

    @Test(expected = NullPointerException.class)
    public void getPublicFileNullSession() {
        this.server = getValidXatkitServer();
        server.getPublicFile(null, "test.txt");
    }

    @Test(expected = NullPointerException.class)
    public void getPublicFileNullFile() {
        this.server = getValidXatkitServer();
        server.getPublicFile(new XatkitSession("test"), null);
    }

    @Test(expected = NullPointerException.class)
    public void getPublicFileNullPath() {
        this.server = getValidXatkitServer();
        server.getPublicFile(null);
    }

    @Test
    public void getPublicFileValidFile() {
        this.server = getValidXatkitServer();
        XatkitSession session = new XatkitSession("test");
        server.createOrReplacePublicFile(session, "test.txt", "A test file");
        File file = server.getPublicFile(session, "test.txt");
        assertThat(file).as("The file is not null").isNotNull();
        assertThat(file).as("The file exists").exists();
    }

    @Test
    public void getPublicFileValidFileHierarchy() {
        this.server = getValidXatkitServer();
        XatkitSession session = new XatkitSession("test");
        server.createOrReplacePublicFile(session, "test2/test.txt", "A test file");
        File file = server.getPublicFile(session, "test2/test.txt");
        assertThat(file).as("File is not null").isNotNull();
        assertThat(file).as("File exists").exists();
    }

    @Test
    public void getPublicFileNotExist() {
        this.server = getValidXatkitServer();
        File file = server.getPublicFile("do-not-exist.png");
        assertThat(file).as("Returned file is null").isNull();
    }

    @Test(expected = XatkitException.class)
    public void getPublicFileIllegalPath() {
        this.server = getValidXatkitServer();
        /*
         * Need to first create a dummy file, otherwise the path is not resolved at all.
         */
        XatkitSession session = new XatkitSession("test");
        server.createOrReplacePublicFile(session, "test.txt", "A test file");
        File file = server.getPublicFile(session, "../../pom.xml");
    }

    @Test(expected = NullPointerException.class)
    public void getPublicURLNullFile() {
        this.server = getValidXatkitServer();
        server.getPublicURL(null);
    }

    @Test
    public void getPublicURLValidFile() {
        this.server = getValidXatkitServer();
        XatkitSession session = new XatkitSession("test");
        server.createOrReplacePublicFile(session, "test.txt", "A test file");
        File file = server.getPublicFile("test/test.txt");
        String url = server.getPublicURL(file);
        assertThat(url).as("URL not null").isNotNull();
        assertThat(url).as("Valid URL").isEqualTo("http://localhost:1234/content/test/test.txt");
    }

    @Test
    public void getPublicURLValidFileHierarchy() {
        this.server = getValidXatkitServer();
        XatkitSession session = new XatkitSession("test");
        server.createOrReplacePublicFile(session, "test2/test.txt", "A test file");
        File file = server.getPublicFile(session, "test2/test.txt");
        String url = server.getPublicURL(file);
        assertThat(url).as("URL is not null").isNotNull();
        assertThat(url).as("Valid URL").isEqualTo("http://localhost:1234/content/test/test2/test.txt");
    }

    @Test
    public void getPublicURLFileNotExist() {
        this.server = getValidXatkitServer();
        File file = new File("public/do-not-exist.png");
        String url = this.server.getPublicURL(file);
        assertThat(url).as("URL is null").isNull();
    }

    @Test(expected = XatkitException.class)
    public void getPublicURLIllegalPath() {
        this.server = getValidXatkitServer();
        File file = new File("pom.xml");
        String url = server.getPublicURL(file);
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
        configuration.setProperty(XatkitServerUtils.SERVER_PORT_KEY, 1234);
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

    private void assertXatkitServerState(XatkitServer server) {
        softly.assertThat(server.getRegisteredRestHandlers()).as("Empty registered RestHandler collection").isEmpty();
        assertThat(server.getHttpServer()).as("Not null HttpServer").isNotNull();
        assertThat(server.isStarted()).as("Server not started").isFalse();
        /*
         * Do not check the HttpServer port, it returns -1 until the server is started.
         */
    }

    private void assertValidRestHandlerResult(Object result) {
        assertThat(result).as("Result is not null").isNotNull();
        assertThat(result).as("Result is a JsonObject").isInstanceOf(JsonObject.class);
        JsonObject resultObject = (JsonObject) result;
        assertThat(resultObject.get("called").getAsBoolean()).as("Result contains the handler value").isTrue();
    }
}
