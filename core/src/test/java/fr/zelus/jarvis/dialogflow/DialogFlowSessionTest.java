package fr.zelus.jarvis.dialogflow;

import com.google.cloud.dialogflow.v2.SessionName;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

public class DialogFlowSessionTest {

    private static String VALID_PROJECT_ID = "jarvis-fd96e";

    private DialogFlowSession session;

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test(expected = NullPointerException.class)
    public void constructNullSessionName() {
        session = new DialogFlowSession(null);
    }

    @Test
    public void constructValidSessionName() {
        SessionName sessionName = SessionName.of(VALID_PROJECT_ID, "demo");
        session = new DialogFlowSession(sessionName);
        softly.assertThat(session.getSessionId()).as("Valid session ID").isEqualTo(sessionName.toString());
        softly.assertThat(session.getSessionName()).as("Valid SessionName").isEqualTo(sessionName);
        softly.assertThat(session.getJarvisContext()).as("Not null context").isNotNull();
    }
}
