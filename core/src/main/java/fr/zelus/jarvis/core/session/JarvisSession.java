package fr.zelus.jarvis.core.session;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;

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
     * Constructs a new, empty {@link JarvisSession} with the provided {@code sessionId}.
     * See {@link #JarvisSession(String, Configuration)} to construct a {@link JarvisSession} with a given
     * {@link Configuration}.
     *
     * @param sessionId the unique identifier of the {@link JarvisSession}
     */
    public JarvisSession(String sessionId) {
        this(sessionId, new BaseConfiguration());
    }

    /**
     * Constructs a new, empty {@link JarvisSession} with the provided {@code sessionId} and {@code configuration}.
     * <p>
     * This constructor forwards the provided {@link Configuration} to the underlying {@link JarvisContext} and can
     * be used to customize {@link JarvisContext} properties.
     *
     * @param sessionId     the unique identifier of the {@link JarvisSession}
     * @param configuration the {@link Configuration} parameterizing the {@link JarvisSession}
     * @throws NullPointerException if the provided {@code sessionId} or {@code configuration} is {@code null}
     */
    public JarvisSession(String sessionId, Configuration configuration) {
        checkNotNull(sessionId, "Cannot construct a %s with the session Id %s", JarvisSession.class.getSimpleName(),
                sessionId);
        checkNotNull(configuration, "Cannot construct a %s with the provided %s: %s", JarvisSession.class
                .getSimpleName(), Configuration.class.getSimpleName(), configuration);
        this.sessionId = sessionId;
        this.jarvisContext = new JarvisContext(configuration);
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
