package fr.zelus.jarvis.core.session;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * A session holding user-related information.
 * <p>
 * A {@link JarvisSession} is bound to a user, and holds all the volatile information related to the current
 * conversation. A {@link JarvisSession} contains a {@link JarvisContext}, that represents the contextual variables
 * of the current conversation.
 *
 * @see JarvisContext
 * @see fr.zelus.jarvis.core.JarvisCore#getOrCreateJarvisSession(String)
 */
public class JarvisSession {

    /**
     * The unique identifier of the {@link JarvisSession}.
     */
    private String sessionId;

    /**
     * The {@link JarvisContext} used to store context-related variables.
     */
    private JarvisContext jarvisContext;

    /**
     * Constructs a new, empty {@link JarvisSession} with the provided {@code sessionID}.
     *
     * @param sessionId the unique identifier of the {@link JarvisSession}
     * @throws NullPointerException if the provided {@code sessionId} is {@code null}
     */
    public JarvisSession(String sessionId) {
        checkNotNull(sessionId, "Cannot construct a %s with the session ID null", JarvisSession.class.getSimpleName());
        this.sessionId = sessionId;
        this.jarvisContext = new JarvisContext();
    }

    /**
     * Returns the unique identifier of the {@link JarvisSession}.
     *
     * @return the unique identifier of the {@link JarvisSession}
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Returns the session's {@link JarvisContext} holding context-related variables.
     *
     * @return the session's {@link JarvisContext} holding context-related variables
     */
    public JarvisContext getJarvisContext() {
        return jarvisContext;
    }
}
