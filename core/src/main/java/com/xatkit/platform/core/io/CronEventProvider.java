package com.xatkit.platform.core.io;

import com.xatkit.core.XatkitException;
import com.xatkit.core.platform.io.RuntimeEventProvider;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.EventInstance;
import com.xatkit.intent.IntentFactory;
import com.xatkit.platform.core.CorePlatform;
import com.xatkit.platform.core.CoreUtils;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;
import org.apache.commons.configuration2.Configuration;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.xatkit.dsl.DSL.event;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class CronEventProvider extends RuntimeEventProvider<CorePlatform> {

    /**
     * The initial delay (in seconds) to wait before generating the first {@code CronTick} event.
     */
    protected long initialDelay;

    /**
     * The interval between the generation of two {@code CronTick} events.
     * <p>
     * <b>Note</b>: this interval cannot be smaller than {@code 1} second.
     */
    protected long period;

    /**
     * The scheduler used to queue event generation tasks.
     */
    protected ScheduledExecutorService scheduler;

    /**
     * The handler allowing to stop event generation when closing this provider.
     */
    protected ScheduledFuture<?> handle;

    /**
     * Constructs an <b>unstarted</b> {@link CronEventProvider} and binds it to the provided {@code corePlatform}.
     * <p>
     * This provider is automatically started during the bot deployment using {@link #start(Configuration)}.
     *
     * @param corePlatform the {@link CorePlatform} managing this provider
     */
    public CronEventProvider(CorePlatform corePlatform) {
        super(corePlatform);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The provided {@code configuration} can specify the date/time of the first generated event, as well as the
     * period, with the {@link CoreUtils#CRON_START_ON_KEY} and {@link CoreUtils#CRON_PERIOD_KEY} properties,
     * respectively. Note that the precision of this provider is {@code 1} second, meaning that {@code CronTick}
     * events can not be generated faster than 1/second, and that the maximum precision of the date/time of the
     * initial event sent is 1 second.
     * <p>
     * If the {@code configuration} does not specify these properties the provider is started with {@code startOn
     * =Instant.now()} and will generate a single event ({@code period=-1}).
     * <p>
     * <b>Note</b>: if specified, the starting date/time must follow the ISO_DATE_TIME format (it should be parsable
     * by {@link DateTimeFormatter#ISO_DATE_TIME}.
     */
    @Override
    public void start(@NonNull Configuration configuration) {
        this.xatkitCore = runtimePlatform.getXatkitCore();
        checkNotNull(configuration, "Cannot construct a %s with the provided %s %s", this.getClass().getSimpleName(),
                Configuration.class.getSimpleName(), configuration);
        String cronStartTimeProperty = configuration.getString(CoreUtils.CRON_START_ON_KEY);
        if (isNull(cronStartTimeProperty)) {
            initialDelay = 0;
        } else {
            try {
                ZonedDateTime cronStartTime = ZonedDateTime.parse(cronStartTimeProperty,
                        DateTimeFormatter.ISO_DATE_TIME);
                initialDelay = (cronStartTime.toEpochSecond() - Instant.now().getEpochSecond());
            } catch (DateTimeParseException e) {
                throw new XatkitException(MessageFormat.format("Cannot parse the provided start date {0}, the date " +
                        "does not follow the ISO_DATE_TIME convention", cronStartTimeProperty), e);
            }
        }
        period = configuration.getLong(CoreUtils.CRON_PERIOD_KEY, -1);
        scheduler = Executors.newScheduledThreadPool(1);
        this.runtimePlatform.startEventProvider(this);
    }

    /**
     * Starts the provider and initializes its scheduler with the given properties.
     * <p>
     * This method is not blocking: the event generation process is handled by a dedicated {@link Thread}. This
     * {@link Thread} should be stopped by calling {@link #close()} when event generation is no longer required.
     *
     * @see #close()
     */
    @Override
    public void run() {
        Log.info("Starting {0} with initialDelay={1}s, period={2}s", this.getClass().getSimpleName(), initialDelay,
                period);
        final Runnable cronTickCreator = () -> {
            try {
                EventInstance cronTickEvent = IntentFactory.eINSTANCE.createEventInstance();
                cronTickEvent.setDefinition(CronTick);
                XatkitSession cronSession = new XatkitSession("cron");
                this.sendEventInstance(cronTickEvent, cronSession);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        if (period > 0) {
            handle = scheduler.scheduleAtFixedRate(cronTickCreator, initialDelay, period, TimeUnit.SECONDS);
        } else {
            handle = scheduler.schedule(cronTickCreator, initialDelay, TimeUnit.SECONDS);
        }
    }

    /**
     * Interrupt the ongoing generation tasks and shutdown the associated scheduler.
     * <p>
     * This method <b>does not wait</b> for the underlying task to complete, and will interrupt it to close the
     * scheduler.
     */
    @Override
    public void close() {
        super.close();
        if (nonNull(handle) && !handle.isCancelled()) {
            handle.cancel(true);
        }
        if (nonNull(scheduler) && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    public static EventDefinition CronTick = event("CronTick")
            .getEventDefinition();
}
