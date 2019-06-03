package com.xatkit.core.platform.io;

import com.xatkit.core.XatkitCore;
import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.EventInstance;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An abstract class representing user input providers.
 * <p>
 * Concrete implementations of this class are dynamically instantiated by the {@link XatkitCore} component, and use
 * it to notify the engine about new messages to handle. Note that {@link RuntimeEventProvider} instances are
 * started in a dedicated {@link Thread}.
 * <p>
 * Instances of this class can be configured using the {@link Configuration}-based constructor, that enable to pass
 * additional parameters to the constructor.
 *
 * @param <T> the concrete {@link RuntimePlatform} subclass type containing the provider
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
     * Constructs a new {@link RuntimeEventProvider} with the provided {@code runtimePlatform}.
     * <p>
     * <b>Note</b>: this constructor should be used by {@link RuntimeEventProvider}s that do not require additional
     * parameters to be initialized. In that case see {@link #RuntimeEventProvider(RuntimePlatform, Configuration)}.
     *
     * @param runtimePlatform the {@link RuntimePlatform} containing this {@link RuntimeEventProvider}
     * @throws NullPointerException if the provided {@code runtimePlatform} is {@code null}
     */
    public RuntimeEventProvider(T runtimePlatform) {
        this(runtimePlatform, new BaseConfiguration());
    }

    /**
     * Constructs a new {@link RuntimeEventProvider} with the provided {@code runtimePlatform} and {@code
     * configuration}.
     * <p>
     * <b>Note</b>: this constructor will be called by xatkit internal engine when initializing the
     * {@link XatkitCore} component. Subclasses implementing this constructor typically
     * need additional parameters to be initialized, that can be provided in the {@code configuration}.
     *
     * @param runtimePlatform the {@link RuntimePlatform} containing this {@link RuntimeEventProvider}
     * @param configuration   the {@link Configuration} used to initialize the {@link RuntimeEventProvider}
     * @throws NullPointerException if the provided {@code runtimePlatform} is {@code null}
     */
    public RuntimeEventProvider(T runtimePlatform, Configuration configuration) {
        /*
         * Do nothing with the configuration, it can be used by subclasses that require additional initialization
         * information.
         */
        checkNotNull(runtimePlatform, "Cannot construct an instance of %s with a null %s", this.getClass()
                .getSimpleName(), RuntimePlatform.class.getSimpleName());
        this.runtimePlatform = runtimePlatform;
        this.xatkitCore = runtimePlatform.getXatkitCore();
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
     * This method can be extended to perform specific checks before triggering actions (e.g. ensure that a specific
     * context variable has been set).
     *
     * @param eventInstance the {@link EventInstance} to send to the Xatkit core component
     * @param session       the {@link XatkitSession} associated to the provided {@code eventInstance}
     */
    public void sendEventInstance(EventInstance eventInstance, XatkitSession session) {
        this.xatkitCore.getExecutionService().handleEventInstance(eventInstance, session);
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
