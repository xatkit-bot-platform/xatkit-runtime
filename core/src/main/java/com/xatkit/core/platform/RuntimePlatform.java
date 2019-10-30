package com.xatkit.core.platform;

import com.xatkit.core.XatkitCore;
import com.xatkit.core.XatkitException;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.platform.io.RuntimeEventProvider;
import com.xatkit.core.platform.io.WebhookEventProvider;
import com.xatkit.core.server.XatkitServer;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.EventInstance;
import com.xatkit.platform.ActionDefinition;
import com.xatkit.platform.EventProviderDefinition;
import com.xatkit.platform.PlatformDefinition;
import com.xatkit.util.Loader;
import com.xatkit.util.XbaseUtils;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.eclipse.xtext.xbase.XMemberFeatureCall;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;

/**
 * The concrete implementation of a {@link PlatformDefinition}.
 * <p>
 * A {@link RuntimePlatform} manages a set of {@link RuntimeAction}s that represent the concrete actions that can
 * be executed by the platform. This class provides primitives to enable/disable specific actions, and construct
 * {@link RuntimeAction} instances from a given {@link EventInstance}.
 * <p>
 * Note that enabling a {@link RuntimeAction} will load the corresponding class, that must be stored in the
 * <i>action</i> package of the concrete {@link RuntimePlatform} implementation. For example, enabling the action
 * <i>MyAction</i> from the {@link RuntimePlatform} <i>myPlatformPackage.MyPlatform</i> will attempt to load the class
 * <i>myPlatformPackage.action.MyAction</i>.
 */
public abstract class RuntimePlatform {

    /**
     * The {@link XatkitCore} instance containing this platform.
     */
    protected XatkitCore xatkitCore;

    /**
     * The {@link Configuration} used to initialize this class.
     * <p>
     * This {@link Configuration} is used by the {@link RuntimePlatform} to initialize the
     * {@link RuntimeEventProvider}s and
     * {@link RuntimeAction}s.
     *
     * @see #startEventProvider(EventProviderDefinition)
     * @see #createRuntimeAction(XMemberFeatureCall, List, XatkitSession)
     */
    protected Configuration configuration;

    /**
     * The {@link Map} containing the {@link RuntimeAction} associated to this platform.
     * <p>
     * This {@link Map} is used as a cache to retrieve {@link RuntimeAction} that have been previously loaded.
     *
     * @see #enableAction(ActionDefinition)
     * @see #disableAction(ActionDefinition)
     * @see #createRuntimeAction(XMemberFeatureCall, List, XatkitSession)
     */
    protected Map<String, Class<? extends RuntimeAction>> actionMap;

    /**
     * The {@link Map} containing the {@link EventProviderThread}s associated to this platform.
     * <p>
     * This {@link Map} filled when new {@link RuntimeEventProvider}s are started (see
     * {@link #startEventProvider(EventProviderDefinition)}), and is used to cache
     * {@link EventProviderThread}s and stop them when the platform is {@link #shutdown()}.
     *
     * @see #shutdown()
     */
    protected Map<String, EventProviderThread> eventProviderMap;


    /**
     * Constructs a new {@link RuntimePlatform} from the provided {@link XatkitCore} and {@link Configuration}.
     * <p>
     * <b>Note</b>: this constructor will be called by xatkit internal engine when initializing the
     * {@link RuntimePlatform}s. Subclasses implementing this constructor typically need additional parameters to be
     * initialized, that can be provided in the {@code configuration}.
     *
     * @param xatkitCore    the {@link XatkitCore} instance associated to this platform
     * @param configuration the {@link Configuration} used to initialize the {@link RuntimePlatform}
     * @throws NullPointerException if the provided {@code xatkitCore} or {@code configuration} is {@code null}
     * @see #RuntimePlatform(XatkitCore)
     */
    public RuntimePlatform(XatkitCore xatkitCore, Configuration configuration) {
        checkNotNull(xatkitCore, "Cannot construct a %s from the provided %s %s", this.getClass().getSimpleName(),
                XatkitCore.class.getSimpleName(), xatkitCore);
        checkNotNull(configuration, "Cannot construct a %s from the provided %s %s", this.getClass().getSimpleName(),
                Configuration.class.getSimpleName(), configuration);
        this.xatkitCore = xatkitCore;
        this.configuration = configuration;
        this.actionMap = new HashMap<>();
        this.eventProviderMap = new HashMap<>();
    }

