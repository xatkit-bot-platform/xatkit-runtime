package com.xatkit.core.recognition.dialogflow;

import com.google.cloud.dialogflow.v2.SessionName;
import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.session.RuntimeContexts;
import com.xatkit.test.util.VariableLoaderHelper;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

public class DialogFlowSessionTest extends AbstractXatkitTest {

    private static String VALID_PROJECT_ID = VariableLoaderHelper.getXatkitDialogFlowProject();

    private DialogFlowSession session;

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test(expected = NullPointerException.class)
    public void constructNullSessionName() {
        session = new DialogFlowSession(null);
    }

    @Test
    public void constructValidSessionName() {
        SessionName sessionName = getValidSessionName();
        session = new DialogFlowSession(sessionName);
        assertDialogFlowSessionHasName(session, sessionName);
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        SessionName sessionName = getValidSessionName();
        session = new DialogFlowSession(sessionName, null);
    }

    @Test
    public void constructEmptyConfiguration() {
        SessionName sessionName = getValidSessionName();
        session = new DialogFlowSession(sessionName, new BaseConfiguration());
        assertDialogFlowSessionHasName(session, sessionName);
    }

    @Test
    public void constructConfigurationWithContextProperty() {
        SessionName sessionName = getValidSessionName();
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(RuntimeContexts.VARIABLE_TIMEOUT_KEY, 10);
        session = new DialogFlowSession(sessionName, configuration);
        assertDialogFlowSessionHasName(session, sessionName);
        softly.assertThat(session.getRuntimeContexts().getVariableTimeout()).as("Valid RuntimeContexts variable timeout")
                .isEqualTo(10);
    }

    private SessionName getValidSessionName() {
        return SessionName.of(VALID_PROJECT_ID, "demo");
    }

    private void assertDialogFlowSessionHasName(DialogFlowSession session, SessionName expectedSessionName) {
        softly.assertThat(session.getContextId()).as("Valid context ID").isEqualTo(expectedSessionName.toString());
        softly.assertThat(session.getSessionName()).as("Valid SessionName").isEqualTo(expectedSessionName);
        softly.assertThat(session.getRuntimeContexts()).as("Not null context").isNotNull();
    }
}
