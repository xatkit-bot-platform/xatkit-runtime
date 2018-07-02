package fr.zelus.jarvis.dialogflow;

import com.google.cloud.dialogflow.v2.SessionName;
import fr.zelus.jarvis.core.session.JarvisSession;

public class DialogFlowSession extends JarvisSession {

    private SessionName sessionName;

    public DialogFlowSession(SessionName sessionName) {
        super(sessionName.toString());
        this.sessionName = sessionName;
    }

    public SessionName getSessionName() {
        return sessionName;
    }
}
