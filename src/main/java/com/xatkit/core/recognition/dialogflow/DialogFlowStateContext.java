package com.xatkit.core.recognition.dialogflow;

import com.google.cloud.dialogflow.v2.SessionName;
import com.xatkit.execution.StateContext;

/**
 * A DialogFlow {@link StateContext} that managing an internal DialogFlow {@link SessionName}.
 * <p>
 * This context provides a direct access to the DialogFlow {@link SessionName} instance, that is required by
 * {@link DialogFlowClients} to access the remote agent.
 */
public interface DialogFlowStateContext extends StateContext {

    /**
     * Returns the raw DialogFlow session.
     *
     * @return the raw DialogFlow session
     */
    SessionName getSessionName();
}