    /**
     * Constructs a new {@link RuntimePlatform} from the provided {@link XatkitCore}.
     * <p>
     * <b>Note</b>: this constructor should be used by {@link RuntimePlatform}s that do not require additional
     * parameters to be initialized. In that case see {@link #RuntimePlatform(XatkitCore, Configuration)}.
     *
     * @throws NullPointerException if the provided {@code xatkitCore} is {@code null}
     * @see #RuntimePlatform(XatkitCore, Configuration)
     */
    public RuntimePlatform(XatkitCore xatkitCore) {
        this(xatkitCore, new BaseConfiguration());
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
     * Returns the {@link XatkitCore} instance associated to this platform.
     *
     * @return the {@link XatkitCore} instance associated to this platform
     */
    public final XatkitCore getXatkitCore() {
        return this.xatkitCore;
    }

    /**
     * Starts the {@link RuntimeEventProvider} corresponding to the provided {@code eventProviderDefinition}.
     * <p>
     * This method dynamically loads the {@link RuntimeEventProvider} corresponding to the provided {@code
     * eventProviderDefinition}, and starts it in a dedicated {@link Thread}.
     * <p>
     * This method also registers {@link WebhookEventProvider}s to the underlying {@link XatkitServer} (see
     * {@link XatkitServer#registerWebhookEventProvider(WebhookEventProvider)}).
     *
     * @param eventProviderDefinition the {@link EventProviderDefinition} representing the
     *                                {@link RuntimeEventProvider} to
     *                                start
     * @throws NullPointerException if the provided {@code eventProviderDefinition} or {@code xatkitCore} is {@code
     *                              null}
     * @see RuntimeEventProvider#run()
     * @see XatkitServer#registerWebhookEventProvider(WebhookEventProvider)
     */
    public final void startEventProvider(EventProviderDefinition eventProviderDefinition) {
        checkNotNull(eventProviderDefinition, "Cannot start the provided %s %s", EventProviderDefinition.class
                .getSimpleName(), eventProviderDefinition);
        checkNotNull(xatkitCore, "Cannot start the provided %s with the given %s %s", eventProviderDefinition
                .getClass().getSimpleName(), XatkitCore.class.getSimpleName(), xatkitCore);
        Log.info("Starting {0}", eventProviderDefinition.getName());
        String eventProviderQualifiedName = this.getClass().getPackage().getName() + ".io." + eventProviderDefinition
                .getName();
        Class<? extends RuntimeEventProvider> eventProviderClass = Loader.loadClass(eventProviderQualifiedName,
                RuntimeEventProvider.class);
        RuntimeEventProvider runtimeEventProvider = Loader.constructRuntimeEventProvider(eventProviderClass, this,
                configuration);
        if (runtimeEventProvider instanceof WebhookEventProvider) {
            /*
             * Register the WebhookEventProvider in the XatkitServer
             */
            Log.info("Registering {0} in the {1}", runtimeEventProvider, XatkitServer.class.getSimpleName());
            xatkitCore.getXatkitServer().registerWebhookEventProvider((WebhookEventProvider) runtimeEventProvider);
        }
        Log.info("Starting RuntimeEventProvider {0}", eventProviderClass.getSimpleName());
        EventProviderThread eventProviderThread = new EventProviderThread(runtimeEventProvider);
        eventProviderMap.put(eventProviderDefinition.getName(), eventProviderThread);
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
     * Retrieves and loads the {@link RuntimeAction} defined by the provided {@link ActionDefinition}.
     * <p>
     * This method loads the corresponding {@link RuntimeAction} based on xatkit's naming convention. The
     * {@link RuntimeAction} must be located under the {@code actionDefinition} sub-package of the
     * {@link RuntimePlatform}
     * concrete subclass package.
     *
     * @param actionDefinition the {@link ActionDefinition} representing the {@link RuntimeAction} to enable
     * @see Loader#loadClass(String, Class)
     */
    public void enableAction(ActionDefinition actionDefinition) {
        if (!actionMap.containsKey(actionDefinition.getName())) {
            String actionQualifiedName =
                    this.getClass().getPackage().getName() + ".action." + actionDefinition.getName();
            Class<? extends RuntimeAction> runtimeAction = Loader.loadClass(actionQualifiedName, RuntimeAction.class);
            actionMap.put(actionDefinition.getName(), runtimeAction);
        }
    }

    /**
     * Disables the {@link RuntimeAction} defined by the provided {@link ActionDefinition}.
     *
     * @param actionDefinition the {@link ActionDefinition} representing the {@link RuntimeAction} to disable
     */
    public void disableAction(ActionDefinition actionDefinition) {
        actionMap.remove(actionDefinition.getName());
    }

    /**
     * Disables all the {@link RuntimeAction}s of the {@link RuntimePlatform}.
     */
    public final void disableAllActions() {
        actionMap.clear();
    }

    /**
     * Returns all the {@link RuntimeAction} {@link Class}es associated to this {@link RuntimePlatform}.
     * <p>
     * This method returns the {@link Class}es describing the {@link RuntimeAction}s associated to this platform. To
     * construct a new {@link RuntimeAction} see {@link #createRuntimeAction(XMemberFeatureCall, List, XatkitSession)}.
     *
     * @return all the {@link RuntimeAction} {@link Class}es associated to this {@link RuntimePlatform}
     * @see #createRuntimeAction(XMemberFeatureCall, List, XatkitSession)
     */
    public final Collection<Class<? extends RuntimeAction>> getActions() {
        return actionMap.values();
    }

    /**
     * Creates a new {@link RuntimeAction} instance from the provided {@link XMemberFeatureCall}.
     * <p>
     * This methods attempts to construct a {@link RuntimeAction} defined by the provided {@code actionCall} by
     * matching the provided {@code arguments} to the {@link ActionDefinition}'s parameters.
     *
     * @param actionCall the {@link XMemberFeatureCall} representing the {@link RuntimeAction} to create
     * @param arguments  the {@link List} of computed values used as arguments for the created {@link RuntimeAction}
     * @param session    the {@link XatkitSession} associated to the action
     * @return a new {@link RuntimeAction} instance from the provided {@code actionCall}
     * @throws NullPointerException if the provided {@code actionCall}, {@code arguments}, or {@code session} is
     *                              {@code null}
     * @throws XatkitException      if the provided {@code actionCall} does not match any {@link RuntimeAction},
     *                              or if an error occurred when building the {@link RuntimeAction}
     */
    public RuntimeAction createRuntimeAction(XMemberFeatureCall actionCall, List<Object> arguments,
                                             XatkitSession session) {
        checkNotNull(actionCall, "Cannot construct a %s from the provided %s %s", RuntimeAction.class
                .getSimpleName(), XMemberFeatureCall.class.getSimpleName(), actionCall);
        checkNotNull(arguments, "Cannot construct a %s from the provided argument list %s",
                RuntimeAction.class.getSimpleName(), arguments);
        checkNotNull(session, "Cannot construct a %s from the provided %s %s", RuntimeAction.class.getSimpleName(),
                XatkitSession.class.getSimpleName(), session);
        String actionName = XbaseUtils.getActionName(actionCall);

        Class<? extends RuntimeAction> runtimeActionClass = actionMap.get(actionName);
        if (isNull(runtimeActionClass)) {
            throw new XatkitException(MessageFormat.format("Cannot create the {0} {1}, the action is not " +
                    "loaded in the platform", RuntimeAction.class.getSimpleName(), actionName));
        }

        Object[] argumentValues = arguments.toArray();
        Object[] fullArgumentValues = new Object[argumentValues.length + 2];
        fullArgumentValues[0] = this;
        fullArgumentValues[1] = session;
        RuntimeAction runtimeAction;

        if (argumentValues.length > 0) {
            System.arraycopy(argumentValues, 0, fullArgumentValues, 2, argumentValues.length);
        }
        try {
            /**
             * The types of the parameters are not known, use {@link Loader#construct(Class, Object[])} to try to
             * find a constructor that accepts them.
             */
            runtimeAction = Loader.construct(runtimeActionClass, fullArgumentValues);
        } catch (NoSuchMethodException e) {
            throw new XatkitException(MessageFormat.format("Cannot find a {0} constructor for the provided parameter " +
                    "types ({1})", runtimeActionClass.getSimpleName(), printClassArray(fullArgumentValues)), e);
        } catch (InvocationTargetException e) {
            throw new XatkitException(MessageFormat.format("An error occurred when calling the {0} constructor for " +
                            "the provided parameter types ({1}), see attached exception",
                    runtimeActionClass.getSimpleName(),
                    printClassArray(fullArgumentValues)), e);
        }
        runtimeAction.init();
        return runtimeAction;
    }


    /**
     * Shuts down the {@link RuntimePlatform}.
     * <p>
     * This method attempts to terminate all the running {@link RuntimeEventProvider} threads, close the corresponding
     * {@link RuntimeEventProvider}s, and disables all the platform's actions.
     *
     * @see RuntimeEventProvider#close()
     * @see #disableAllActions()
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
        /*
         * Disable the actions at the end, in case a running EventProviderThread triggers an action computation
         * before it is closed.
         */
        this.disableAllActions();
    }

    /**
     * Formats the provided {@code array} in a {@link String} used representing their {@link Class}es.
     * <p>
     * The returned {@link String} is "a1.getClass().getSimpleName(), a2.getClass().getSimpleName(), an.getClass()
     * .getSimpleName()", where <i>a1</i>, <i>a2</i>, and <i>an</i> are elements in the provided {@code array}.
     *
     * @param array the array containing the elements to print the {@link Class}es of
     * @return a {@link String} containing the formatted elements' {@link Class}es
     */
    private String printClassArray(Object[] array) {
        List<String> toStringList = StreamSupport.stream(Arrays.asList(array).spliterator(), false).map(o ->
        {
            if (isNull(o)) {
                return "null";
            } else {
                return o.getClass().getSimpleName();
            }
        }).collect(Collectors.toList());
        return String.join(", ", toStringList);
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
