package com.xatkit.core;

import com.xatkit.core.platform.Formatter;
import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.platform.io.RuntimeEventProvider;
import com.xatkit.core.recognition.IntentRecognitionProvider;
import com.xatkit.core.recognition.IntentRecognitionProviderException;
import com.xatkit.core.recognition.IntentRecognitionProviderFactory;
import com.xatkit.core.server.XatkitServer;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.intent.Context;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.EventInstance;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.util.ExecutionModelUtils;
import fr.inria.atlanmod.commons.log.Log;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.configuration2.Configuration;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * The core component of the xatkit framework.
 * <p>
 * This class is constructed from an {@link ExecutionModel}, that defines the Intent to Action bindings that are
 * executed by the application. Constructing an instance of this class will load the {@link RuntimePlatform}s used by
 * the provided {@link ExecutionModel}, and enable the corresponding {@link RuntimeAction}s. It also creates an
 * instance of {@link EventDefinitionRegistry} that can be accessed to retrieve and manage {@link EventDefinition} .
 *
 * @see EventDefinitionRegistry
 * @see ExecutionService
 * @see RuntimePlatform
 */
public class XatkitCore implements Runnable {

    /**
     * The {@link Configuration} key to store the configuration folder path.
     */
    public static String CONFIGURATION_FOLDER_PATH_KEY = "xatkit.core.configuration.path";

    private ExecutionModel executionModel;

    /**
     * The {@link Configuration} used to initialize this class.
     */
    private Configuration configuration;

    /**
     * The {@link IntentRecognitionProvider} used to compute {@link RecognizedIntent}s from input text.
     */
    @Getter
    private IntentRecognitionProvider intentRecognitionProvider;

    /**
     * The {@link EventDefinitionRegistry} used to cache {@link EventDefinition}s and
     * {@link IntentDefinition}s from the input {@link ExecutionModel} and provides utility methods to retrieve
     * specific
     * {@link EventDefinition}s and {@link IntentDefinition}s and clear the cache.
     *
     * @see #getEventDefinitionRegistry() ()
     */
    @Getter
    private EventDefinitionRegistry eventDefinitionRegistry;

    /**
     * The {@link ExecutionService} used to handle {@link EventInstance}s and execute the associated
     * {@link RuntimeAction}s.
     *
     * @see ExecutionService#handleEventInstance(EventInstance, XatkitSession)
     * @see RuntimeAction
     */
    @Getter
    private ExecutionService executionService;

    /**
     * The {@link Map} used to store and retrieve {@link XatkitSession}s associated to users.
     *
     * @see #getOrCreateXatkitSession(String)
     */
    private Map<String, XatkitSession> sessions;

    /**
     * The {@link XatkitServer} instance used to capture incoming webhooks.
     */
    @Getter
    private XatkitServer xatkitServer;

    /**
     * The {@link Formatter}s used to format execution-level {@link Object}s into {@link String}.
     */
    private Map<String, Formatter> formatters;

    public XatkitCore(@NonNull ExecutionModel executionModel, @NonNull Configuration configuration) {
        this.executionModel = executionModel;
        this.configuration = configuration;
    }

    @Override
    public void run() {
        try {
            this.eventDefinitionRegistry = new EventDefinitionRegistry();
            this.formatters = new HashMap<>();
            this.registerFormatter("Default", new Formatter());
            /*
             * Start the server before creating the IntentRecognitionProvider, we need a valid XatkitServer instance
             * to register the analytics REST endpoints (this is also required to start the EventProviderDefinition).
             */
            this.xatkitServer = new XatkitServer(configuration);
            this.intentRecognitionProvider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(this,
                    configuration);
            this.sessions = new HashMap<>();
            this.executionService = new ExecutionService(executionModel, configuration);
            this.loadExecutionModel(executionModel);
            xatkitServer.start();
            Log.info("Xatkit bot started");
        } catch (Throwable t) {
            Log.error("An error occurred when starting the {0}, trying to close started services", this.getClass()
                    .getSimpleName());
            stopServices();
            throw t;
        }
    }

    /**
     * Registers the provided {@code formatter} with the given {@code formatterName}.
     *
     * @param formatterName the name of the formatter
     * @param formatter     the {@link Formatter} to register
     */
    public void registerFormatter(String formatterName, Formatter formatter) {
        if (formatters.containsKey(formatterName)) {
            Log.warn("A formatter is already registered with the name {0}, erasing it", formatterName);
        }
        formatters.put(formatterName, formatter);
    }

    /**
     * Returns the {@link Formatter} associated to the provided {@code formatterName}.
     *
     * @param formatterName the name of the {@link Formatter} to retrieve
     * @return the {@link Formatter}
     * @throws XatkitException if there is no {@link Formatter} associated to the provided {@code formatterName}.
     */
    public Formatter getFormatter(String formatterName) {
        Formatter formatter = formatters.get(formatterName);
        if (nonNull(formatter)) {
            return formatter;
        } else {
            throw new XatkitException(MessageFormat.format("Cannot find formatter {0}", formatterName));
        }
    }

