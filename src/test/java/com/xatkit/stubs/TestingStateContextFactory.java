package com.xatkit.stubs;

import com.xatkit.core.recognition.dialogflow.DialogFlowStateContext;
import com.xatkit.execution.StateContext;
import lombok.NonNull;

/**
 * Creates instances of {@link TestingStateContext} from {@link StateContext} instances.
 * <p>
 * This factory is typically used to wrap a {@link StateContext} created by an
 * {@link com.xatkit.core.recognition.IntentRecognitionProvider} into a {@link TestingStateContext} that provides
 * additional methods to customize the intents that can be matched by the provider.
 * <p>
 * Using {@link TestingStateContext}s allows to quickly create valid {@link StateContext} instances with a given set
 * of intents enabled (as if they were part of the current state's transitions). This shorten intent provider test
 * cases as they don't require a valid bot model anymore.
 * <p>
 * <b>Usage Example</b>
 * <pre>
 * {@code
 * IntentRecognitionProvider provider = getProvider();
 * StateContext baseContext = provider.createContext("myContext");
 * TestingStateContext testingContext = TestingStateContextFactory.wrap(baseContext);
 * testingContext.enableIntent(myIntent);
 * provider.getIntent("input", testingContext);
 * // check the result
 * }
 * </pre>
 */
public class TestingStateContextFactory {

    /**
     * Wraps the provided {@code stateContext} into a {@link TestingStateContext}.
     *
     * @param stateContext the {@link StateContext} to wrap
     * @return the created {@link TestingStateContext}
     * @throws NullPointerException if the provided {@code stateContext} is {@code null}
     */
    public static TestingStateContext wrap(@NonNull StateContext stateContext) {
        return new TestingStateContext(stateContext);
    }

    /**
     * Wraps the provided {@code stateContext} into a {@link TestingStateContext}.
     *
     * @param stateContext the {@link StateContext} to wrap
     * @return the created {@link TestingStateContext}
     * @throws NullPointerException if the provided {@code stateContext} is {@code null}
     */
    public static TestingStateContext wrap(DialogFlowStateContext stateContext) {
        return new DialogFlowTestingStateContext(stateContext);
    }
}
