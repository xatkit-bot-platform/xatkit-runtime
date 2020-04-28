package com.xatkit;

import com.xatkit.core.XatkitCore;
import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.io.RuntimeEventProvider;
import com.xatkit.core.recognition.IntentRecognitionProvider;
import com.xatkit.core.session.XatkitSession;
import org.junit.After;
import org.junit.Before;

import static java.util.Objects.nonNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A generic test case that defines utility methods to test {@link RuntimeEventProvider} subclasses.
 * <p>
 * Test cases targeting {@link RuntimeEventProvider}s can extend this class to reuse the initialized
 * {@link RuntimePlatform} and a mocked {@link XatkitCore} instance. This class takes care of the life-cycle of the
 * initialized {@link RuntimePlatform} and {@link XatkitCore}.
 *
 * @param <E> the {@link RuntimeEventProvider} {@link Class} under test
 * @param <P> the {@link RuntimePlatform} containing the provider under test
 */
public abstract class AbstractEventProviderTest<E extends RuntimeEventProvider<P>, P extends RuntimePlatform> extends AbstractXatkitTest {

    /**
     * The {@link RuntimePlatform} instance containing the provider under test.
     */
    protected P platform;

    /**
     * The {@link RuntimeEventProvider} instance under test.
     */
    protected E provider;

    /**
     * A mock of the {@link XatkitCore}.
     */
    protected XatkitCore mockedXatkitCore;

    /**
     * A mock of the {@link IntentRecognitionProvider}.
     * <p>
     * This mock is returned when calling {@code mockedXatkitCore.getIntentRecognitionProvider()}.
     */
    protected IntentRecognitionProvider mockedIntentRecognitionProvider;

    /**
     * Initializes the {@link RuntimePlatform} and the empty {@link XatkitSession}.
     */
    @Before
    public void setUp() {
        mockedXatkitCore = mock(XatkitCore.class);
        mockedIntentRecognitionProvider = mock(IntentRecognitionProvider.class);
        when(mockedXatkitCore.getIntentRecognitionProvider()).thenReturn(mockedIntentRecognitionProvider);
        platform = getPlatform();
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
