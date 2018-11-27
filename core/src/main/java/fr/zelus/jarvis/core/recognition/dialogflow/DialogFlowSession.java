package fr.zelus.jarvis.core.recognition.dialogflow;

import com.google.cloud.dialogflow.v2.SessionName;
import fr.zelus.jarvis.core.session.JarvisSession;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;

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
     * <p>
     * See {@link #DialogFlowSession(SessionName, Configuration)} to construct a {@link DialogFlowSession} with a
     * given {@link Configuration}.
     *
     * @param sessionName the raw DialogFlow session
     */
    public DialogFlowSession(SessionName sessionName) {
        this(sessionName, new BaseConfiguration());
    }

    /**
     * Constructs a new {@link DialogFlowSession} from the provided {@code sessionName} and {@code configuration}.
     * <p>
     * This constructor sets the {@code sessionId} value by calling {@link SessionName#toString()}, that may not be
     * unique in some rare cases? Use {@link #getSessionName()} to compare {@link DialogFlowSession}s.
     *
     * @param sessionName   the raw DialogFlow session
     * @param configuration the {@link Configuration} parameterizing the {@link DialogFlowSession}
     * @see JarvisSession#JarvisSession(String, Configuration)
     */
    public DialogFlowSession(SessionName sessionName, Configuration configuration) {
        super(sessionName.toString(), configuration);
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
