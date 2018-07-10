package fr.zelus.jarvis.core.session;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

public class JarvisSessionTest {

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
        softly.assertThat(session.getSessionId()).as("Valid session ID").isEqualTo("session");
        softly.assertThat(session.getJarvisContext()).as("Not null context").isNotNull();
        softly.assertThat(session.getJarvisContext().getContextMap()).as("Empty context").isEmpty();
    }
}
