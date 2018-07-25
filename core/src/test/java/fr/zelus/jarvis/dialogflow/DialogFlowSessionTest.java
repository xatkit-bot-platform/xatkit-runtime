package fr.zelus.jarvis.dialogflow;

import com.google.cloud.dialogflow.v2.SessionName;
import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.util.VariableLoaderHelper;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

public class DialogFlowSessionTest {

    private static String VALID_PROJECT_ID = VariableLoaderHelper.getJarvisDialogFlowProject();

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
        checkDialogFlowSession(session, sessionName);
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
        checkDialogFlowSession(session, sessionName);
    }

    @Test
    public void constructConfigurationWithContextProperty() {
        SessionName sessionName = getValidSessionName();
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JarvisContext.VARIABLE_TIMEOUT_KEY, 10);
        session = new DialogFlowSession(sessionName, configuration);
        checkDialogFlowSession(session, sessionName);
        softly.assertThat(session.getJarvisContext().getVariableTimeout()).as("Valid JarvisContext variable timeout")
                .isEqualTo(10);
    }

    private SessionName getValidSessionName() {
        return SessionName.of(VALID_PROJECT_ID, "demo");
    }

    private void checkDialogFlowSession(DialogFlowSession session, SessionName expectedSessionName) {
        softly.assertThat(session.getSessionId()).as("Valid session ID").isEqualTo(expectedSessionName.toString());
        softly.assertThat(session.getSessionName()).as("Valid SessionName").isEqualTo(expectedSessionName);
        softly.assertThat(session.getJarvisContext()).as("Not null context").isNotNull();
    }
}
