package com.xatkit.core;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.platform.io.RuntimeEventProvider;
import com.xatkit.core.recognition.IntentRecognitionProvider;
import com.xatkit.core.recognition.IntentRecognitionProviderException;
import com.xatkit.core.recognition.IntentRecognitionProviderFactory;
import com.xatkit.core.server.XatkitServer;
import com.xatkit.dsl.model.ExecutionModelProvider;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.EventInstance;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.RecognizedIntent;
import fr.inria.atlanmod.commons.log.Log;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.configuration2.Configuration;

import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Runs a Xatkit bot.
 * <p>
 * This class is constructed with an {@link ExecutionModel} representing the bot behavior, and takes care of
 * initializing the runtime components that are required to deploy and manage the bot.
 * <p>
 * See the code below to start an existing bot:
 * <pre>
 * {@code
 * ExecutionModel model = [...]
 * Configuration configuration = new BaseConfiguration();
 * // add properties in the configuration if needed
 * XatkitBot xatkitBot = new XatkitBot(model, configuration);
 * xatkitBot.run();
 * // The bot is now deployed and running
 * }
 * </pre>
 *
 * @see #run()
 */
public class XatkitBot implements Runnable {

    /**
     * The {@link Configuration} key to store the configuration folder path.
     */
    @Deprecated
    public static String CONFIGURATION_FOLDER_PATH_KEY = "xatkit.core.configuration.path";

    /**
     * The {@link ExecutionModel} representing the bot to deploy and execute.
     */
    private ExecutionModel executionModel;

    /**
     * The {@link Configuration} used to initialize this class.
     */
    private Configuration configuration;

    /**
     * The {@link IntentRecognitionProvider} used to compute {@link RecognizedIntent}s from input text.
     *
     * @see IntentRecognitionProviderFactory
     */
    @Getter
    private IntentRecognitionProvider intentRecognitionProvider;

    /**
     * The {@link EventDefinitionRegistry} used to cache {@link EventDefinition}s and{@link IntentDefinition}s.
     * <p>
     * This registry is populated with the content of the {@link ExecutionModel}.
     *
     * @see #getEventDefinitionRegistry()
     */
    @Getter
    private EventDefinitionRegistry eventDefinitionRegistry;

    /**
     * The {@link ExecutionService} that manages the execution of the bot.
     * <p>
     * The {@link ExecutionService} manages the states of the bot and executes their body/fallback, and checks
     * whether state's transition are navigable.
     *
     * @see ExecutionService#handleEventInstance(EventInstance, StateContext)
     */
    @Getter
    private ExecutionService executionService;

    /**
     * The {@link Map} used to store and retrieve {@link StateContext}s associated to users.
     *
     * @see #getOrCreateContext(String)
     */
    private Map<String, StateContext> stateContexts;

    /**
     * The {@link XatkitServer} instance used to capture incoming webhooks.
     */
    @Getter
    private XatkitServer xatkitServer;

    /**
     * Creates an <b>unstarted</b> {@link XatkitBot} instance.
     * <p>
     * The underlying bot can be started by calling {@link #run()}.
     *
     * @param executionModel the model containing the bot's execution logic
     * @param configuration  the Xatkit {@link Configuration}
     */
    public XatkitBot(@NonNull ExecutionModel executionModel, @NonNull Configuration configuration) {
        this.executionModel = executionModel;
        this.configuration = this.adaptConfiguration(configuration);
    }

    /**
     * Creates an <b>unstarted</b> {@link XatkitBot} instance.
     * <p>
     * This method is an utility constructor that allows to provide models built with the fluent DSL, and is
     * equivalent to
     * <pre>
     * {@code
     * new XatkitBot(executionModelProvider.getExecutionModel(), configuration);
     * }
     * </pre>
     * <p>
     * The underlying bot can be started by calling {@link #run()}.
     *
     * @param executionModelProvider the provider containing the bot's model
     * @param configuration          the Xatkit {@link Configuration}
     */
    public XatkitBot(@NonNull ExecutionModelProvider executionModelProvider, @NonNull Configuration configuration) {
        this(executionModelProvider.getExecutionModel(), configuration);
    }

