package com.xatkit.platform.core;

/**
 * An utility interface that holds Core-related properties.
 */
public interface CoreUtils {

    /**
     * The {@link org.apache.commons.configuration2.Configuration} key to specify the date/time when the
     * {@link com.xatkit.platform.core.io.CronEventProvider} should start generating {@code CronTick} events.
     * <p>
     * <b>Note</b>: the starting date/time must follow the ISO_DATE_TIME format (it should be parsable
     * by {@link java.time.format.DateTimeFormatter#ISO_DATE_TIME}.
     */
    String CRON_START_ON_KEY = "xatkit.core.cron.start_on";

    /**
     * The {@link org.apache.commons.configuration2.Configuration} key to specify the interval between two {@code
     * CronTick} event generation (in seconds).
     * <p>
     * The precision of the period is {@code 1} second, meaning that {@code CronTick} events can not be generated
     * faster than 1/second
     */
    String CRON_PERIOD_KEY = "xatkit.core.cron.period";
}
