package com.xatkit.core.platform;

import com.xatkit.AbstractPlatformTest;
import com.xatkit.core.platform.io.RuntimeEventProvider;
import com.xatkit.core.platform.io.WebhookEventProvider;
import com.xatkit.core.server.XatkitServer;
import com.xatkit.stubs.EmptyRuntimePlatform;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RuntimePlatformTest extends AbstractPlatformTest<RuntimePlatform> {

    private RuntimeEventProvider mockedEventProvider;

    private WebhookEventProvider mockedWebhookProvider;

    private XatkitServer mockedXatkitServer;

    @Before
    public void setUp() {
        super.setUp();
        this.mockedEventProvider = mock(RuntimeEventProvider.class);
        this.mockedWebhookProvider = mock(WebhookEventProvider.class);
        this.mockedXatkitServer = mock(XatkitServer.class);
        when(mockedXatkitCore.getXatkitServer()).thenReturn(mockedXatkitServer);
        doAnswer(invocationOnMock -> {
            /*
             * We need to wait here to be sure the provider's Thread is running (otherwise the computation finishes
             * before we can check it)
             */
            synchronized (this) {
                try {
                    wait();
                } catch(InterruptedException e) {

                }
            }
            return null;
        }).when(mockedEventProvider).run();
        doAnswer(invocationOnMock -> {
            synchronized (this) {
                try {
                    wait();
                } catch(InterruptedException e) {

                }
            }
            return null;
        }).when(mockedWebhookProvider).run();
        this.platform = getPlatform();
    }

    @Test
    public void getName() {
        assertThat(platform.getName()).as("Valid runtimePlatform name").isEqualTo("EmptyRuntimePlatform");
    }

    @Test
    public void getConfigurationNotStarted() {
        assertThat(platform.getConfiguration()).isNull();
    }

    @Test
    public void getConfigurationStarted() {
        platform.start(mockedXatkitCore, configuration);
        assertThat(platform.getConfiguration()).as("Not null Configuration").isNotNull();
    }

    @Test
    public void getXatkitCoreNotStarted() {
        assertThat(platform.getXatkitCore()).isNull();
    }

    @Test
    public void getXatkitCoreStarted() {
        platform.start(mockedXatkitCore, configuration);
        assertThat(platform.getXatkitCore()).as("Not null XatkitCore").isNotNull();
        assertThat(platform.getXatkitCore()).as("Valid XatkitCore").isEqualTo(mockedXatkitCore);
    }

    @Test(expected = NullPointerException.class)
    public void startEventProviderNullEventProviderDefinition() {
        platform.start(mockedXatkitCore, configuration);
        platform.startEventProvider(null);
    }

    @Test
    public void startEventProvider() {
        platform.start(mockedXatkitCore, configuration);
        platform.startEventProvider(mockedEventProvider);
        assertThat(platform.eventProviderMap).containsKey(mockedEventProvider.getClass().getSimpleName());
        RuntimePlatform.EventProviderThread eventProviderThread =
                platform.eventProviderMap.get(mockedEventProvider.getClass().getSimpleName());
        assertThat(eventProviderThread.getRuntimeEventProvider()).isInstanceOf(RuntimeEventProvider.class);
        assertThat(eventProviderThread.isAlive()).isTrue();
    }

    @Test
    public void startValidEventProviderWebhook() {
        platform.start(mockedXatkitCore, configuration);
        platform.startEventProvider(mockedWebhookProvider);
        assertThat(platform.eventProviderMap).containsKey(mockedWebhookProvider.getClass().getSimpleName());
        RuntimePlatform.EventProviderThread eventProviderThread =
                platform.eventProviderMap.get(mockedWebhookProvider.getClass().getSimpleName());
        assertThat(eventProviderThread.getRuntimeEventProvider()).isInstanceOf(WebhookEventProvider.class);
        assertThat(eventProviderThread.isAlive()).isTrue();
        verify(mockedXatkitServer).registerWebhookEventProvider(mockedWebhookProvider);
    }

    @Test
    public void shutdownRegisteredEventProviderAndActionDefinition() {
        platform.start(mockedXatkitCore, configuration);
        platform.startEventProvider(mockedEventProvider);
        // Enables the actionDefinition in the RuntimePlatform
        platform.shutdown();
        assertThat(platform.getEventProviderMap()).as("Empty RuntimeEventProvider map").isEmpty();
    }

    public RuntimePlatform getPlatform() {
        return new EmptyRuntimePlatform();
    }
}