    /**
     * Load the runtime instances from the provided {@link ExecutionModel}.
     * <p>
     * This method starts the {@link RuntimeEventProvider}s, builds the {@link RuntimePlatform}s, and enables their
     * {@link RuntimeAction}s from the definitions specified in the provided {@code executionModel}.
     * <p>
     * This method also registers the {@link EventDefinition}s used in the provided {@code executionModel} in the
     * {@link IntentRecognitionProvider}.
     * <p>
     * <b>Note:</b> the {@link RuntimePlatform}s associated to the provided {@link ExecutionModel} have to be
     * in the classpath in order to be dynamically loaded and instantiated.
     *
     * @param executionModel the {@link ExecutionModel} to load the runtime instances from
     * @see #startEventProviders(ExecutionModel)
     * @see #registerEventDefinition(EventDefinition)
     */
    private void loadExecutionModel(ExecutionModel executionModel) {
        boolean intentRegistered = false;
        this.startPlatforms(executionModel);
        this.startEventProviders(executionModel);
        Log.info("Registering execution rule events");

        Iterable<EventDefinition> accessedEvents = ExecutionModelUtils.getAllAccessedEvents(executionModel);
        for (EventDefinition e : accessedEvents) {
            intentRegistered |= this.registerEventDefinition(e);
        }
        if (intentRegistered) {
            /*
             * New intents have been registered in the IntentRecognitionProvider, we should explicitly ask the
             * ML Engine to train in order to take them into account.
             */
            try {
                intentRecognitionProvider.trainMLEngine();
            } catch (IntentRecognitionProviderException e) {
                Log.error("Cannot train the ML engine, see attached exception", e);
            }
        }
    }

    /**
     * Starts the {@link RuntimePlatform}s used in the provided {@code executionModel}.
     *
     * @param executionModel the {@link ExecutionModel} to start the {@link RuntimePlatform}s from
     */
    private void startPlatforms(ExecutionModel executionModel) {
        for (Object object : executionModel.getUsedPlatforms()) {
            /*
             * TODO this cast shouldn't exist: we need to fix the metamodel.
             */
            RuntimePlatform platform = (RuntimePlatform) object;
            platform.start(this, configuration);
        }
    }

    /**
     * Starts the {@link RuntimeEventProvider}s used in the provided {@code executionModel}.
     *
     * @param executionModel the {@link ExecutionModel} to start the {@link RuntimeEventProvider}s from
     */
    private void startEventProviders(ExecutionModel executionModel) {
        for (Object object : executionModel.getUsedProviders()) {
            /*
             * TODO this cast shouldn't exist: we need to fix the metamodel.
             */
            RuntimeEventProvider<?> eventProvider = (RuntimeEventProvider<?>) object;
            eventProvider.start(this.configuration);
        }
    }

    /**
     * Registers the provided {@link EventDefinition}.
     * <p>
     * The provided {@code eventDefinition} is added to the {@link EventDefinitionRegistry}. If the {@code
     * eventDefinition} is an {@link IntentDefinition} this method also takes care of registering it to the
     * {@link IntentRecognitionProvider}.
     *
     * @param eventDefinition the {@link EventDefinition} to register
     * @return {@code true} if the event has been registered to the {@link IntentRecognitionProvider}, {@code false}
     * otherwise
     * @see EventDefinitionRegistry
     * @see IntentRecognitionProvider#registerIntentDefinition(IntentDefinition)
     * @see IntentRecognitionProvider#registerEntityDefinition(EntityDefinition)
     */
    private boolean registerEventDefinition(EventDefinition eventDefinition) {
        this.eventDefinitionRegistry.registerEventDefinition(eventDefinition);
        Log.debug("Registering event {0}", eventDefinition.getName());
        if (eventDefinition instanceof IntentDefinition) {
            IntentDefinition intentDefinition = (IntentDefinition) eventDefinition;
            for (Context outContext : intentDefinition.getOutContexts()) {
                for (ContextParameter parameter : outContext.getParameters()) {
                    try {
                        this.intentRecognitionProvider.registerEntityDefinition(parameter.getEntity()
                                .getReferredEntity());
                    } catch (IntentRecognitionProviderException e) {
                        Log.error(e.getMessage());
                    }
                }
            }
            try {
                this.intentRecognitionProvider.registerIntentDefinition(intentDefinition);
                return true;
            } catch (IntentRecognitionProviderException e) {
                Log.error(e.getMessage());
            }
        }
        return false;
    }

