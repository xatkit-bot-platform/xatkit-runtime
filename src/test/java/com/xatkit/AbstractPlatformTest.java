package com.xatkit;

import com.xatkit.core.XatkitBot;
import com.xatkit.core.platform.RuntimePlatform;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.After;
import org.junit.Before;

import static java.util.Objects.nonNull;
import static org.mockito.Mockito.mock;

/**
 * A generic test case that defines utility methods to test {@link RuntimePlatform} subclasses.
 * <p>
 * Test cases targeting {@link RuntimePlatform}s can extend this class to reuse a mocked {@link XatkitBot}, and an
 * initialized {@link Configuration}.
 *
 * @param <P> the {@link RuntimePlatform} {@link Class} under test
 */
public class AbstractPlatformTest<P extends RuntimePlatform> extends AbstractXatkitTest {

    /**
     * The {@link RuntimePlatform} under test
     */
    protected P platform;

    /**
     * A mock of the {@link XatkitBot}.
     */
    protected XatkitBot mockedXatkitBot;

    /**
     * A default configuration that can be reused by subclasses.
     */
    protected Configuration configuration;

    /**
     * Initializes the mocked {@link XatkitBot} and the default {@link Configuration}.
     */
    @Before
    public void setUp() {
        mockedXatkitBot = mock(XatkitBot.class);
        configuration = new BaseConfiguration();
    }

    /**
     * Shutdowns the {@link RuntimePlatform}.
     */
    @After
    public void tearDown() {
        if(nonNull(platform)) {
            platform.shutdown();
        }
    }
}
