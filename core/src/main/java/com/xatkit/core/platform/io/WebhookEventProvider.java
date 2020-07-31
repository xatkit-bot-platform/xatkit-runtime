package com.xatkit.core.platform.io;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.server.HttpMethod;
import com.xatkit.core.server.RestHandler;
import com.xatkit.core.server.XatkitServer;
import com.xatkit.execution.ExecutionModel;
import lombok.NonNull;
import org.apache.commons.configuration2.Configuration;

/**
 * A specialised {@link RuntimeEventProvider} that handles Rest requests sent by the {@link XatkitServer}.
 * <p>
 * Concrete subclasses <b>must</b> implement the {@link #getEndpointURI()} that sets the URI to register the provider
 * to, and {@link #createRestHandler()} that creates the concrete {@link RestHandler} instance handling incoming Rest
 * requests.
 *
 * @param <T> the concrete {@link RuntimePlatform} subclass type containing the provider
 * @param <H> the {@link RestHandler} type processing incoming Rest requests
 * @see RestHandler
 * @see com.xatkit.core.server.RestHandlerFactory
 */
public abstract class WebhookEventProvider<T extends RuntimePlatform, H extends RestHandler> extends RuntimeEventProvider<T> {

    /**
     * The {@link RestHandler} used to process incoming Rest requests.
     */
    private H restHandler;

    /**
     * Creates an <b>unstarted</b> {@link WebhookEventProvider} managed by the provided {@code platform}.
     * <p>
     * As for {@link RuntimeEventProvider}, this constructor does not have access to the
     * {@link com.xatkit.core.XatkitCore} nor the {@link Configuration}: it is typically called when defining a bot
     * to have a usable reference to set in {@link ExecutionModel#getUsedProviders()}, but it is initialized during
     * the bot deployment using the {@link WebhookEventProvider#start(Configuration)} method.
     *
     * @param platform the {@link RuntimePlatform} managing this provider
     */
    public WebhookEventProvider(T platform) {
        super(platform);
    }

    /**
     * Starts the provider and registers its {@link RestHandler}.
     *
     * @param configuration the {@link Configuration} of the bot currently run
     */
    @Override
    public void start(@NonNull Configuration configuration) {
        super.start(configuration);
        this.restHandler = createRestHandler();
    }

    /**
     * Returns the URI of the REST endpoint to register the provider to.
     * <p>
     * The returned {@link String} must be prefixed by a {@code '/'}.
     *
     * @return the URI of the Rest endpoint to register the provider to
     * @see XatkitServer#registerWebhookEventProvider(WebhookEventProvider)
     */
    public abstract String getEndpointURI();

    /**
     * Returns the {@link HttpMethod} of the REST endpoint to register the provided to.
     * <p>
     * This method returns {@link HttpMethod#POST} by default, subclasses can override this method to return custom
     * {@link HttpMethod}.
     *
     * @return the {@link HttpMethod} of the REST endpoint to register the provider to
     */
    public HttpMethod getEndpointMethod() {
        return HttpMethod.POST;
    }

    /**
     * Returns the concrete {@link RestHandler} instance that handles incoming Rest requests.
     * <p>
     * This handler can be defined with the utility methods provided in
     * {@link com.xatkit.core.server.RestHandlerFactory}.
     *
     * @return the concrete {@link RestHandler} instance that handles incoming Rest requests
     * @see com.xatkit.core.server.RestHandlerFactory
     */
    protected abstract H createRestHandler();

    /**
     * Returns the {@link RestHandler} embedded in this provider.
     *
     * @return the {@link RestHandler} embedded in this provider
     */
    public H getRestHandler() {
        return this.restHandler;
    }

    /**
     * Runs the provider.
     */
    @Override
    public void run() {
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {

            }
        }
    }
}
