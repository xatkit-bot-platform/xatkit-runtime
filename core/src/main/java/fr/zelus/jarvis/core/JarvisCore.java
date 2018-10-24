package fr.zelus.jarvis.core;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.core_modules.CoreModulesUtils;
import fr.zelus.jarvis.intent.*;
import fr.zelus.jarvis.io.EventProvider;
import fr.zelus.jarvis.module.Action;
import fr.zelus.jarvis.module.EventProviderDefinition;
import fr.zelus.jarvis.module.Module;
import fr.zelus.jarvis.module.ModulePackage;
import fr.zelus.jarvis.orchestration.ActionInstance;
import fr.zelus.jarvis.orchestration.OrchestrationLink;
import fr.zelus.jarvis.orchestration.OrchestrationModel;
import fr.zelus.jarvis.orchestration.OrchestrationPackage;
import fr.zelus.jarvis.recognition.IntentRecognitionProvider;
import fr.zelus.jarvis.recognition.IntentRecognitionProviderException;
import fr.zelus.jarvis.recognition.IntentRecognitionProviderFactory;
import fr.zelus.jarvis.server.JarvisServer;
import fr.zelus.jarvis.util.Loader;
import org.apache.commons.configuration2.Configuration;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;

/**
 * The core component of the jarvis framework.
 * <p>
 * This class is constructed from an {@link OrchestrationModel}, that defines the Intent to Action bindings that are
 * executed by the application. Constructing an instance of this class will load the {@link JarvisModule}s used by
 * the provided {@link OrchestrationModel}, and enable the corresponding {@link JarvisAction}s. It also creates an
 * instance of {@link EventDefinitionRegistry} that can be accessed to retrieve and manage {@link EventDefinition} .
 *
 * @see EventDefinitionRegistry
 * @see JarvisModuleRegistry
 * @see OrchestrationService
 * @see JarvisModule
 */
public class JarvisCore {

    /**
     * The {@link Configuration} key to store the {@link OrchestrationModel} to use.
     *
     * @see #JarvisCore(Configuration)
     */
    public static String ORCHESTRATION_MODEL_KEY = "jarvis.orchestration.model";

    /**
     * The {@link Configuration} used to initialize this class.
     * <p>
     * This {@link Configuration} is used to load and initialize modules, see
     * {@link #loadJarvisModuleFromModuleModel(Module)} for more information on module loading.
     *
     * @see #loadJarvisModuleFromModuleModel(Module)
     */
    private Configuration configuration;

    /**
     * The {@link IntentRecognitionProvider} used to compute {@link RecognizedIntent}s from input text.
     */
    private IntentRecognitionProvider intentRecognitionProvider;

    /**
     * The {@link JarvisModuleRegistry} used to cache loaded module, and provides utility method to retrieve,
     * unregister, and clear them.
     *
     * @see #getJarvisModuleRegistry()
     */
    private JarvisModuleRegistry jarvisModuleRegistry;

    /**
     * The {@link EventDefinitionRegistry} used to cache {@link fr.zelus.jarvis.intent.EventDefinition}s and
     * {@link IntentDefinition}s from the input {@link OrchestrationModel} and provides utility methods to retrieve
     * specific
     * {@link EventDefinition}s and {@link IntentDefinition}s and clear the cache.
     *
     * @see #getEventDefinitionRegistry() ()
     */
    private EventDefinitionRegistry eventDefinitionRegistry;

    /**
     * The {@link OrchestrationService} used to handle {@link EventInstance}s and execute the associated
     * {@link JarvisAction}s.
     *
     * @see OrchestrationService#handleEventInstance(EventInstance, JarvisSession)
     * @see JarvisAction
     */
    protected OrchestrationService orchestrationService;

    /**
     * The {@link Map} used to store and retrieve {@link JarvisSession}s associated to users.
     *
     * @see #getOrCreateJarvisSession(String)
     */
    private Map<String, JarvisSession> sessions;

    /**
     * The {@link JarvisServer} instance used to capture incoming webhooks.
     */
    private JarvisServer jarvisServer;

