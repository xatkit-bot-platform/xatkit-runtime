package com.xatkit.stubs;

import com.google.cloud.dialogflow.v2.SessionName;
import com.xatkit.core.recognition.dialogflow.DialogFlowStateContext;
import com.xatkit.intent.IntentDefinition;
import lombok.NonNull;

/**
 * A {@link DialogFlowStateContext} decorator that can be configured to enable the matching of specific
 * {@link com.xatkit.intent.IntentDefinition}s.
 * <p>
 * This testing context wraps a {@link DialogFlowStateContext} instance and delegates DialogFlow-specific operations
 * to it (e.g. {@link #getSessionName()}). This wrapping is required because
 * {@link com.xatkit.core.recognition.dialogflow.DialogFlowClients} only works with valid non-stubbed DialogFlow
 * {@link SessionName}s.
 * <p>
 * See the documentation of {@link TestingStateContext} for more information on intent matching configuration.
 * <p>
 * Instances of this class are typically created with {@link TestingStateContextFactory#wrap(DialogFlowStateContext)}.
 *
 * @see TestingStateContext
 */
public class DialogFlowTestingStateContext extends TestingStateContext implements DialogFlowStateContext {

    /**
     * The base {@link DialogFlowStateContext} used to delegate DialogFlow-specific operations.
     */
    private DialogFlowStateContext base;

    /**
     * Creates an empty {@link DialogFlowTestingStateContext} wrapping the provided {@code base}.
     * <p>
     * A new {@link DialogFlowTestingStateContext} does not enable any {@link com.xatkit.intent.IntentDefinition} by
     * default, use {@link #enableIntents(IntentDefinition...)} to configure the context according to your use case.
     *
     * @param base the {@link DialogFlowStateContext} to wrap
     * @throws NullPointerException if the provided {@code base} is {@code null}
     * @see #enableIntents(IntentDefinition...)
     */
    public DialogFlowTestingStateContext(@NonNull DialogFlowStateContext base) {
        super(base);
        this.base = base;
    }

    /**
     * Returns the {@link SessionName} of the wrapped {@link DialogFlowStateContext}.
     * <p>
     * This method ensures that a {@link DialogFlowTestingStateContext} can be used as a valid
     * {@link DialogFlowStateContext} by the DialogFlow provider.
     *
     * @return the {@link SessionName} of the wrapped {@link DialogFlowStateContext}
     */
    @Override
    public SessionName getSessionName() {
        return base.getSessionName();
    }
}
