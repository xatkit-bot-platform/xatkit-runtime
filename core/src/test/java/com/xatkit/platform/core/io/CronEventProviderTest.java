package com.xatkit.platform.core.io;

import com.xatkit.AbstractEventProviderTest;
import com.xatkit.core.EventDefinitionRegistry;
import com.xatkit.core.ExecutionService;
import com.xatkit.core.XatkitException;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.EventInstance;
import com.xatkit.platform.core.CorePlatform;
import com.xatkit.platform.core.CoreUtils;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CronEventProviderTest extends AbstractEventProviderTest<CronEventProvider, CorePlatform> {

    private Configuration configuration;

    private ExecutionService mockedExecutionService;

    private EventDefinitionRegistry mockedEventRegistry;

    @Before
    public void setUp() {
        configuration = new BaseConfiguration();
        super.setUp();
        mockedExecutionService = mock(ExecutionService.class);
        when(mockedXatkitCore.getExecutionService()).thenReturn(mockedExecutionService);
        mockedEventRegistry = mock(EventDefinitionRegistry.class);
        when(mockedXatkitCore.getEventDefinitionRegistry()).thenReturn(mockedEventRegistry);
        when(mockedEventRegistry.getEventDefinition(CronEventProvider.CronTick.getName())).thenReturn(CronEventProvider.CronTick);
    }

    @After
    public void tearDown() {
        super.tearDown();
        if (nonNull(provider)) {
            provider.close();
        }
    }

    @Test(expected = NullPointerException.class)
    public void startNullConfiguration() {
        provider = new CronEventProvider(platform);
        provider.start(null);
    }

    @Test
    public void startNoProperties() throws InterruptedException {
        provider = new CronEventProvider(platform);
        provider.start(new BaseConfiguration());
        assertThatFieldsAreSet(provider, 0, -1);
        /*
         * Wait no more than 1s (the minimum interval that can be set on the provider). This way we can be sure that
         * the event has been immediately triggered.
         */
        Thread.sleep(500);
        assertThatXatkitCoreContainsCronTicks(1);
    }

    @Test(expected = XatkitException.class)
    public void startCustomStartOnPropertyInvalidFormat() {
        configuration.addProperty(CoreUtils.CRON_START_ON_KEY, "123");
        provider = new CronEventProvider(platform);
        provider.start(configuration);
    }

    @Test
    public void startCustomStartOnProperty() throws InterruptedException {
        configuration.addProperty(CoreUtils.CRON_START_ON_KEY, getDateTimeStringNowPlusXSecond(1));
        provider = new CronEventProvider(platform);
        provider.start(configuration);
        assertThatFieldsAreSet(provider, 1, -1);
        /*
         * Wait less than the minimum interval to ensure the event hasn't been thrown.
         */
        Thread.sleep(500);
        assertThatXatkitCoreContainsCronTicks(0);
        Thread.sleep(1000);
        assertThatXatkitCoreContainsCronTicks(1);
    }

    @Test
    public void startCustomPeriodProperty() throws InterruptedException {
        configuration.addProperty(CoreUtils.CRON_PERIOD_KEY, 1);
        provider = new CronEventProvider(platform);
        provider.start(configuration);
        assertThatFieldsAreSet(provider, 0, 1);
        /*
         * Wait less than the minimum interval to ensure the event has been thrown when starting the provider.
         * thrown.
         */
        Thread.sleep(500);
        assertThatXatkitCoreContainsCronTicks(1);
        Thread.sleep(1000);
        assertThatXatkitCoreContainsCronTicks(2);
        Thread.sleep(1000);
        assertThatXatkitCoreContainsCronTicks(3);
    }

    @Test
    public void closeProviderProperlyStarted() throws InterruptedException {
        configuration.addProperty(CoreUtils.CRON_PERIOD_KEY, 1);
        provider = new CronEventProvider(platform);
        provider.start(configuration);
        Thread.sleep(500);
        provider.close();
        assertThat(provider.scheduler.isShutdown()).as("Schedule is shutdown").isTrue();
    }

    @Override
    protected CorePlatform getPlatform() {
        CorePlatform corePlatform = new CorePlatform();
        corePlatform.start(mockedXatkitCore, configuration);
        return corePlatform;
    }

    private String getDateTimeStringNowPlusXSecond(int secondsToAdd) {
        return DateTimeFormatter.ISO_DATE_TIME
                .withZone(ZoneId.of("UTC"))
                .format(Instant.now().plus(secondsToAdd, ChronoUnit.SECONDS));
    }

    private void assertThatFieldsAreSet(CronEventProvider provider, long initialDelay, long period) {
        assertThat(provider.initialDelay).as("Initial delay is " + initialDelay).isGreaterThanOrEqualTo(initialDelay);
        assertThat(provider.period).as("Period is " + period).isEqualTo(period);
        assertThat(provider.scheduler).as("Schedule not null").isNotNull();
    }

    private void assertThatXatkitCoreContainsCronTicks(int cronTickCount) {
        ArgumentCaptor<EventInstance> captor = ArgumentCaptor.forClass(EventInstance.class);
        verify(mockedExecutionService, times(cronTickCount)).handleEventInstance(captor.capture(),
                any(XatkitSession.class));
        assertThat(captor.getAllValues()).allMatch(e -> e.getDefinition().getName().equals("CronTick"));
    }
}
