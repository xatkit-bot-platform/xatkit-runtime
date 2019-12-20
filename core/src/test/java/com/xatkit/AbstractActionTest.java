package com.xatkit;

import com.xatkit.core.XatkitCore;
import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.stubs.StubXatkitCore;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import static java.util.Objects.nonNull;

/**
 * A generic test case that defines utility methods to test {@link RuntimeAction} subclasses.
 * <p>
 * Test cases targeting {@link RuntimeAction}s can extend this class to reuse the initialized {@link RuntimePlatform}
 * , an empty {@link XatkitSession}, as well as a {@link StubXatkitCore} instance. This class takes care of the
 * life-cycle of the initialized {@link RuntimePlatform} and {@link XatkitCore}.
 *
 * @param <A> the {@link RuntimeAction} {@link Class} under test
 * @param <P> the {@link RuntimePlatform} containing the action under test
 */
public abstract class AbstractActionTest<A extends RuntimeAction<P>, P extends RuntimePlatform> extends AbstractXatkitTest {

    /**
     * The {@link StubXatkitCore} used to initialize the {@link RuntimePlatform}.
     * <p>
     * This field is static, meaning that the same {@link StubXatkitCore} instance will be used for all the tests.
     */
    protected static StubXatkitCore XATKIT_CORE;

    /**
     * The {@link RuntimePlatform} instance containing the action under test.
     */
    protected P platform;

    /**
     * The {@link RuntimeAction} instance under test.
     */
    protected A action;

    /**
     * An empty {@link XatkitSession} that can be used to create instances of the action under test.
     * <p>
     * This session is reset before each test.
     */
    protected XatkitSession emptySession;

    /**
     * Initializes the {@link StubXatkitCore} instance.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        XATKIT_CORE = new StubXatkitCore();
    }

    /**
     * Shutdown the {@link StubXatkitCore} instance.
     */
    @AfterClass
    public static void tearDownAfterClass() {
        if (nonNull(XATKIT_CORE) && !XATKIT_CORE.isShutdown()) {
            XATKIT_CORE.shutdown();
        }
    }

    /**
     * Initializes the {@link RuntimePlatform} and the empty {@link XatkitSession}.
     */
    @Before
    public void setUp() {
        platform = getPlatform();
        emptySession = new XatkitSession("session");
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
