package com.xatkit.core.recognition.dialogflow;

import com.google.cloud.dialogflow.v2.SessionName;
import com.xatkit.execution.impl.StateContextImpl;
import lombok.NonNull;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ConfigurationConverter;

/**
 * A DialogFlow context implementation that relies on DialogFlow internal sessions.
 * <p>
 * This class computes the unique identifier of the session by using the internal DialogFlow API. The raw session can
 * be accessed by calling {@link #getSessionName()}.
 */
public class DialogFlowStateContextImpl extends StateContextImpl implements DialogFlowStateContext {

    /**
     * The raw DialogFlow session.
     */
    private SessionName sessionName;

    /**
     * Constructs a new {@link DialogFlowStateContextImpl} from the provided {@code sessionName}.
     * <p>
     * This constructor sets the {@code sessionId} value by calling {@link SessionName#toString()}, that may not be
     * unique in some rare cases. Use {@link #getSessionName()} to compare {@link DialogFlowStateContextImpl}s.
     * <p>
     * See {@link #DialogFlowStateContextImpl(SessionName, Configuration)} to construct a
     * {@link DialogFlowStateContextImpl} with a given {@link Configuration}.
     *
     * @param sessionName the raw DialogFlow session
     */
    public DialogFlowStateContextImpl(SessionName sessionName) {
        this(sessionName, new BaseConfiguration());
    }

    /**
     * Constructs a new {@link DialogFlowStateContextImpl} from the provided {@code sessionName} and {@code
     * configuration}.
     * <p>
     * This constructor sets the {@code sessionId} value by calling {@link SessionName#toString()}, that may not be
     * unique in some rare cases? Use {@link #getSessionName()} to compare {@link DialogFlowStateContextImpl}s.
     *
     * @param sessionName   the raw DialogFlow session
     * @param configuration the {@link Configuration} parameterizing the {@link DialogFlowStateContextImpl}
     */
    public DialogFlowStateContextImpl(@NonNull SessionName sessionName, @NonNull Configuration configuration) {
        super();
        this.setContextId(sessionName.toString());
        this.setConfiguration(ConfigurationConverter.getMap(configuration));
        this.sessionName = sessionName;
    }

    /**
     * {@inheritDoc}
     */
    public SessionName getSessionName() {
        return sessionName;
    }
}