    /**
     * Constructs a new {@link JarvisCore} instance from the provided {@code configuration}.
     * <p>
     * The provided {@code configuration} must provide values for the following key (note that additional values may
     * be required according to the used {@link EventProvider}s and {@link JarvisModule}s):
     * <ul>
     * <li><b>jarvis.orchestration.model</b>: the {@link OrchestrationModel} defining the Intent to
     * Action bindings (or the string representing its location)</li>
     * </ul>
     * <p>
     * The provided {@link OrchestrationModel} defines the Intent to Action bindings that are executed by the
     * application. This constructor takes care of loading the {@link JarvisModule}s associated to the provided
     * {@code orchestrationModel} and enables the corresponding {@link JarvisAction}s.
     * <p>
     * <b>Note:</b> the {@link JarvisModule}s associated to the provided {@code orchestrationModel} have to be
     * in the classpath in order to be dynamically loaded and instantiated.
     *
     * @param configuration the {@link Configuration} to construct the instance from
     * @throws NullPointerException if the provided {@code configuration} or one of the mandatory values is {@code null}
     * @throws JarvisException      if the framework is not able to retrieve the {@link OrchestrationModel}
     * @see OrchestrationModel
     */
    public JarvisCore(Configuration configuration) {
        checkNotNull(configuration, "Cannot construct a jarvis instance from a null configuration");
        this.configuration = configuration;
        OrchestrationModel orchestrationModel = getOrchestrationModel(configuration.getProperty
                (ORCHESTRATION_MODEL_KEY));
        checkNotNull(orchestrationModel, "Cannot construct a jarvis instance from a null orchestration model");
        this.intentRecognitionProvider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(this,
                configuration);
        this.sessions = new HashMap<>();
        this.jarvisModuleRegistry = new JarvisModuleRegistry();
        this.orchestrationService = new OrchestrationService(orchestrationModel, jarvisModuleRegistry);
        this.eventDefinitionRegistry = new EventDefinitionRegistry();
        /*
         * Start the server before processing the EventProviderDefinitions, we need to have a valid JarvisServer
         * instance to call JarvisServer#registerWebhookEventProvider
         */
        this.jarvisServer = new JarvisServer(configuration);
        boolean intentRegistered = false;
        for (EventProviderDefinition eventProviderDefinition : orchestrationModel.getEventProviderDefinitions()) {
            Module eventProviderModule = (Module) eventProviderDefinition.eContainer();
            JarvisModule eventProviderJarvisModule = this.jarvisModuleRegistry.getJarvisModule(eventProviderModule
                    .getName());
            if (isNull(eventProviderJarvisModule)) {
                eventProviderJarvisModule = loadJarvisModuleFromModuleModel(eventProviderModule);
                this.jarvisModuleRegistry.registerJarvisModule(eventProviderJarvisModule);
            }
            eventProviderJarvisModule.startEventProvider(eventProviderDefinition);
        }
        for (OrchestrationLink link : orchestrationModel.getOrchestrationLinks()) {
            /*
             * Extracts the IntentDefinitions
             */
            EventDefinition eventDefinition = link.getEvent();
            this.eventDefinitionRegistry.registerEventDefinition(eventDefinition);
            Log.info("Registering event {0}", eventDefinition.getName());
            if (eventDefinition instanceof IntentDefinition) {
                IntentDefinition intentDefinition = (IntentDefinition) eventDefinition;
                try {
                    this.intentRecognitionProvider.registerIntentDefinition(intentDefinition);
                    intentRegistered = true;
                } catch (IntentRecognitionProviderException e) {
                    Log.warn(e.getMessage());
                }
            }
            /*
             * Load the action modules
             */
            for (ActionInstance actionInstance : link.getActions()) {
                Action action = actionInstance.getAction();
                Module module = (Module) action.eContainer();
                JarvisModule jarvisModule = this.jarvisModuleRegistry.getJarvisModule(module.getName());
                if (isNull(jarvisModule)) {
                    jarvisModule = loadJarvisModuleFromModuleModel(module);
                    this.jarvisModuleRegistry.registerJarvisModule(jarvisModule);
                }
                jarvisModule.enableAction(action);
            }
        }
        if (intentRegistered) {
            /*
             * New intents have been registered in the IntentRecognitionProvider, we should explicitly ask the
             * ML Engine to train in order to take them into account.
             */
            intentRecognitionProvider.trainMLEngine();
        }
        jarvisServer.start();
        Log.info("Jarvis bot started");
    }

