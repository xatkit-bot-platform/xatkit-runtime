package com.xatkit;

import com.xatkit.core.XatkitCore;
import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.session.XatkitSession;
import org.junit.After;
import org.junit.Before;

import static java.util.Objects.nonNull;
import static org.mockito.Mockito.mock;

/**
 * A generic test case that defines utility methods to test {@link RuntimeAction} subclasses.
 * <p>
 * Test cases targeting {@link RuntimeAction}s can extend this class to reuse the initialized {@link RuntimePlatform}
 * , an empty {@link XatkitSession}, as well as a mocked {@link XatkitCore} instance. This class takes care of the
 * life-cycle of the initialized {@link RuntimePlatform} and {@link XatkitCore}.
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
     * A mock of the {@link XatkitCore}.
     */
    protected XatkitCore mockedXatkitCore;

    /**
     * An empty {@link XatkitSession} that can be used to create instances of the action under test.
     * <p>
     * This session is reset before each test.
     */
    protected XatkitSession session;

    /**
     * Initializes the {@link RuntimePlatform} and the empty {@link XatkitSession}.
     */
    @Before
    public void setUp() {
        mockedXatkitCore = mock(XatkitCore.class);
        platform = getPlatform();
        session = new XatkitSession("session");
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
