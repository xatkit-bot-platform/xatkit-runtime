package fr.zelus.jarvis.dialogflow;

import com.google.cloud.dialogflow.v2.SessionName;
import fr.zelus.jarvis.core.session.JarvisSession;

/**
 * A DialogFlow {@link JarvisSession} implementation that relies on DialogFlow internal sessions.
 * <p>
 * This class computes the unique identifier of the session by using the internal DialogFlow API. The raw session can
 * be accessed by calling {@link #getSessionName()}.
 */
public class DialogFlowSession extends JarvisSession {

    /**
     * The raw DialogFlow session.
     */
    private SessionName sessionName;

    /**
     * Constructs a new {@link DialogFlowSession} from the provided {@code sessionName}.
     * <p>
     * This constructor sets the {@code sessionId} value by calling {@link SessionName#toString()}, that may not be
     * unique in some rare cases. Use {@link #getSessionName()} to compare {@link DialogFlowSession}s.
     *
     * @param sessionName the raw DialogFlow session
     */
    public DialogFlowSession(SessionName sessionName) {
        super(sessionName.toString());
        this.sessionName = sessionName;
    }

    /**
     * Returns the raw DialogFlow session.
     *
     * @return the raw DialogFlow session
     */
    public SessionName getSessionName() {
        return sessionName;
    }
}
