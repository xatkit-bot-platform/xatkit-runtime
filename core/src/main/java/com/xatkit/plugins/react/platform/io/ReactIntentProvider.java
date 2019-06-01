package com.xatkit.plugins.react.platform.io;

import com.xatkit.core.server.JarvisServer;
import com.xatkit.plugins.chat.platform.io.ChatIntentProvider;
import com.xatkit.plugins.react.platform.ReactPlatform;
import org.apache.commons.configuration2.Configuration;

/**
 * A React {@link ChatIntentProvider}.
 * <p>
 * This class initializes a {@link ReactWebhook} and registers it to the
 * {@link JarvisServer}, allowing to receive messages from jarvis-react installations
 * as events (this is a quick fix for #221, provider hierarchy should be refactored to allow webhook-based chat
 * providers).
 */
public class ReactIntentProvider extends ChatIntentProvider<ReactPlatform> {

    /**
     * Constructs a new {@link ReactIntentProvider} from the provided {@code runtimePlatform} and {@code configuration}.
     * <p>
     * This constructor initializes the {@link ReactWebhook} that is used to receive messages from jarvis-react
     * installations as events.
     *
     * @param runtimePlatform the {@link ReactPlatform} containing this {@link ReactIntentProvider}
     * @param configuration   the platform's {@link Configuration}
     */
    public ReactIntentProvider(ReactPlatform runtimePlatform, Configuration configuration) {
        super(runtimePlatform, configuration);
        /*
         * Register the webhook that receives the messages from the React application. This webhook relies on this
         * class to send event instances to the core component.
         */
        ReactWebhook webhookProvider = new ReactWebhook(runtimePlatform, configuration, this);
        this.runtimePlatform.getJarvisCore().getJarvisServer().registerWebhookEventProvider(webhookProvider);
    }

    @Override
    public void run() {
        /*
         * Do nothing, the work is done by the internal webhook provider.
         */
    }

}
