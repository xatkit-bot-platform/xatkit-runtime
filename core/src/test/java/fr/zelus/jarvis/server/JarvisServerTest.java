package fr.zelus.jarvis.server;

import fr.zelus.jarvis.AbstractJarvisTest;
import fr.zelus.jarvis.core.JarvisException;
import fr.zelus.jarvis.stubs.StubJarvisCore;
import fr.zelus.jarvis.stubs.io.StubJsonWebhookEventProvider;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class JarvisServerTest extends AbstractJarvisTest {

    private JarvisServer server;

    private StubJarvisCore stubJarvisCore;

    @After
    public void tearDown() {
        if (nonNull(server) && server.isStarted()) {
            server.stop();
        }
        if (nonNull(stubJarvisCore)) {
            stubJarvisCore.shutdown();
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
    public void stopNotStartedServer() {
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
        assertThat(server.getRegisteredWebhookEventProviders()).as("WebhookEventProvider collection size is " +
                "1").hasSize(1);
        assertThat(server.getRegisteredWebhookEventProviders().iterator().next()).as("Valid " +
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
        assertThat(server.getRegisteredWebhookEventProviders()).as("WebhookEventProvider collection is empty")
                .isEmpty();
    }

    @Test
    public void notifyAcceptedContentType() {
        this.server = getValidJarvisServer();
        StubJsonWebhookEventProvider stubJsonWebhookEventProvider = getStubWebhookEventProvider();
        this.server.registerWebhookEventProvider(stubJsonWebhookEventProvider);
        this.server.notifyWebhookEventProviders("application/json", "{field: value}");
        assertThat(stubJsonWebhookEventProvider.hasReceivedEvent()).as("WebhookEventProvider has received an event")
                .isTrue();
    }

    @Test
    public void notifyNotAcceptedContentType() {
        this.server = getValidJarvisServer();
        StubJsonWebhookEventProvider stubJsonWebhookEventProvider = getStubWebhookEventProvider();
        this.server.registerWebhookEventProvider(stubJsonWebhookEventProvider);
        this.server.notifyWebhookEventProviders("not valid", "test");
        assertThat(stubJsonWebhookEventProvider.hasReceivedEvent()).as("WebhookEventProvider hasn't received an " +
                "event").isFalse();
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
        return new StubJsonWebhookEventProvider(stubJarvisCore);
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
}
