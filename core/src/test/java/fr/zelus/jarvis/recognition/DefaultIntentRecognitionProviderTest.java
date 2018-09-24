package fr.zelus.jarvis.recognition;

import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.intent.IntentFactory;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultIntentRecognitionProviderTest {

    private DefaultIntentRecognitionProvider provider;

    @Before
    public void setUp() {
        provider = new DefaultIntentRecognitionProvider(new BaseConfiguration());
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        provider = new DefaultIntentRecognitionProvider(null);
    }

    @Test
    public void constructValidConfiguration() {
        provider = new DefaultIntentRecognitionProvider(new BaseConfiguration());
        assertThat(provider.isShutdown()).as("Provider not shut down").isFalse();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void registerIntentDefinition() {
        provider.registerIntentDefinition(IntentFactory.eINSTANCE.createIntentDefinition());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void deleteIntentDefinition() {
        provider.deleteIntentDefinition(IntentFactory.eINSTANCE.createIntentDefinition());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void trainMLEngine() {
        provider.trainMLEngine();
    }

    @Test
    public void createSessionEmptyConfiguration() {
        JarvisSession session = provider.createSession("SessionID");
        assertThat(session).as("Not null session").isNotNull();
        assertThat(session.getSessionId()).as("Valid session id").isEqualTo("SessionID");
        assertThat(session.getJarvisContext()).as("Not null context").isNotNull();
        assertThat(session.getJarvisContext().getVariableTimeout()).as("Default variable timeout").isEqualTo
                (JarvisContext.DEFAULT_VARIABLE_TIMEOUT_VALUE);
    }

    @Test
    public void createSessionCustomTimeoutValue() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JarvisContext.VARIABLE_TIMEOUT_KEY, 10);
        provider = new DefaultIntentRecognitionProvider(configuration);
        JarvisSession session = provider.createSession("SessionID");
        assertThat(session).as("Not null session").isNotNull();
        assertThat(session.getSessionId()).as("Valid sessio id").isEqualTo("SessionID");
        assertThat(session.getJarvisContext()).as("Not null context").isNotNull();
        assertThat(session.getJarvisContext().getVariableTimeout()).as("Custom variable timeout").isEqualTo(10);
    }

    @Test
    public void shutdown() {
        provider.shutdown();
        assertThat(provider.isShutdown()).as("Provider is shutdown").isTrue();
    }

    @Test
    public void isShutdownNotShutdown() {
        assertThat(provider.isShutdown()).as("Provider is not shutdown").isFalse();
    }
}
