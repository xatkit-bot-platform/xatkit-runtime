package fr.zelus.jarvis.core;

import com.google.cloud.dialogflow.v2.Intent;
import com.google.cloud.dialogflow.v2.SessionName;
import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.dialogflow.DialogFlowApi;
import fr.zelus.jarvis.module.Module;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;

/**
 * A message broker that receives input messages, retrieve their {@link Intent}s, and dispatch them to the loaded
 * {@link JarvisModule}s.
 * <p>
 * This class takes care of loading the {@link JarvisModule}s from their {@link Module} definition. {@link Module}
 * definition are descriptive objects representing the intents, actions, and concrete {@link JarvisModule} class to
 * load, and are constructed using jarvis' <i>Module DSL</i>.
 *
 * @see Module
 * @see JarvisModule
 */
public class JarvisCore {

    /**
     * The {@link DialogFlowApi} used to access the DialogFlow framework and send user input for {@link Intent}
     * extraction.
     */
    private DialogFlowApi dialogFlowApi;

    /**
     * The DialogFlow session associated to this {@link JarvisCore} instance.
     */
    private SessionName sessionName;

    /**
     * The {@link JarvisModule}s used to handle {@link Intent}s extracted from user input.
     * <p>
     * These {@link JarvisModule}s are loaded from the {@link Module} model definitions provided in this class'
     * constructor using the method {@link #loadJarvisModuleFromModuleModel(Module)}.
     *
     * @see #loadJarvisModuleFromModuleModel(Module)
     * @see #handleMessage(String)
     */
    private List<JarvisModule> modules;

    /**
     * The {@link OrchestrationService} used to find {@link JarvisAction}s to execute when an {@link Intent} is
     * received.
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
     * Constructs a new {@link JarvisCore} with the provided {@code projectId} and {@code languageCode}.
     * <p>
     * Note that this constructor initializes an empty {@link #modules} list. Use
     * {@link #loadModule(Module)} to register additional {@link Module}s. See
     * {@link #JarvisCore(String, String, List)} to construct a {@link JarvisCore} instance with preset
     * {@link Module}s.
     *
     * @param projectId    the unique identifier of the DialogFlow project
     * @param languageCode the code of the language processed by DialogFlow
     * @see #loadModule(Module)
     * @see #unloadModule(Module)
     */
    public JarvisCore(String projectId, String languageCode) {
        this(projectId, languageCode, new ArrayList<>());
    }

    /**
     * Constructs a new {@link JarvisCore} with the provided {@code projectId}, {@code languageCode}, and load the
     * concrete {@link JarvisModule}s described by the provided {@code moduleModels}.
     *
     * @param projectId    the unique identifier of the DialogFlow project
     * @param languageCode the code of the language processed by DialogFlow
     * @param moduleModels the jarvis {@link Module}s to load
     * @throws NullPointerException if the provided {@code projectId}, {@code languageCode}, or {@code moduleModels} is
     *                              {@code null}
     * @see #handleMessage(String)
     */
    public JarvisCore(String projectId, String languageCode, List<Module> moduleModels) {
        checkNotNull(projectId, "Cannot construct a jarvis instance from a null projectId");
        checkNotNull(languageCode, "Cannot construct a jarvis instance from a null language code");
        checkNotNull(moduleModels, "Cannot construct a jarvis instance from a null module list");
        this.dialogFlowApi = new DialogFlowApi(projectId, languageCode);
        this.sessionName = dialogFlowApi.createSession();
        this.modules = new ArrayList<>();
        for (Module module : moduleModels) {
            this.modules.add(loadJarvisModuleFromModuleModel(module));
        }
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
     * Registers a new {@link JarvisModule} to the {@link #modules} list.
     *
     * @param moduleModel the jarvis {@link Module}s to load
     * @throws NullPointerException if the provided {@code moduleModel} is {@code null}
     */
    public void loadModule(Module moduleModel) {
        checkNotNull(moduleModel, "Cannot register the module null");
        this.modules.add(loadJarvisModuleFromModuleModel(moduleModel));
    }

    /**
     * Unregisters a {@link JarvisModule} from the {@link #modules} list.
     *
     * @param moduleModel the jarvis {@link Module} to unload
     * @throws NullPointerException     if the provided {@code moduleModel} is {@code null}
     * @throws IllegalArgumentException if the provided {@code moduleModel} hasn't been removed from the list
     */
    public void unloadModule(Module moduleModel) {
        checkNotNull(moduleModel, "Cannot unregister the module null");
        boolean removed = false;
        Iterator<JarvisModule> moduleIterator = this.modules.iterator();
        while (moduleIterator.hasNext()) {
            JarvisModule module = moduleIterator.next();
            if (module.getName().equals(moduleModel.getName())) {
                moduleIterator.remove();
                removed = true;
                break;
            }
        }
        if (!removed) {
            throw new IllegalArgumentException(MessageFormat.format("Cannot remove {0} from the module list, please " +
                    "ensure that this module is in the list", moduleModel));
        }
    }

    /**
     * Unregisters all the {@link JarvisModule}s.
     */
    public void clearModules() {
        this.modules.clear();
    }

    /**
     * Returns an unmodifiable {@link List} containing the registered {@link #modules}.
     *
     * @return an unmodifiable {@link List} containing the registered {@link #modules}
     */
    public List<JarvisModule> getModules() {
        return Collections.unmodifiableList(modules);
    }

    /**
     * Shuts down the underlying {@link DialogFlowApi}, cleans the {@link #modules} list, and shuts down the
     * {@link #executorService}.
     * <p>
     * <b>Note:</b> calling this method invalidates the DialogFlow connection, and thus shuts down {@link Intent}
     * detections and voice recognitions features. New {@link JarvisAction}s cannot be processed either.
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
        this.clearModules();
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
     * Handles a new input message and dispatch it through the registered {@code modules}.
     * <p>
     * This method relies on the {@link DialogFlowApi} to retrieve the {@link Intent} of the input message, and
     * notifies all the registered modules of the new {@link Intent}.
     *
     * @param message the input message
     * @throws NullPointerException if the provided {@code message} is {@code null}
     * @see JarvisModule#handleIntent(Intent)
     * @see JarvisModule#acceptIntent(Intent)
     */
    public void handleMessage(String message) {
        checkNotNull(message, "Cannot handle null message");
        Intent intent = dialogFlowApi.getIntent(message, sessionName);
        boolean handled = false;

        for (JarvisModule module : modules) {
            if (module.acceptIntent(intent)) {
                JarvisAction action = module.handleIntent(intent);
                /*
                 * There is at least one module that can handle the intent
                 */
                handled = true;
                /*
                 * Submit the action to the executor service and don't wait for its completion.
                 */
                executorService.submit(action);
            }
        }
        if (!handled) {
            /*
             * Log an error if the intent hasn't been handled. Note that not handling an intent is not a text
             * recognition issue on the DialogFlow side (the framework was able to detect an intent), but a jarvis
             * issue: there is no registered module that can handle the intent returned by DialogFlow.
             */
            Log.warn("The intent {0} hasn't been handled, make sure that the corresponding JarvisModule is loaded " +
                    "and registered", intent.getDisplayName());
        }
    }
}
