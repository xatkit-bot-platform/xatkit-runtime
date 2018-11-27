package edu.uoc.som.jarvis.core.session;

import edu.uoc.som.jarvis.AbstractJarvisTest;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

public class JarvisSessionTest extends AbstractJarvisTest {

    private JarvisSession session;

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test(expected = NullPointerException.class)
    public void constructNullSessionId() {
        session = new JarvisSession(null);
    }

    @Test
    public void constructValidSessionId() {
        session = new JarvisSession("session");
        checkJarvisSession(session);
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        session = new JarvisSession("session", null);
    }

    @Test
    public void constructEmptyConfiguration() {
        session = new JarvisSession("session", new BaseConfiguration());
        checkJarvisSession(session);
    }

    @Test
    public void constructConfigurationWithContextProperty() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(RuntimeContexts.VARIABLE_TIMEOUT_KEY, 10);
        session = new JarvisSession("session", configuration);
        checkJarvisSession(session);
        softly.assertThat(session.getRuntimeContexts().getVariableTimeout()).as("Valid RuntimeContexts variable timeout")
                .isEqualTo(10);
    }

    private void checkJarvisSession(JarvisSession session) {
        softly.assertThat(session.getSessionId()).as("Valid session ID").isEqualTo("session");
        softly.assertThat(session.getRuntimeContexts()).as("Not null context").isNotNull();
        softly.assertThat(session.getRuntimeContexts().getContextMap()).as("Empty context").isEmpty();
    }
}
