package com.xatkit.core.session;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.State;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class XatkitSessionTest extends AbstractXatkitTest {

    private static State TEST_STATE = ExecutionFactory.eINSTANCE.createState();

    static {
        TEST_STATE.setName("Test_State");
    }

    private XatkitSession session;

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test(expected = NullPointerException.class)
    public void constructNullSessionId() {
        session = new XatkitSession(null);
    }

    @Test
    public void constructValidSessionId() {
        session = new XatkitSession("session");
        assertValidXatkitSession(session);
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        session = new XatkitSession("session", null);
    }

    @Test
    public void constructEmptyConfiguration() {
        session = new XatkitSession("session", new BaseConfiguration());
        assertValidXatkitSession(session);
    }

    @Test
    public void constructConfigurationWithContextProperty() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(RuntimeContexts.VARIABLE_TIMEOUT_KEY, 10);
        session = new XatkitSession("session", configuration);
        assertValidXatkitSession(session);
        softly.assertThat(session.getRuntimeContexts().getVariableTimeout()).as("Valid RuntimeContexts variable timeout")
                .isEqualTo(10);
    }

    @Test(expected = NullPointerException.class)
    public void storeNullKey() {
        session = new XatkitSession("sessionID");
        session.store(null, "value");
    }

    @Test
    public void storeNullValue() {
        session = new XatkitSession("sessionID");
        session.store("key", null);
    }

    @Test
    public void storeValidValue() {
        session = new XatkitSession("sessionID");
        session.store("key", "value");
        /*
         * Do not check if the value is correctly stored, this is done in get() test cases.
         */
    }

    @Test(expected = NullPointerException.class)
    public void storeListNullKey() {
        session = new XatkitSession("sessionID");
        session.storeList(null, "value");
    }

    @Test
    public void storeListNullValue() {
        session = new XatkitSession("sessionID");
        session.storeList("key", null);
    }

    @Test
    public void getStoredValueSingleValued() {
        session = new XatkitSession("sessionID");
        session.store("key", "value");
        Object result = session.get("key");
        assertThat(result).as("Valid result").isEqualTo("value");
    }

    @Test
    public void getStoredValueList() {
        session = new XatkitSession("sessionID");
        session.storeList("key", "value");
        Object result = session.get("key");
        assertThat(result).as("Result is a list").isInstanceOf(List.class);
        assertThat((List) result).as("Result list contains the stored value").contains("value");
    }

    @Test
    public void getStoredValueSingleErasedWithList() {
        session = new XatkitSession("sessionID");
        session.store("key", "value");
        session.storeList("key", "value2");
        Object result = session.get("key");
        assertThat(result).as("Result is a list").isInstanceOf(List.class);
        assertThat((List) result).as("Result list contains the stored value").contains("value2");
    }

    @Test
    public void getStoredValueListErasedWithSingle() {
        session = new XatkitSession("sessionID");
        session.storeList("key", "value");
        session.store("key", "value2");
        Object result = session.get("key");
        assertThat(result).as("Valid single-valued result").isEqualTo("value2");
    }

    @Test
    public void getStateNewSession() {
        session = new XatkitSession("sessionID");
        assertThat(session.getState()).as("Session state is null").isNull();
    }

    @Test
    public void setStateNewSession() {
        session = new XatkitSession("sessionId");
        session.setState(TEST_STATE);
        assertThat(session.getState()).as("State has been set").isEqualTo(TEST_STATE);
    }

    @Test
    public void setStateErasePreviousState() {
        session = new XatkitSession("sessionId");
        session.setState(TEST_STATE);
        State testState2 = ExecutionFactory.eINSTANCE.createState();
        testState2.setName("Test_State2");
        session.setState(testState2);
        assertThat(session.getState()).as("State correctly erased").isEqualTo(testState2);
    }

    private void assertValidXatkitSession(XatkitSession session) {
        softly.assertThat(session.getSessionId()).as("Valid session ID").isEqualTo("session");
        softly.assertThat(session.getRuntimeContexts()).as("Not null context").isNotNull();
        softly.assertThat(session.getRuntimeContexts().getContextMap()).as("Empty context").isEmpty();
    }
}
