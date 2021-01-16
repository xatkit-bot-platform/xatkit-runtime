package com.xatkit.stubs;

import com.xatkit.execution.State;
import com.xatkit.execution.StateContext;
import com.xatkit.execution.impl.StateContextImpl;
import com.xatkit.execution.impl.StateImpl;
import com.xatkit.intent.IntentDefinition;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A {@link StateContext} wrapper that can be configured to enable the matching of specific {@link IntentDefinition}s.
 * <p>
 * This testing context is initialized with a the {@code contextId} of the provided base {@link StateContext}. Note that
 * the {@link #getState()} method has a dedicated implementation that always returns a {@link TestingState}, which is
 * used to enable specific intent matching.
 * <p>
 * This class is used to quickly create a {@link StateContext} with a given set of intents enabled (as if they were
 * part of the current state's transition). This shortens intent provider test cases as they don't require a valid
 * bot model anymore.
 * <p>
 * Instances of this class are typically created with {@link TestingStateContextFactory#wrap(StateContext)}.
 * <p>
 * <b>Warning:</b> this class is not a complete decorator, it only reuses the {@code contextId} of the provided base
 * {@link StateContext}. Other attributes such as the configuration or the session are not duplicated nor
 * synchronized with the base context.
 *
 * @see #enableIntents(IntentDefinition...)
 */
public class TestingStateContext extends StateContextImpl implements StateContext {

    /**
     * Creates an empty {@link TestingStateContext} wrapping the provided {@code base}.
     * <p>
     * A new {@link TestingStateContext} does not enable any {@link IntentDefinition} by default, use
     * {@link #enableIntents(IntentDefinition...)} to configure the context
     * according to your test case.
     *
     * @see #enableIntents(IntentDefinition...)
     */
    public TestingStateContext(StateContext base) {
        /*
         * We are just reusing the contextId, we don't do a complete delegate pattern as in
         * DialogFlowTestingStateContext, because the StateContext interface is very dense (inherited from EMF).
         */
        this.contextId = base.getContextId();
        this.state = new TestingState();
    }

    /**
     * Enables the provided {@code intents} to be matched by
     * an {@link com.xatkit.core.recognition.IntentRecognitionProvider}.
     * <p>
     * {@link com.xatkit.core.recognition.IntentRecognitionProvider}s can only match intents that are accessed in the
     * transitions of the current state (using the {@code intentIs() predicate}. This method configures the context
     * to fake this behavior.
     *
     * @param intents the {@link IntentDefinition}s to enable
     * @throws NullPointerException if the provided {@code intents} array is {@code null}
     */
    public void enableIntents(@NonNull IntentDefinition... intents) {
        ((TestingState) this.state).enableIntents(Arrays.asList(intents));
    }

    /**
     * The fake {@link State} used to enable {@link IntentDefinition} from the {@link TestingStateContext}.
     * <p>
     * This class is configured with {@link #enableIntents(List)}, and returns the provided list when calling
     * {@link #getAllAccessedIntents()}. This effectively simulates that the {@link IntentDefinition}s are used in
     * the transitions of the state, enabling their matching by the
     * {@link com.xatkit.core.recognition.IntentRecognitionProvider}.
     *
     * @see #enableIntents(List)
     */
    private static class TestingState extends StateImpl implements State {

        /**
         * The name of the testing state.
         * <p>
         * This value is set by default when constructing an instance of this class. A {@link TestingState} is not
         * contained in a state machine, so there is no point to configure its name: the current state will always be
         * the same instance of {@link TestingState} as long as the test manipulates a {@link TestingStateContext}.
         */
        private static final String STATE_NAME = "TestingState";

        /**
         * The list of {@link IntentDefinition}s that are enabled by this state.
         * <p>
         * This list is set with {@link #enableIntents(List)}, and is returned by the state when calling
         * {@link #getAllAccessedIntents()}. This effectively simulates that the {@link IntentDefinition}s are used
         * in the transitions of the state, enabling their matching.
         *
         * @see #enableIntents(List)
         * @see #getAllAccessedIntents()
         */
        private List<IntentDefinition> enabledIntents;

        /**
         * Creates an empty {@link TestingState}.
         * <p>
         * The created state name is set with {@link #STATE_NAME}. A new {@link TestingState} does not enable any
         * {@link IntentDefinition} by default, use {@link #enableIntents(List)} to configure the state according to
         * your test case. This is typically done through
         * {@link TestingStateContext#enableIntents(IntentDefinition...)}.
         *
         * @see #enableIntents(List)
         * @see TestingStateContext#enableIntents(IntentDefinition...)
         */
        public TestingState() {
            this.name = STATE_NAME;
            this.enabledIntents = new ArrayList<>();
        }

        /**
         * Enables the provided {@code intents} to be matched by an
         * {@link com.xatkit.core.recognition.IntentRecognitionProvider}.
         * <p>
         * The provided {@code intents} are returned when calling {@link #getAllAccessedIntents()}, simulating that
         * they are used in the state's transitions.
         *
         * @param intents the {@link IntentDefinition}s to enable
         * @throws NullPointerException if the provided list is {@code null}
         * @see #getAllAccessedIntents()
         */
        public void enableIntents(@NonNull List<IntentDefinition> intents) {
            this.enabledIntents = intents;
        }

        /**
         * Returns the list of {@link IntentDefinition}s enabled by this state.
         * <p>
         * This method returns the value provided in {@link #enableIntents(List)}, simulating that these intents are
         * used in the state's transitions.
         *
         * @return the list of {@link IntentDefinition}s enabled by this state
         */
        @Override
        public Collection<IntentDefinition> getAllAccessedIntents() {
            return this.enabledIntents;
        }
    }
}
