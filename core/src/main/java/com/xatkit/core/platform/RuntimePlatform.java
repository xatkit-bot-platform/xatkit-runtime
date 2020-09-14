package com.xatkit.core.platform;

import com.xatkit.core.XatkitBot;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.platform.io.RuntimeEventProvider;
import com.xatkit.core.platform.io.WebhookEventProvider;
import com.xatkit.core.server.XatkitServer;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;
import org.apache.commons.configuration2.Configuration;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A convenience wrapper to store platform-specific data.
 * <p>
 * This class is typically used to store platform-level credentials, OAuth tokens, or database accesses.
 * {@link RuntimeAction}s bound to this platform have a direct reference to the platform, allowing to easily access
 * and share information across actions.
 */
public abstract class RuntimePlatform {

    /**
     * The {@link XatkitBot} instance managing this platform.
     */
    protected XatkitBot xatkitBot;

    /**
     * The {@link Configuration} used to initialize this class.
     * <p>
     * This {@link Configuration} is used by the {@link RuntimePlatform} to initialize the
     * {@link RuntimeEventProvider}s and
     * {@link RuntimeAction}s.
     *
     * @see #startEventProvider(RuntimeEventProvider)
     */
    protected Configuration configuration;

    /**
     * The {@link Map} containing the {@link EventProviderThread}s associated to this platform.
     * <p>
     * This {@link Map} filled when new {@link RuntimeEventProvider}s are started (see
     * {@link #startEventProvider(RuntimeEventProvider)}), and is used to cache
     * {@link EventProviderThread}s and stop them when the platform is {@link #shutdown()}.
     *
     * @see #shutdown()
     */
    protected Map<String, EventProviderThread> eventProviderMap = new HashMap<>();

    /**
     * Constructs an <b>unstarted</b> instance of this platform.
     * <p>
     * This constructor doesn't have access to the {@link XatkitBot} nor the {@link Configuration}: it is typically
     * called when defining a bot to have a usable reference to call actions on, but it is initialized during the bot
     * deployment using the {@link RuntimePlatform#start(XatkitBot, Configuration)} method.
     *
     * @see #start(XatkitBot, Configuration)
     */
    public RuntimePlatform() {
    }

    /**
     * Starts the platform.
     * <p>
     * This method binds the {@code xatkitBot} and {@code configuration} instances associated to the current bot to
     * the platform. Subclasses typically override this method to initialize their internal data structure (e.g.
     * retrieve an authentication token from the configuration and start a client library with it).
     * <p>
     * This method is automatically called by Xatkit when a bot is starting.
     *
     * @param xatkitBot    the {@link XatkitBot} instance managing this platform
     * @param configuration the {@link Configuration} of the bot currently run
     */
    public void start(@NonNull XatkitBot xatkitBot, @NonNull Configuration configuration) {
        this.xatkitBot = xatkitBot;
        this.configuration = configuration;
        this.eventProviderMap = new HashMap<>();
    }

    /**
     * Returns the name of the platform.
     * <p>
     * This method returns the value of {@link Class#getSimpleName()}, and can not be overridden by concrete
     * subclasses. {@link RuntimePlatform}'s names are part of xatkit's naming convention, and are used to dynamically
     * load platforms and actions.
     *
     * @return the name of the platform.
     */
    public final String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Returns the {@link XatkitBot} instance associated to this platform.
     *
     * @return the {@link XatkitBot} instance associated to this platform
     */
    public final XatkitBot getXatkitBot() {
        return this.xatkitBot;
    }

    /**
     * Returns the platform's {@link Configuration}.
     *
     * @return the platform's {@link Configuration}
     */
    public Configuration getConfiguration() {
        return this.configuration;
    }

    /**
     * Starts the provided {@code eventProvider} in a dedicated thread.
     * <p>
     * {@link WebhookEventProvider}s are registered to the underlying {@link XatkitServer} (see
     * {@link XatkitServer#registerWebhookEventProvider(WebhookEventProvider)}..
     *
     * @param eventProvider the {@link RuntimeEventProvider} to start
     * @throws NullPointerException if the provided {@code eventProvider} is {@code null}
     * @see RuntimeEventProvider#run()
     * @see XatkitServer#registerWebhookEventProvider(WebhookEventProvider)
     */
    public final void startEventProvider(@NonNull RuntimeEventProvider eventProvider) {
        Log.info("Starting {0}", eventProvider.getClass().getSimpleName());
        if (eventProvider instanceof WebhookEventProvider) {
            /*
             * Register the WebhookEventProvider in the XatkitServer
             */
            Log.info("Registering {0} in the {1}", eventProvider.getClass().getSimpleName(),
                    XatkitServer.class.getSimpleName());
            xatkitBot.getXatkitServer().registerWebhookEventProvider((WebhookEventProvider) eventProvider);
        }
        EventProviderThread eventProviderThread = new EventProviderThread(eventProvider);
        eventProviderMap.put(eventProvider.getClass().getSimpleName(), eventProviderThread);
        eventProviderThread.start();
    }

    /**
     * Returns {@link Map} containing the {@link EventProviderThread}s associated to this platform.
     * <b>Note:</b> this method is protected for testing purposes, and should not be called by client code.
     *
     * @return the {@link Map} containing the {@link EventProviderThread}s associated to this platform
     */
    protected Map<String, EventProviderThread> getEventProviderMap() {
        return eventProviderMap;
    }

    /**
     * Shuts down the {@link RuntimePlatform}.
     * <p>
     * This method attempts to terminate all the running {@link RuntimeEventProvider} threads, close the corresponding
     * {@link RuntimeEventProvider}s, and disables all the platform's actions.
     *
     * @see RuntimeEventProvider#close()
     */
    public void shutdown() {
        Collection<EventProviderThread> threads = this.eventProviderMap.values();
        for (EventProviderThread thread : threads) {
            thread.getRuntimeEventProvider().close();
            thread.interrupt();
            try {
                thread.join(1000);
            } catch (InterruptedException e) {
                Log.warn("Caught an {0} while waiting for {1} thread to finish", e.getClass().getSimpleName(), thread
                        .getRuntimeEventProvider().getClass().getSimpleName());
            }
        }
        this.eventProviderMap.clear();
    }

    /**
     * Returns a {@link String} representation of this platform.
     * <p>
     * Platforms are singletons in Xatkit: we can safely return their type (see {@link Class#getSimpleName()}) as a
     * way to identify them in logs.
     *
     * @return a {@link String} representation of this platform
     */
    @Override
    public String toString() {
        return MessageFormat.format("*{0}", this.getClass().getSimpleName());
    }

    /**
     * The {@link Thread} class used to start {@link RuntimeEventProvider}s.
     * <p>
     * <b>Note:</b> this class is protected for testing purposes, and should not be called by client code.
     */
    protected static class EventProviderThread extends Thread {

        /**
         * The {@link RuntimeEventProvider} run by this {@link Thread}.
         */
        private RuntimeEventProvider runtimeEventProvider;

        /**
         * Constructs a new {@link EventProviderThread} to run the provided {@code runtimeEventProvider}
         *
         * @param runtimeEventProvider the {@link RuntimeEventProvider} to run
         */
        public EventProviderThread(RuntimeEventProvider runtimeEventProvider) {
            super(runtimeEventProvider);
            this.runtimeEventProvider = runtimeEventProvider;
        }

        /**
         * Returns the {@link RuntimeEventProvider} run by this {@link Thread}.
         *
         * @return the {@link RuntimeEventProvider} run by this {@link Thread}
         */
        public RuntimeEventProvider getRuntimeEventProvider() {
            return runtimeEventProvider;
        }

    }
}