    /**
     * Retrieves the {@link OrchestrationModel} from the provided {@code property}.
     * <p>
     * This method checks if the provided {@code property} is already an in-memory {@link OrchestrationModel}
     * instance, or if it is defined by a {@link String} or an {@link URI} representing the path of the model. In
     * that case, the method attempts to load the model at the provided location and returns it.
     * <p>
     * This method supports loading of model path defined by {@link String}s and {@link URI}s. Support for additional
     * types is planned in the next releases.
     * <p>
     * This method loads Jarvis core module {@link Resource}s and add them to the local {@link ResourceSet}, enabling
     * EMF proxy resolution from the loaded {@link OrchestrationModel} to Jarvis core modules.
     *
     * @param property the {@link Object} representing the {@link OrchestrationModel} to extract
     * @return the {@link OrchestrationModel} from the provided {@code property}
     * @throws JarvisException      if the provided {@code property} type is not handled, if the
     *                              underlying {@link Resource} cannot be loaded or if it does not contain an
     *                              {@link OrchestrationModel} top-level
     *                              element, or if the loaded {@link OrchestrationModel} is empty.
     * @throws NullPointerException if the provided {@code property} is {@code null}
     * @see #loadJarvisCoreModuleResources(ResourceSet)
     */
    protected OrchestrationModel getOrchestrationModel(Object property) {
        checkNotNull(property, "Cannot retrieve the OrchestrationModel from the property null, please ensure it is " +
                "set in the %s property of the jarvis configuration", ORCHESTRATION_MODEL_KEY);
        /*
         * Register the EPackages used in the OrchestrationModel
         */
        EPackage.Registry.INSTANCE.put(OrchestrationPackage.eINSTANCE.getNsURI(), OrchestrationPackage.eINSTANCE);
        EPackage.Registry.INSTANCE.put(IntentPackage.eINSTANCE.getNsURI(), IntentPackage.eINSTANCE);
        EPackage.Registry.INSTANCE.put(ModulePackage.eINSTANCE.getNsURI(), ModulePackage.eINSTANCE);
        if (property instanceof OrchestrationModel) {
            return (OrchestrationModel) property;
        } else {
            URI uri;
            if (property instanceof String) {
                uri = URI.createURI((String) property);
            } else if (property instanceof URI) {
                uri = (URI) property;
            } else {
                /*
                 * Unknown property type
                 */
                throw new JarvisException(MessageFormat.format("Cannot retrieve the orchestration model from the " +
                        "provided property {0}, the property type ({1}) is not supported", property, property
                        .getClass().getSimpleName()));
            }
            ResourceSet resourceSet = new ResourceSetImpl();
            resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl
                    ());
            loadJarvisCoreModuleResources(resourceSet);
            Resource orchestrationModelResource;
            try {
                orchestrationModelResource = resourceSet.getResource(uri, true);
            } catch (Exception e) {
                throw new JarvisException(MessageFormat.format("Cannot load the orchestration model at the given " +
                        "location: {0}", uri.toString()), e);
            }
            if (isNull(orchestrationModelResource)) {
                throw new JarvisException(MessageFormat.format("Cannot load the provided orchestration model (uri: " +
                        "{0})", uri));
            }
            if (orchestrationModelResource.getContents().isEmpty()) {
                throw new JarvisException(MessageFormat.format("The provided orchestration model is empty (uri: {0})" +
                        "", orchestrationModelResource.getURI()));
            }
            OrchestrationModel orchestrationModel;
            try {
                orchestrationModel = (OrchestrationModel) orchestrationModelResource.getContents().get(0);
            } catch (ClassCastException e) {
                String errorMessage = MessageFormat.format("The provided orchestration model does not contain a " +
                        "top-level" +
                        " element with the type OrchestrationModel (uri: {0})", orchestrationModelResource.getURI());
                throw new JarvisException(MessageFormat.format("The provided orchestration model does not contain an " +
                        "OrchestrationModel top-level element (uri: {0})", orchestrationModelResource.getURI()), e);
            }
            return orchestrationModel;
        }
    }

    /**
     * Loads the core module {@link Resource}s and adds them to the provided {@code resourceSet}.
     * <p>
     * This method searches in the classpath the {@code modules/} folder, that is typically stored in the {@code
     * core_modules} project. The contents of the folder are then iterated and each {@code xmi} file is loaded as a
     * core module {@link Resource} that is added to the provided {@code resourceSet}
     * <p>
     * This method loads <b>all</b> the core module {@link Resource}s in the provided {@code resourceSet}, even if
     * they are not used in the application's {@link OrchestrationModel}.
     * <p>
     * <b>Note:</b> this method loads the {@code modules/} folder from the {@code core_modules} jar file if the
     * application is executed in a standalone mode. In a development environment (i.e. with all the project sources
     * imported) this method will retrieve the {@code modules/} folder from the local installation, if it exist.
     *
     * @param resourceSet the {@link ResourceSet} to load and register the core module {@link Resource}s from
     * @throws JarvisException if an error occurred when loading the core module resources
     */
    private void loadJarvisCoreModuleResources(ResourceSet resourceSet) {
        checkNotNull(resourceSet, "Cannot load the Jarvis core modules from the provided {0} {1}", ResourceSet.class
                .getSimpleName(), resourceSet);
        Log.info("Loading Jarvis core modules");
        URL url = this.getClass().getClassLoader().getResource("modules/");
        java.net.URI uri;
        try {
            uri = url.toURI();
        } catch (URISyntaxException e) {
            throw new JarvisException("An error occurred when loading the core modules, see attached exception", e);
        }
        /*
         * Jarvis is imported as a jar, we need to setup a FileSystem that handles jar file loading.
         */
        if (uri.getScheme().equals("jar")) {
            Map<String, String> env = new HashMap<>();
            env.put("create", "true");
            try {
                FileSystems.newFileSystem(uri, env);
            } catch (IOException e) {
                throw new JarvisException("An error occurred when loading the core modules, see attached exception", e);
            }
        }
        Path modulesPath = Paths.get(uri);
        try {
            Files.walk(modulesPath, 1).filter(p -> !Files.isDirectory(p)).forEach(modulePath -> {
                try {
                    InputStream is = Files.newInputStream(modulePath);
                    URI modulePathmapURI = URI.createURI(CoreModulesUtils.CORE_MODULE_PATHMAP + modulePath
                            .getFileName());

                    resourceSet.getURIConverter().getURIMap().put(modulePathmapURI, URI.createURI(modulePath
                            .getFileName()
                            .toString()));
                    Resource moduleResource = resourceSet.createResource(modulePathmapURI);
                    moduleResource.load(is, Collections.emptyMap());
                    Module module = (Module) moduleResource.getContents().get(0);
                    is.close();
                    Log.info("Module {0} loaded", module.getName());
                } catch (IOException e) {
                    throw new JarvisException(MessageFormat.format("An error occurred when loading the module {0}, " +
                            "see attached exception", modulePath), e);
                }
            });
        } catch (IOException e) {
            throw new JarvisException("An error occurred when crawling the core modules, see attached exception", e);
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
     * @see JarvisModule
     */
    private JarvisModule loadJarvisModuleFromModuleModel(Module moduleModel) throws JarvisException {
        Log.info("Loading JarvisModule {0}", moduleModel.getName());
        Class<? extends JarvisModule> jarvisModuleClass = Loader.loadClass(moduleModel.getJarvisModulePath(),
                JarvisModule.class);
        return Loader.constructJarvisModule(jarvisModuleClass, this, configuration);
    }

    /**
     * Returns the underlying {@link OrchestrationService}.
     *
     * @return the underlying {@link OrchestrationService}
     */
    public OrchestrationService getOrchestrationService() {
        return this.orchestrationService;
    }

    /**
     * Returns the underlying {@link IntentRecognitionProvider}.
     * <p>
     * <b>Note:</b> this method is designed to ease debugging and testing, direct interactions with the
     * {@link IntentRecognitionProvider} API may create consistency issues. In particular, jarvis does not ensure
     * that {@link JarvisAction}s will be triggered in case of direct queries to the
     * {@link IntentRecognitionProvider} API.
     *
     * @return the underlying {@link IntentRecognitionProvider}
     */
    public IntentRecognitionProvider getIntentRecognitionProvider() {
        return intentRecognitionProvider;
    }

    /**
     * Returns the {@link EventDefinitionRegistry} associated to this instance.
     * <p>
     * This registry is used to cache {@link EventDefinition}s and {@link IntentDefinition}s from the input
     * {@link OrchestrationModel} and provides utility methods to retrieve specific {@link EventDefinition} and
     * {@link IntentDefinition} and clear the cache.
     *
     * @return the {@link EventDefinitionRegistry} associated to this instance
     */
    public EventDefinitionRegistry getEventDefinitionRegistry() {
        return eventDefinitionRegistry;
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
     * Returns the {@link JarvisServer} used to capture incoming webhooks.
     *
     * @return the {@link JarvisServer} used to capture incoming webhooks
     */
    protected JarvisServer getJarvisServer() {
        return jarvisServer;
    }

    /**
     * Shuts down the {@link JarvisCore} and the underlying engines.
     * <p>
     * This method shuts down the underlying {@link IntentRecognitionProvider}, unloads and shuts down all the
     * {@link JarvisModule}s associated to this instance, unregisters the {@link EventDefinition} from the associated
     * {@link EventDefinitionRegistry}, shuts down the {@link OrchestrationService}, and stops the {@link JarvisServer}.
     * <p>
     * <b>Note:</b> calling this method invalidates the {@link IntentRecognitionProvider} connection, and thus shuts
     * down intent recognition features. New {@link JarvisAction}s cannot be processed either.
     *
     * @see IntentRecognitionProvider#shutdown()
     * @see JarvisModule#shutdown()
     * @see EventDefinitionRegistry#unregisterEventDefinition(EventDefinition)
     * @see OrchestrationService#shutdown()
     * @see JarvisServer#stop()
     */
    public void shutdown() {
        Log.info("Shutting down JarvisCore");
        if (isShutdown()) {
            throw new JarvisException("Cannot perform shutdown, JarvisCore is already shutdown");
        }
        /* Shutdown the orchestration service first in case there are running tasks using the IntentRecognitionProvider
         * API.
         */
        this.orchestrationService.shutdown();
        this.jarvisServer.stop();
        this.intentRecognitionProvider.shutdown();
        Collection<JarvisModule> jarvisModules = this.getJarvisModuleRegistry().getModules();
        for (JarvisModule jarvisModule : jarvisModules) {
            jarvisModule.shutdown();
        }
        this.getJarvisModuleRegistry().clearJarvisModules();
        this.getEventDefinitionRegistry().clearEventDefinitions();
    }

    /**
     * Returns whether the {@link JarvisCore} client is shutdown.
     * <p>
     * This class is considered as shutdown if its underlying {@link OrchestrationService},
     * {@link IntentRecognitionProvider}, and {@link JarvisServer} are shutdown.
     *
     * @return {@code true} if the {@link JarvisCore} client is shutdown, {@code false} otherwise
     */
    public boolean isShutdown() {
        return (!jarvisServer.isStarted()) && orchestrationService.isShutdown() && intentRecognitionProvider
                .isShutdown();
    }

    /**
     * Retrieves or creates the {@link JarvisSession} associated to the provided {@code sessionId}.
     * <p>
     * If the {@link JarvisSession} does not exist a new one is created using
     * {@link IntentRecognitionProvider#createSession(String)}.
     *
     * @param sessionId the identifier to get or retrieve a session from
     * @return the {@link JarvisSession} associated to the provided {@code sessionId}
     * @throws NullPointerException if the provided {@code sessionId} is {@code null}
     */
    public JarvisSession getOrCreateJarvisSession(String sessionId) {
        checkNotNull(sessionId, "Cannot create or retrieve the %s from the provided session ID %s", JarvisSession
                .class.getSimpleName(), sessionId);
        JarvisSession session = getJarvisSession(sessionId);
        if (isNull(session)) {
            session = this.intentRecognitionProvider.createSession(sessionId);
            sessions.put(sessionId, session);
        }
        return session;
    }

    /**
     * Returns the {@link JarvisSession} associated to the provided {@code sessionId}
     *
     * @param sessionId the identifier to retrieve the session from
     * @return the {@link JarvisSession} associated to the provided {@code sessionId}
     * @throws NullPointerException if the provided {@code sessionId} is {@code null}
     */
    public JarvisSession getJarvisSession(String sessionId) {
        checkNotNull(sessionId, "Cannot retrieve a session from null as the session ID");
        return sessions.get(sessionId);
    }

    /**
     * Invalidates all the {@link JarvisSession}s and clear the session registry.
     */
    public void clearJarvisSessions() {
        this.sessions.clear();
    }
}