    /**
     * Shuts down the {@link XatkitCore} and the underlying engines.
     * <p>
     * This method shuts down the underlying {@link IntentRecognitionProvider}, unloads and shuts down all the
     * {@link RuntimePlatform}s associated to this instance, unregisters the {@link EventDefinition} from the associated
     * {@link EventDefinitionRegistry}, shuts down the {@link ExecutionService}, and stops the {@link XatkitServer}.
     * <p>
     * <b>Note:</b> calling this method invalidates the {@link IntentRecognitionProvider} connection, and thus shuts
     * down intent recognition features. New {@link RuntimeAction}s cannot be processed either.
     *
     * @see IntentRecognitionProvider#shutdown()
     * @see RuntimePlatform#shutdown()
     * @see EventDefinitionRegistry#unregisterEventDefinition(EventDefinition)
     * @see ExecutionService#shutdown()
     * @see XatkitServer#stop()
     */
    public void shutdown() {
        Log.info("Shutting down XatkitCore");
        if (isShutdown()) {
            throw new XatkitException("Cannot perform shutdown, XatkitCore is already shutdown");
        }
        /* Shutdown the ExecutionService first in case there are running tasks using the IntentRecognitionProvider
         * API.
         */
        this.stopServices();
        for (Object object : this.executionModel.getUsedProviders()) {
            /*
             * TODO this cast shouldn't exist: we need to fix the metamodel.
             */
            RuntimeEventProvider<?> eventProvider = (RuntimeEventProvider<?>) object;
            eventProvider.close();
        }
        for (Object object : this.executionModel.getUsedPlatforms()) {
            /*
             * TODO this cast shouldn't exist: we need to fix the metamodel.
             */
            RuntimePlatform platform = (RuntimePlatform) object;
            platform.shutdown();
        }
        this.getEventDefinitionRegistry().clearEventDefinitions();
    }

    /**
     * Stops the running services.
     * <p>
     * This method does not throw any exception, but logs an error if an issue occurred when stoping a service.
     * Catching all the exception is done to attempt to stop all the services, but calling this method does not
     * ensure that all the services have been properly stopped.
     */
    private void stopServices() {
        /*
         * Catch each Throwable, if a service throw an error when closing we want to try to close the other ones.
         */
        if (nonNull(this.executionService)) {
            try {
                this.executionService.shutdown();
            } catch (Throwable t) {
                Log.error("An error occurred when closing the {0}", this.executionService.getClass().getSimpleName());
            }
        }
        if (nonNull(this.xatkitServer)) {
            try {
                this.xatkitServer.stop();
            } catch (Throwable t) {
                Log.error("An error occurred when closing the {0}", this.xatkitServer.getClass().getSimpleName());
            }
        }
        if (nonNull(this.intentRecognitionProvider)) {
            try {
                this.intentRecognitionProvider.shutdown();
            } catch (Throwable t) {
                Log.error("An error occurred when closing the {0}", this.intentRecognitionProvider.getClass()
                        .getSimpleName());
            }
        }
    }

    /**
     * Returns whether the {@link XatkitCore} client is shutdown.
     * <p>
     * This class is considered as shutdown if its underlying {@link ExecutionService},
     * {@link IntentRecognitionProvider}, and {@link XatkitServer} are shutdown.
     *
     * @return {@code true} if the {@link XatkitCore} client is shutdown, {@code false} otherwise
     */
    public boolean isShutdown() {
        return (!xatkitServer.isStarted()) && executionService.isShutdown() && intentRecognitionProvider
                .isShutdown();
    }

    /**
     * Retrieves or creates the {@link XatkitSession} associated to the provided {@code sessionId}.
     * <p>
     * If the {@link XatkitSession} does not exist a new one is created using
     * {@link IntentRecognitionProvider#createSession(String)}.
     *
     * @param sessionId the identifier to get or retrieve a session from
     * @return the {@link XatkitSession} associated to the provided {@code sessionId}
     * @throws NullPointerException if the provided {@code sessionId} is {@code null}
     */
    public XatkitSession getOrCreateXatkitSession(@NonNull String sessionId) {
        XatkitSession session = getXatkitSession(sessionId);
        if (isNull(session)) {
            try {
                session = this.intentRecognitionProvider.createSession(sessionId);
            } catch (IntentRecognitionProviderException e) {
                throw new XatkitException(MessageFormat.format("Cannot create session {0}, see attached exception",
                        sessionId), e);
            }
            sessions.put(sessionId, session);
            /*
             * The executor service takes care of configuring the new session and setting the init state.
             */
            executionService.initSession(session);
        }
        return session;
    }

    /**
     * Returns the {@link XatkitSession} associated to the provided {@code sessionId}
     *
     * @param sessionId the identifier to retrieve the session from
     * @return the {@link XatkitSession} associated to the provided {@code sessionId}
     * @throws NullPointerException if the provided {@code sessionId} is {@code null}
     */
    public XatkitSession getXatkitSession(@NonNull String sessionId) {
        return sessions.get(sessionId);
    }

    public Iterable<XatkitSession> getXatkitSessions() {
        return sessions.values();
    }

    /**
     * Invalidates all the {@link XatkitSession}s and clear the session registry.
     */
    public void clearXatkitSessions() {
        this.sessions.clear();
    }

    /**
     * Logs a warning message and stops the running services if the {@link XatkitCore} hasn't been closed properly.
     *
     * @throws Throwable if an error occurred when stopping the running services.
     */
    @Override
    protected void finalize() throws Throwable {
        if (!this.isShutdown()) {
            Log.warn("{0} hasn't been shutdown properly, trying to stop running services");
            this.shutdown();
        }
        super.finalize();
    }
}
