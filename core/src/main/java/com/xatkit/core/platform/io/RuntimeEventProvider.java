package com.xatkit.core.platform.io;

import com.xatkit.core.XatkitCore;
import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.server.HttpMethod;
import com.xatkit.core.server.RestHandler;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.intent.EventInstance;
import lombok.NonNull;
import org.apache.commons.configuration2.Configuration;

/**
 * Generates {@link EventInstance}s that can be used by a Xatkit bot.
 * <p>
 * This class maps received inputs (e.g. REST request, user message, socket event) to {@link EventInstance}s that are
 * used by the Xatkit engine.
 *
 * @param <T> the {@link RuntimePlatform} containing the provider
 */
public abstract class RuntimeEventProvider<T extends RuntimePlatform> implements Runnable {

    /**
     * The {@link XatkitCore} instance used to handle events.
     * <p>
     * This attribute is a shortcut for {@code runtimePlatform.getXatkitCore()}.
     */
    protected XatkitCore xatkitCore;

    /**
     * The {@link RuntimePlatform} subclass containing this action.
     */
    protected T runtimePlatform;

    /**
     * Creates an <b>unstarted</b> {@link RuntimeEventProvider} managed by the provided {@code platform}.
     * <p>
     * As for {@link RuntimePlatform}, this constructor does not have access to the {@link XatkitCore} nor the
     * {@link Configuration}: it is typically called when defining a bot to have a usable reference to set in
     * {@link ExecutionModel#getUsedProviders()}, but it is initialized during the bot deployment using the
     * {@link RuntimeEventProvider#start(Configuration)} method.
     *
     * @param platform the {@link RuntimePlatform} managing this provider
     */
    public RuntimeEventProvider(@NonNull T platform) {
        this.runtimePlatform = platform;
    }

    /**
     * Starts the provider.
     * <p>
     * This method takes as input the bot {@code configuration} that can contain specific properties to customize the
     * provider (e.g. to filter some input). Subclasses typically override this method to initialize their internal
     * data structure and register REST handlers to receive push events (see
     * {@link com.xatkit.core.server.XatkitServer#registerRestEndpoint(HttpMethod, String, RestHandler)}.
     * <p>
     * This method is automatically called bu Xatkit when a bot using this provider is starting.
     * <p>
     * Note that the {@link XatkitCore} instance is bound to the provider when calling this method.
     *
     * @param configuration the {@link Configuration} of the bot currently run
     * @see RuntimePlatform#startEventProvider(RuntimeEventProvider)
     */
    public void start(@NonNull Configuration configuration) {
        this.xatkitCore = runtimePlatform.getXatkitCore();
        this.runtimePlatform.startEventProvider(this);
    }

    /**
     * Returns the {@link RuntimePlatform} containing this {@link RuntimeEventProvider}.
     *
     * @return the {@link RuntimePlatform} containing this {@link RuntimeEventProvider}
     */
    public T getRuntimePlatform() {
        return runtimePlatform;
    }

    /**
     * Sends the provided {@code eventInstance} and {@code session} for computation to the Xatkit core component.
     * <p>
     * This method sets the <i>triggeredBy</i> field of the provided {@code eventInstance} with the name of the
     * containing platform of this provider.
     * <p>
     * This method can be extended to perform specific checks before triggering actions (e.g. ensure that a specific
     * context variable has been set).
     *
     * @param eventInstance the {@link EventInstance} to send to the Xatkit core component
     * @param session       the {@link XatkitSession} associated to the provided {@code eventInstance}
     */
    public void sendEventInstance(EventInstance eventInstance, XatkitSession session) {
        eventInstance.setTriggeredBy(this.runtimePlatform.getName());
        this.xatkitCore.getExecutionService().handleEventInstance(eventInstance, session);
    }

    public void broadcastEventInstance(EventInstance eventInstance) {
        eventInstance.setTriggeredBy(this.runtimePlatform.getName());
        this.xatkitCore.getXatkitSessions().forEach(xatkitSession ->
                this.xatkitCore.getExecutionService().handleEventInstance(eventInstance, xatkitSession)
        );
    }

    /**
     * Closes the {@link RuntimeEventProvider} and releases internal resources.
     * <p>
     * This method should be overridden by concrete subclasses that manipulate internal resources that require to be
     * explicitly closed.
     */
    public void close() {

    }

}