    /**
     * Starts the underlying bot.
     * <p>
     * This method takes care of deploying the bot (e.g. registering intents to the NLP service(s), starting the
     * accessed platforms and providers, etc) and starts the {@link ExecutionService} managing its execution.
     */
    @Override
    public void run() {
        try {
            this.eventDefinitionRegistry = new EventDefinitionRegistry();
            /*
             * Start the server before creating the IntentRecognitionProvider, we need a valid XatkitServer instance
             * to register the analytics REST endpoints (this is also required to start the EventProviderDefinition).
             */
            this.xatkitServer = new XatkitServer(configuration);
            this.intentRecognitionProvider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(this,
                    configuration);
            this.stateContexts = new HashMap<>();
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
     * Adapts the provided {@code baseConfiguration} with additional runtime properties.
     * <p>
     * This method merges the provided {@code baseConfiguration} with {@link System#getProperties()}, allowing to
     * override a bot configuration from a command-line argument. This can be used, for example, to override the
     * server port if the bot is deployed in a server where the hardcoded port is already used.
     * <p>
     * Command line arguments can be provided with a similar syntax to Xatkit properties, as an example, the code
     * below overrides the port used by the bot:
     * <pre>
     * {@code
     * java -Dxatkit.server.port=5010 -jar MyBot.jar
     * }
     * </pre>
     * @param baseConfiguration the base {@link Configuration} to adapt
     * @return the adapted {@link Configuration}
     */
    private Configuration adaptConfiguration(Configuration baseConfiguration) {
        Properties systemProperties = System.getProperties();
        systemProperties.forEach((k,v) -> {
            if(k instanceof String) {
                /*
                 * Use setProperty here to make sure we remove existing values.
                 */
                baseConfiguration.setProperty((String) k, v);
            }
        });
        return baseConfiguration;
    }

    /**
     * Registers the events used in the provided {@code executionModel} and start the associated platforms/providers.
     *
     * @param executionModel the {@link ExecutionModel} to load
     * @see #startPlatforms(ExecutionModel)
     * @see #startEventProviders(ExecutionModel)
     * @see #registerEventDefinition(EventDefinition)
     */
    private void loadExecutionModel(ExecutionModel executionModel) {
        boolean intentRegistered = false;
        executionModel.consolidate();
        this.startPlatforms(executionModel);
        this.startEventProviders(executionModel);
        Log.info("Registering execution rule events");

        Iterable<EventDefinition> accessedEvents = executionModel.getAllAccessedEvents();
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
            for (ContextParameter parameter : intentDefinition.getParameters()) {
                try {
                    this.intentRecognitionProvider.registerEntityDefinition(parameter.getEntity()
                            .getReferredEntity());
                } catch (IntentRecognitionProviderException e) {
                    Log.error(e.getMessage());
                }
            }
            try {
                this.intentRecognitionProvider.registerIntentDefinition(intentDefinition);
                return true;
            } catch (IntentRecognitionProviderException e) {
                Log.warn(e.getMessage());
            }
        }
        return false;
    }

    /**
     * Shuts down the {@link XatkitBot} and the underlying engines.
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
        Log.info("Shutting down XatkitBot");
        if (isShutdown()) {
            throw new XatkitException("Cannot perform shutdown, XatkitBot is already shutdown");
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
     * Returns whether the {@link XatkitBot} client is shutdown.
     * <p>
     * This class is considered as shutdown if its underlying {@link ExecutionService},
     * {@link IntentRecognitionProvider}, and {@link XatkitServer} are shutdown.
     *
     * @return {@code true} if the {@link XatkitBot} client is shutdown, {@code false} otherwise
     */
    public boolean isShutdown() {
        return (isNull(xatkitServer) || !xatkitServer.isStarted())
            && (isNull(executionService) || executionService.isShutdown())
            && (isNull(intentRecognitionProvider) || intentRecognitionProvider.isShutdown());
    }

    /**
     * Retrieves or creates the {@link StateContext} associated to the provided {@code contextId}.
     * <p>
     * If the {@link StateContext} does not exist a new one is created using
     * {@link IntentRecognitionProvider#createContext(String)}.
     *
     * @param contextId the identifier of the context to get
     * @return the {@link StateContext} associated to the provided {@code contextId}
     * @see #getContext(String)
     */
    public @NonNull StateContext getOrCreateContext(@NonNull String contextId) {
        StateContext context = getContext(contextId);
        if (isNull(context)) {
            try {
                context = this.intentRecognitionProvider.createContext(contextId);
            } catch (IntentRecognitionProviderException e) {
                throw new XatkitException(MessageFormat.format("Cannot create session {0}, see attached exception",
                        contextId), e);
            }
            stateContexts.put(contextId, context);
            /*
             * The executor service takes care of configuring the new session and setting the init state.
             */
            executionService.initContext(context);
        }
        return context;
    }

    /**
     * Returns the {@link StateContext} associated to the provided {@code contextId}
     *
     * @param contextId the identifier of the context
     * @return the {@link StateContext} associated to the provided {@code contextId}, or {@code null} if it does not
     * exist
     */
    public @Nullable
    StateContext getContext(@NonNull String contextId) {
        return stateContexts.get(contextId);
    }

    public Iterable<StateContext> getContexts() {
        return stateContexts.values();
    }

    /**
     * Invalidates all the {@link StateContext}s and clear the session registry.
     */
    public void clearContexts() {
        this.stateContexts.clear();
    }

    /**
     * Logs a warning message and stops the running services if the {@link XatkitBot} hasn't been closed properly.
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
