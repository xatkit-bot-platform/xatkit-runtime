package com.xatkit;

import com.xatkit.core.XatkitBot;
import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.StateContext;
import org.junit.After;
import org.junit.Before;

import static java.util.Objects.nonNull;
import static org.mockito.Mockito.mock;

/**
 * A generic test case that defines utility methods to test {@link RuntimeAction} subclasses.
 * <p>
 * Test cases targeting {@link RuntimeAction}s can extend this class to reuse the initialized {@link RuntimePlatform}
 * , an empty {@link StateContext}, as well as a mocked {@link XatkitBot} instance. This class takes care of the
 * life-cycle of the initialized {@link RuntimePlatform} and {@link XatkitBot}.
 *
 * @param <A> the {@link RuntimeAction} {@link Class} under test
 * @param <P> the {@link RuntimePlatform} containing the action under test
 */
public abstract class AbstractActionTest<A extends RuntimeAction<P>, P extends RuntimePlatform> extends AbstractXatkitTest {

    /**
     * The {@link RuntimePlatform} instance containing the action under test.
     */
    protected P platform;

    /**
     * The {@link RuntimeAction} instance under test.
     */
    protected A action;

    /**
     * A mock of the {@link XatkitBot}.
     */
    protected XatkitBot mockedXatkitBot;

    /**
     * An empty {@link StateContext} that can be used to create instances of the action under test.
     * <p>
     * This context is reset before each test.
     */
    protected StateContext context;

    /**
     * Initializes the {@link RuntimePlatform} and the empty {@link StateContext}.
     */
    @Before
    public void setUp() {
        mockedXatkitBot = mock(XatkitBot.class);
        platform = getPlatform();
        context = ExecutionFactory.eINSTANCE.createStateContext();
        /*
         * This context was previously a XatkitSession instance. We keep the previous name in case some test cases
         * rely on it.
         */
        context.setContextId("session");
    }

    /**
     * Shutdown the {@link RuntimePlatform} containing the action under test.
     */
    @After
    public void tearDown() {
        if (nonNull(platform)) {
            platform.shutdown();
        }
    }

    /**
     * Returns an instance of the {@link RuntimePlatform} containing the action under test.
     * <p>
     * This method must be implemented by subclasses and return a valid instance of {@link RuntimePlatform}. This
     * method is called before each test case to create a fresh {@link RuntimePlatform} instance.
     *
     * @return an instance of the {@link RuntimePlatform} containing the action under test
     */
    protected abstract P getPlatform();
}
