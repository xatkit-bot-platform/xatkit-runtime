package com.xatkit.core.session;

import com.xatkit.AbstractJarvisTest;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test(expected = NullPointerException.class)
    public void storeNullKey() {
        session = new JarvisSession("sessionID");
        session.store(null, "value");
    }

    @Test
    public void storeNullValue() {
        session = new JarvisSession("sessionID");
        session.store("key", null);
    }

    @Test
    public void storeValidValue() {
        session = new JarvisSession("sessionID");
        session.store("key", "value");
        /*
         * Do not check if the value is correctly stored, this is done in get() test cases.
         */
    }

    @Test(expected = NullPointerException.class)
    public void storeListNullKey() {
        session = new JarvisSession("sessionID");
        session.storeList(null, "value");
    }

    @Test
    public void storeListNullValue() {
        session = new JarvisSession("sessionID");
        session.storeList("key", null);
    }

    @Test
    public void getStoredValueSingleValued() {
        session = new JarvisSession("sessionID");
        session.store("key", "value");
        Object result = session.get("key");
        assertThat(result).as("Valid result").isEqualTo("value");
    }

    @Test
    public void getStoredValueList() {
        session = new JarvisSession("sessionID");
        session.storeList("key", "value");
        Object result = session.get("key");
        assertThat(result).as("Result is a list").isInstanceOf(List.class);
        assertThat((List) result).as("Result list contains the stored value").contains("value");
    }

    @Test
    public void getStoredValueSingleErasedWithList() {
        session = new JarvisSession("sessionID");
        session.store("key", "value");
        session.storeList("key", "value2");
        Object result = session.get("key");
        assertThat(result).as("Result is a list").isInstanceOf(List.class);
        assertThat((List) result).as("Result list contains the stored value").contains("value2");
    }

    @Test
    public void getStoredValueListErasedWithSingle() {
        session = new JarvisSession("sessionID");
        session.storeList("key", "value");
        session.store("key", "value2");
        Object result = session.get("key");
        assertThat(result).as("Valid single-valued result").isEqualTo("value2");
    }

    private void checkJarvisSession(JarvisSession session) {
        softly.assertThat(session.getSessionId()).as("Valid session ID").isEqualTo("session");
        softly.assertThat(session.getRuntimeContexts()).as("Not null context").isNotNull();
        softly.assertThat(session.getRuntimeContexts().getContextMap()).as("Empty context").isEmpty();
    }
}
