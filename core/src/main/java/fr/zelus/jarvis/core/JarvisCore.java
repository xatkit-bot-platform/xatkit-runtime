package fr.zelus.jarvis.core;

import com.google.cloud.dialogflow.v2.SessionName;
import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.dialogflow.DialogFlowApi;
import fr.zelus.jarvis.intent.RecognizedIntent;
import fr.zelus.jarvis.module.Action;
import fr.zelus.jarvis.module.Module;
import fr.zelus.jarvis.orchestration.OrchestrationLink;
import fr.zelus.jarvis.orchestration.OrchestrationModel;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * The core component of the jarvis framework.
 * <p>
 * This class is constructed from an {@link OrchestrationModel}, that defines the Intent to Action bindings that are
 * executed by the application. Constructing an instance of this class will load the {@link JarvisModule}s used by
 * the provided {@link OrchestrationModel}, and enable the corresponding {@link JarvisAction}s. It also creates an
 * instance of {@link IntentDefinitionRegistry} that can be accessed to retrieve and manage
 * {@link fr.zelus.jarvis.intent.IntentDefinition} .
 * <p>
 * Once constructed, this class can be globally accessed through the {@link JarvisCore#getInstance()} static method.
 * Easing the access to {@link IntentDefinitionRegistry}, {@link JarvisModuleRegistry}, and
 * {@link OrchestrationService} from the other jarvis components.
 *
 * @see IntentDefinitionRegistry
 * @see JarvisModuleRegistry
 * @see OrchestrationService
 * @see JarvisModule
 */
public class JarvisCore {

    /**
     * The globally registered instance of this class.
     */
    private static JarvisCore INSTANCE = null;

    /**
     * Returns the globally registered instance of this class, if it exists.
     *
     * @return the globally registered instance of this class
     * @throws NullPointerException if there is no instance of this class that is globally registered
     */
    public static JarvisCore getInstance() {
        if (isNull(INSTANCE)) {
            throw new NullPointerException("Cannot retrieve the JarvisCore instance, make sure to initialize it first");
        }
        return INSTANCE;
    }

    /**
     * The {@link DialogFlowApi} used to access the DialogFlow framework and send user input for
     * {@link RecognizedIntent}
     * extraction.
     */
    private DialogFlowApi dialogFlowApi;

    /**
     * The DialogFlow session associated to this {@link JarvisCore} instance.
     */
    private SessionName sessionName;

    /**
     * The {@link JarvisModuleRegistry} used to cache loaded module, and provides utility method to retrieve,
     * unregister,
     * and clear them.
     *
     * @see #getJarvisModuleRegistry()
     */
    private JarvisModuleRegistry jarvisModuleRegistry;

    /**
     * The {@link IntentDefinitionRegistry} used to cache {@link fr.zelus.jarvis.intent.IntentDefinition} from the input
     * {@link OrchestrationModel} and provides utility methods to retrieve specific
     * {@link fr.zelus.jarvis.intent.IntentDefinition} and clear the cache.
     *
     * @see #getIntentDefinitionRegistry()
     */
    private IntentDefinitionRegistry intentDefinitionRegistry;

    /**
     * The {@link OrchestrationService} used to find {@link JarvisAction}s to execute from the received textual inputs.
     *
     * @see #handleMessage(String)
     * @see JarvisAction
     */
    private OrchestrationService orchestrationService;

    /**
     * The {@link ExecutorService} used to process {@link JarvisAction}s returned by the registered
     * {@link JarvisModule}s.
     *
     * @see JarvisModule
     * @see JarvisAction
     */
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * Constructs a new {@link JarvisCore} instance with the provided {@code projectId}, {@code languageCode}, and
     * {@code orchestrationModel}.
     * <p>
     * The provided {@link OrchestrationModel} defines the Intent to Action bindings that are executed by the
     * application. This constructor takes care of loading the {@link JarvisModule}s associated to the provided
     * {@code orchestrationModel} and enables the corresponding {@link JarvisAction}s.
     * <p>
     * Once constructed, this class can be globally retrieved by using {@link JarvisCore#getInstance()} method.
     * <p>
     * <b>Note:</b> the {@link JarvisModule}s associated to the provided {@code orchestrationModel} have to be in the
     * classpath in order to be dynamically loaded and instantiated.
     *
     * @param projectId          the unique identifier of the DialogFlow project
     * @param languageCode       the code of the language processed by DialogFlow
     * @param orchestrationModel the {@link OrchestrationModel} defining the Intent to Action bindings
     * @throws NullPointerException if the provided {@code projectId}, {@code languageCode}, or {@code
     *                              orchestrationModel} is {@code null}
     * @see OrchestrationModel
     */
    public JarvisCore(String projectId, String languageCode, OrchestrationModel orchestrationModel) {
        checkNotNull(projectId, "Cannot construct a jarvis instance from a null projectId");
        checkNotNull(languageCode, "Cannot construct a jarvis instance from a null language code");
        checkNotNull(orchestrationModel, "Cannot construct a jarvis instance from a null orchestration model");
        this.dialogFlowApi = new DialogFlowApi(projectId, languageCode);
        this.sessionName = dialogFlowApi.createSession();
        /*
         * The OrchestrationService instance should be available through a getter for testing purposes.
         */
        this.orchestrationService = new OrchestrationService(orchestrationModel);
        this.jarvisModuleRegistry = new JarvisModuleRegistry();
        this.intentDefinitionRegistry = new IntentDefinitionRegistry();
        for (OrchestrationLink link : orchestrationModel.getOrchestrationLinks()) {
            /*
             * Extracts the IntentDefinitions
             */
            this.intentDefinitionRegistry.registerIntentDefinition(link.getIntent());
            /*
             * Load the action modules
             */
            for (Action action : link.getActions()) {
                Module module = (Module) action.eContainer();
                JarvisModule jarvisModule = this.jarvisModuleRegistry.getJarvisModule(module.getName());
                if (isNull(jarvisModule)) {
                    jarvisModule = loadJarvisModuleFromModuleModel(module);
                    this.jarvisModuleRegistry.registerJarvisModule(jarvisModule);
                }
                jarvisModule.enableAction(action);
            }
        }
        /*
         * The instance is correctly constructed, set it as the global instance of this class.
         */
        if (nonNull(INSTANCE)) {
            Log.warn("Globally registering the constructed JarvisCore instance ({0}) will erase the stored one {1}",
                    this, INSTANCE);
        }
        INSTANCE = this;
    }

    /**
     * Loads the {@link JarvisModule} defined by the provided {@link Module} definition.
     * <p>
     * This method searches in the classpath a {@link Class} matching the input {@link Module#getJarvisModulePath()}
     * value and calls its default constructor.
     *
     * @param moduleModel the jarvis {@link Module} definition to load
     * @return an instance of the loaded {@link JarvisModule}
     * @throws JarvisException if their is no {@link Class} matching the provided {@code moduleModel} or if the
     *                         {@link JarvisModule} can not be constructed
     * @see Module
     */
    private JarvisModule loadJarvisModuleFromModuleModel(Module moduleModel) throws JarvisException {
        Log.info("Loading JarvisModule {0}", moduleModel.getName());
        try {
            return (JarvisModule) Class.forName(moduleModel.getJarvisModulePath()).newInstance();
        } catch (ClassNotFoundException e) {
            String errorMessage = MessageFormat.format("Cannot load the module {0}, invalid path: {1}", moduleModel
                    .getName(), moduleModel.getJarvisModulePath());
            Log.error(errorMessage);
            throw new JarvisException(errorMessage, e);
        } catch (InstantiationException | IllegalAccessException e) {
            String errorMessage = MessageFormat.format("Cannot construct an instance of the module {0}", moduleModel
                    .getName());
            Log.error(errorMessage);
            throw new JarvisException(errorMessage, e);
        }
    }

    /**
     * Returns the {@link DialogFlowApi} used to query the DialogFlow framework.
     * <p>
     * <b>Note:</b> this method is designed to ease debugging and testing, direct interactions with the DialogFlow
     * API may create consistency issues. In particular, jarvis does not ensure that {@link JarvisAction}s will be
     * triggered in case of direct queries to the DialogFlow API.
     *
     * @return the {@link DialogFlowApi} used to query the DialogFlow framework
     */
    public DialogFlowApi getDialogFlowApi() {
        return dialogFlowApi;
    }

    /**
     * Returns the {@link IntentDefinitionRegistry} associated to this instance.
     * <p>
     * This registry is used to cache {@link fr.zelus.jarvis.intent.IntentDefinition} from the input
     * {@link OrchestrationModel} and provides utility methods to retrieve specific
     * {@link fr.zelus.jarvis.intent.IntentDefinition} and clear the cache.
     *
     * @return the {@link IntentDefinitionRegistry} associated to this instance
     */
    public IntentDefinitionRegistry getIntentDefinitionRegistry() {
        return intentDefinitionRegistry;
    }

    /**
     * Returns the {@link JarvisModuleRegistry} associated to this instance.
     * <p>
     * This registry is used to cache loaded module, and provides utility method to retrieve, unregister, and clear
     * them.
     *
     * @return the {@link JarvisModuleRegistry} associated to this instance
     */
    public JarvisModuleRegistry getJarvisModuleRegistry() {
        return jarvisModuleRegistry;
    }

    /**
     * Returns the {@link SessionName} representing the current DialogFlow session.
     * <p>
     * <b>Note:</b> this method is designed to ease testing, and should not be accessed by client applications. In
     * particular, jarvis does not ensure that {@link JarvisAction}s will be triggered in case of direct queries to
     * the DialogFlow API.
     *
     * @return the {@link SessionName} representing the current DialogFlow session
     */
    protected SessionName getSessionName() {
        return sessionName;
    }

    /**
     * Returns the {@link ExecutorService} used to process {@link JarvisAction}s.
     * <p>
     * <b>Note:</b> this method is designed to ease testing, and should not be accessed by client applications.
     * Manipulating {@link JarvisCore}'s {@link ExecutorService} may create consistency issues on currently executed
     * {@link JarvisAction}s.
     *
     * @return the {@link ExecutorService} used to process {@link JarvisAction}s
     */
    protected ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Shuts down the {@link JarvisCore} and the underlying engines.
     * <p>
     * This method shuts down the underlying {@link DialogFlowApi}, unloads all the {@link JarvisModule}s associated to
     * this instance, unregisters the {@link fr.zelus.jarvis.intent.IntentDefinition} from the associated
     * {@link IntentDefinitionRegistry}, and shuts down the {@link #executorService}.
     * <p>
     * Once shutdown, the {@link JarvisCore} instance can not be retrieved using {@link JarvisCore#getInstance()}
     * static method.
     * <p>
     * <b>Note:</b> calling this method invalidates the DialogFlow connection, and thus shuts down intent detections
     * and voice recognitions features. New {@link JarvisAction}s cannot be processed either.
     *
     * @see DialogFlowApi#shutdown()
     */
    public void shutdown() {
        if (isShutdown()) {
            throw new JarvisException("Cannot perform shutdown, JarvisCore is already shutdown");
        }
        // Shutdown the executor first in case there are running tasks using the DialogFlow API.
        this.executorService.shutdownNow();
        this.dialogFlowApi.shutdown();
        this.sessionName = null;
        this.getJarvisModuleRegistry().clearJarvisModules();
        this.getIntentDefinitionRegistry().clearIntentDefinitions();
        if (INSTANCE.equals(this)) {
            INSTANCE = null;
        } else {
            Log.warn("The globally registered JarvisCore instance ({0}) is different from this one ({1}), skipping " +
                    "global instance reset", INSTANCE, this);
        }
    }

    /**
     * Returns whether the {@link JarvisCore} client is shutdown.
     * <p>
     * This class is considered as shutdown if either its underlying {@link ExecutorService} or {@link DialogFlowApi}
     * is shutdown, or if its {@link SessionName} is {@code null}.
     *
     * @return {@code true} if the {@link JarvisCore} client is shutdown, {@code false} otherwise
     */
    public boolean isShutdown() {
        return executorService.isShutdown() || dialogFlowApi.isShutdown() || isNull(sessionName);
    }

    /**
     * Handles an input {@code message} by executing the associated {@link JarvisAction}s.
     * <p>
     * The input {@code message} is forwarded to the underlying {@link DialogFlowApi} that takes care of retrieving
     * the corresponding intent (if any).
     * <p>
     * This method relies on the {@link OrchestrationService} instance to retrieve the {@link JarvisAction}
     * associated to the extracted intent. These {@link JarvisAction}s are then submitted to the local
     * {@link #executorService} that takes care of executing them in a separated thread.
     *
     * @param message the textual input to process
     * @throws NullPointerException if the provided {@code message} is {@code null}
     * @see JarvisAction
     */
    public void handleMessage(String message) {
        checkNotNull(message, "Cannot handle null message");
        RecognizedIntent intent = dialogFlowApi.getIntent(message, sessionName);
        boolean handled = false;

        List<JarvisAction> jarvisActions = orchestrationService.getActionsFromIntent(intent);
        if (jarvisActions.isEmpty()) {
            Log.warn("The intent {0} is not associated to any action", intent.getDefinition().getName());
        }
        for (JarvisAction action : jarvisActions) {
            executorService.submit(action);
        }
    }
}
