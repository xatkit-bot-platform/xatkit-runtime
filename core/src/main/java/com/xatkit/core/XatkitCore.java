package com.xatkit.core;

import com.xatkit.common.Instruction;
import com.xatkit.core.platform.Formatter;
import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.platform.io.RuntimeEventProvider;
import com.xatkit.core.recognition.IntentRecognitionProvider;
import com.xatkit.core.recognition.IntentRecognitionProviderException;
import com.xatkit.core.recognition.IntentRecognitionProviderFactory;
import com.xatkit.core.recognition.dialogflow.DialogFlowException;
import com.xatkit.core.server.XatkitServer;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.execution.ActionInstance;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.execution.ExecutionPackage;
import com.xatkit.execution.ExecutionRule;
import com.xatkit.intent.Context;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.EventInstance;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.IntentPackage;
import com.xatkit.intent.Library;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.metamodels.utils.LibraryLoaderUtils;
import com.xatkit.metamodels.utils.PlatformLoaderUtils;
import com.xatkit.platform.ActionDefinition;
import com.xatkit.platform.EventProviderDefinition;
import com.xatkit.platform.PlatformDefinition;
import com.xatkit.platform.PlatformPackage;
import com.xatkit.util.EMFUtils;
import com.xatkit.util.FileUtils;
import com.xatkit.util.Loader;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
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
 * @see RuntimePlatformRegistry
 * @see ExecutionService
 * @see RuntimePlatform
 */
public class XatkitCore {

    /**
     * The {@link Configuration} key to store the {@link ExecutionModel} to use.
     *
     * @see #XatkitCore(Configuration)
     */
    public static String EXECUTION_MODEL_KEY = "xatkit.execution.model";

    /**
     * The {@link Configuration} key to store the configuration folder path.
     */
    public static String CONFIGURATION_FOLDER_PATH_KEY = "xatkit.core.configuration.path";

    /**
     * The {@link Configuration} key prefix to store the custom platform paths.
     * <p>
     * This prefix is used to specify the paths of the custom platforms that are needed by the provided
     * {@link ExecutionModel}. Note that custom platform path properties are only required if the
     * {@link ExecutionModel} defines an {@code alias} for the imported platform models. {@link ExecutionModel}s
     * relying on absolute paths are directly loaded from the file system, but are not portable.
     * <p>
     * Custom platform properties must be set following this pattern: {@code CUSTOM_PLATFORMS_KEY_PREFIX + <platform
     * alias> = <platform path>}.
     */
    public static String CUSTOM_PLATFORMS_KEY_PREFIX = "xatkit.platforms.custom.";

    /**
     * The {@link Configuration} key prefix to store the custom library paths.
     * <p>
     * This prefix is used to specify the paths of the custom libraries that are needed by the provided
     * {@link ExecutionModel}. Note that custom library path properties are only required if the
     * {@link ExecutionModel} defines an {@code alias} for the imported library models. {@link ExecutionModel}s
     * relying on absolute paths are directly loaded from the file system, but are not portable.
     * <p>
     * Custom library properties must be set following this pattern: {@code CUSTOM_LIBRARIES_KEY_PREFIX + <library
     * alias> = <library path>}.
     */
    public static String CUSTOM_LIBRARIES_KEY_PREFIX = "xatkit.libraries.custom.";

    /**
     * The {@link Configuration} key prefix to store the abstract platform bindings.
     * <p>
     * This prefix is used to specify the path of the concrete {@link RuntimePlatform} to bind to their abstract
     * definition in the provided execution model. Note that each used abstract platform needs to be bound
     * separately, following this pattern {@code ABSTRACT_PLATFORM_BINDINGS_PREFIX + <abstract platform name> =
     * <concrete platform path>}.
     */
    public static String ABSTRACT_PLATFORM_BINDINGS_PREFIX = "xatkit.platforms.abstract.";

    /**
     * The {@link Configuration} used to initialize this class.
     * <p>
     * This {@link Configuration} is used to load and initialize platforms, see
     * {@link #loadRuntimePlatformFromPlatformModel(PlatformDefinition, Configuration)} for more information on
     * platform loading.
     *
     * @see #loadRuntimePlatformFromPlatformModel(PlatformDefinition, Configuration)
     */
    private Configuration configuration;

    /**
     * The {@link IntentRecognitionProvider} used to compute {@link RecognizedIntent}s from input text.
     */
    private IntentRecognitionProvider intentRecognitionProvider;

    /**
     * The {@link RuntimePlatformRegistry} used to cache loaded {@link RuntimePlatform}, and provides utility method
     * to retrieve, unregister, and clear them.
     *
     * @see #getRuntimePlatformRegistry()
     */
    private RuntimePlatformRegistry runtimePlatformRegistry;

    /**
     * The {@link EventDefinitionRegistry} used to cache {@link EventDefinition}s and
     * {@link IntentDefinition}s from the input {@link ExecutionModel} and provides utility methods to retrieve
     * specific
     * {@link EventDefinition}s and {@link IntentDefinition}s and clear the cache.
     *
     * @see #getEventDefinitionRegistry() ()
     */
    private EventDefinitionRegistry eventDefinitionRegistry;

    /**
     * The {@link ResourceSet} used to load the {@link ExecutionModel} and the referenced models.
     * <p>
     * The {@code executionResourceSet} is initialized with the {@link #initializeExecutionResourceSet()}
     * method, that loads the core platforms models from the classpath, and dynamically retrieves the custom
     * platforms and library models.
     *
     * @see #CUSTOM_PLATFORMS_KEY_PREFIX
     * @see #CUSTOM_LIBRARIES_KEY_PREFIX
     */
    protected ResourceSet executionResourceSet;

    /**
     * The {@link ExecutionService} used to handle {@link EventInstance}s and execute the associated
     * {@link RuntimeAction}s.
     *
     * @see ExecutionService#handleEventInstance(EventInstance, XatkitSession)
     * @see RuntimeAction
     */
    protected ExecutionService executionService;

    /**
     * The {@link Map} used to store and retrieve {@link XatkitSession}s associated to users.
     *
     * @see #getOrCreateXatkitSession(String)
     */
    private Map<String, XatkitSession> sessions;

    /**
     * The {@link XatkitServer} instance used to capture incoming webhooks.
     */
    private XatkitServer xatkitServer;

    /**
     * The {@link Formatter}s used to format execution-level {@link Object}s into {@link String}.
     */
    private Map<String, Formatter> formatters;

    /**
     * Constructs a new {@link XatkitCore} instance from the provided {@code configuration}.
     * <p>
     * The provided {@code configuration} must provide values for the following key (note that additional values may
     * be required according to the used {@link RuntimeEventProvider}s and {@link RuntimePlatform}s):
     * <ul>
     * <li><b>xatkit.execution.model</b>: the {@link ExecutionModel} defining the Intent to
     * Action bindings (or the string representing its location)</li>
     * </ul>
     *
     * @param configuration the {@link Configuration} to construct the instance from
     * @throws NullPointerException if the provided {@code configuration} or one of the mandatory values is {@code null}
     * @throws XatkitException      if the framework is not able to retrieve the {@link ExecutionModel}
     * @see ExecutionModel
     */
    public XatkitCore(Configuration configuration) {
        checkNotNull(configuration, "Cannot construct a %s instance from a null configuration",
                this.getClass().getSimpleName());
        try {
            this.configuration = configuration;
            initializeExecutionResourceSet();
            ExecutionModel executionModel = getExecutionModel(configuration);
            checkNotNull(executionModel, "Cannot construct a %s instance from a null %s", this.getClass()
                    .getSimpleName(), ExecutionModel.class.getSimpleName());
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
            this.runtimePlatformRegistry = new RuntimePlatformRegistry();
            this.executionService = new ExecutionService(executionModel, runtimePlatformRegistry);
            this.eventDefinitionRegistry = new EventDefinitionRegistry();
            this.loadExecutionModel(executionModel);
            xatkitServer.start();
            Log.info("Xatkit bot started");
            if (this.isReactBot()) {
                Log.info("You can test your chatbot here {0} (note that the bot's behavior can be slightly " +
                        "different on the test page than when it is deployed on a server)", "http://localhost:" +
                        this.xatkitServer.getPort() + "/admin");
            }
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
     * Returns whether the bot uses the React platform.
     * <p>
     * A react bot uses (not exclusively) the {@code ReactPlatform} in its execution model. This method is used to
     * deploy a web-page with the bot that can be used to test the modeled bot through its React actions and events.
     * This means that bots using multiple ChatPlatform as their input may work differently on the test page and when
     * deployed.
     *
     * @return {@code true} if the modeled bot uses the React platform, {@code false} otherwise
     */
    private boolean isReactBot() {
        for (RuntimePlatform runtimePlatform : this.runtimePlatformRegistry.getRuntimePlatforms()) {
            /*
             * Check the name instead of the type because ReactPlatform is not part of the main project anymore.
             * TODO this method should be moved in the <i>react-platform</i> project
             */
            if (runtimePlatform.getName().equals("ReactPlatform")) {
                return true;
            }
            /*
             * Do not test the ChatPlatform, if it is bound to ReactPlatform it has been initialized already.
             */
        }
        return false;
    }

    /**
     * Load the runtime instances from the provided {@link ExecutionModel}.
     * <p>
     * This method starts the {@link RuntimeEventProvider}s, builds the {@link RuntimePlatform}s, and enables their
     * {@link RuntimeAction}s from the definitions specified in the provided {@code executionModel}.
     * <p>
     * This method also registers the {@link IntentDefinition}s used in the provided {@code executionModel} in the
     * {@link IntentRecognitionProvider}.
     * <p>
     * <b>Note:</b> the {@link RuntimePlatform}s associated to the provided {@link ExecutionModel} have to be
     * in the classpath in order to be dynamically loaded and instantiated.
     *
     * @param executionModel the {@link ExecutionModel} to load the runtime instances from
     * @see #startEventProviders(ExecutionModel)
     * @see #registerExecutionRuleEvent(ExecutionRule)
     * @see #enableExecutionRuleActions(ExecutionRule)
     */
    private void loadExecutionModel(ExecutionModel executionModel) {
        boolean intentRegistered = false;
        this.startEventProviders(executionModel);
        for (ExecutionRule rule : executionModel.getExecutionRules()) {
            intentRegistered |= this.registerExecutionRuleEvent(rule);
            this.enableExecutionRuleActions(rule);
        }
        if (intentRegistered) {
            /*
             * New intents have been registered in the IntentRecognitionProvider, we should explicitly ask the
             * ML Engine to train in order to take them into account.
             */
            intentRecognitionProvider.trainMLEngine();
        }
    }

    /**
     * Starts the {@link RuntimeEventProvider}s used in the provided {@code executionModel}.
     * <p>
     * This method instantiates the {@link RuntimePlatform}s managing the {@link RuntimeEventProvider}s to construct
     * if necessary (i.e. if they have not been instantiated before).
     *
     * @param executionModel the {@link ExecutionModel} to start the {@link RuntimeEventProvider}s from
     */
    private void startEventProviders(ExecutionModel executionModel) {
        for (EventProviderDefinition eventProviderDefinition : executionModel.getEventProviderDefinitions()) {
            /*
             * The EventProviderDefinition is still a proxy, meaning that the proxy resolution failed.
             */
            if (eventProviderDefinition.eIsProxy()) {
                throw new XatkitException(MessageFormat.format("An error occurred when resolving the proxy {0} " +
                        "from the {1}", eventProviderDefinition, ExecutionModel.class.getSimpleName()));
            }
            PlatformDefinition eventProviderPlatform = (PlatformDefinition) eventProviderDefinition.eContainer();
            RuntimePlatform eventProviderRuntimePlatform = this.runtimePlatformRegistry.getRuntimePlatform
                    (eventProviderPlatform.getName());
            if (isNull(eventProviderRuntimePlatform)) {
                eventProviderRuntimePlatform = loadRuntimePlatformFromPlatformModel(eventProviderPlatform,
                        configuration);
            }
            eventProviderRuntimePlatform.startEventProvider(eventProviderDefinition);
        }
    }

    /**
     * Registers the {@link EventDefinition} of the provided {@code rule} to the {@link IntentRecognitionProvider}.
     * <p>
     * If the provided {@code rule} contains an {@link IntentDefinition} this method also attempts to register its
     * contexts if they are required in its training sentences.
     *
     * @param rule the {@link ExecutionRule} to register the {@link EventDefinition} from
     * @return {@code true} if the {@link EventDefinition} was registered in the {@link IntentRecognitionProvider},
     * {@code false} otherwise
     * @see IntentRecognitionProvider#registerEntityDefinition(EntityDefinition)
     * @see IntentRecognitionProvider#registerIntentDefinition(IntentDefinition)
     */
    private boolean registerExecutionRuleEvent(ExecutionRule rule) {
        /*
         * We don't need to check whether the EventDefinition is a proxy, EventDefinitions are contained in
         * EventProviderDefinitions, that have been checked before.
         */
        EventDefinition eventDefinition = rule.getEvent();
        this.eventDefinitionRegistry.registerEventDefinition(eventDefinition);
        Log.info("Registering event {0}", eventDefinition.getName());
        if (eventDefinition instanceof IntentDefinition) {
            IntentDefinition intentDefinition = (IntentDefinition) eventDefinition;
            for (Context outContext : intentDefinition.getOutContexts()) {
                for (ContextParameter parameter : outContext.getParameters()) {
                    try {
                        this.intentRecognitionProvider.registerEntityDefinition(parameter.getEntity()
                                .getReferredEntity());
                    } catch (DialogFlowException e) {
                        Log.warn(e.getMessage());
                    }
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
     * Enables the {@link RuntimeAction}s associated to the provided {@code rule}.
     * <p>
     * This method retrieves all the {@link ActionInstance}s used in the provided {@code rule} and enable through
     * their containing {@link RuntimePlatform}.
     * <p>
     * This method instantiates the {@link RuntimePlatform}s containing the {@link RuntimeAction}s associated to the
     * retrieved {@link ActionInstance} if necessary (i.e. if they have not been initialized before).
     *
     * @param rule the {@link ExecutionRule} to enable the {@link RuntimeAction}s from
     */
    private void enableExecutionRuleActions(ExecutionRule rule) {
        /*
         * Load the action platforms
         */
        for (ActionInstance actionInstance : getActionInstancesFromRule(rule)) {
            ActionDefinition actionDefinition = actionInstance.getAction();
            /*
             * The Action is still a proxy, meaning that the proxy resolution failed.
             */
            if (actionDefinition.eIsProxy()) {
                throw new XatkitException(MessageFormat.format("An error occurred when resolving the proxy " +
                        "{0} from the {1}", actionDefinition, ExecutionModel.class.getSimpleName()));
            }
            PlatformDefinition platform = (PlatformDefinition) actionDefinition.eContainer();
            RuntimePlatform runtimePlatform = this.runtimePlatformRegistry.getRuntimePlatform(platform
                    .getName());
            if (isNull(runtimePlatform)) {
                runtimePlatform = loadRuntimePlatformFromPlatformModel(platform, configuration);
            }
            runtimePlatform.enableAction(actionDefinition);
        }
    }

    /**
     * Returns the {@link ActionInstance}s contained in the provided {@code rule}.
     * <p>
     * This method searches in the full AST of the provided {@code rule}, and returns any {@link ActionInstance}
     * transitively contained in it.
     *
     * @param rule the {@link ExecutionRule} to to retrieve the {@link ActionInstance}s from
     * @return a {@link List} of {@link ActionInstance} contained in the provided {@code rule}
     */
    private List<ActionInstance> getActionInstancesFromRule(ExecutionRule rule) {
        List<ActionInstance> result = new ArrayList<>();
        for (Instruction i : rule.getInstructions()) {
            if (i instanceof ActionInstance) {
                result.add((ActionInstance) i);
            } else {
                Iterator<EObject> iContents = i.eAllContents();
                while (iContents.hasNext()) {
                    EObject content = iContents.next();
                    if (content instanceof ActionInstance) {
                        result.add((ActionInstance) content);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Retrieves the {@link ExecutionModel} from the provided {@code property}.
     * <p>
     * This method checks if the provided {@code property} is already an in-memory {@link ExecutionModel}
     * instance, or if it is defined by a {@link String} or an {@link URI} representing the path of the model. In
     * that case, the method attempts to load the model at the provided location and returns it.
     * <p>
     * This method supports loading of model path defined by {@link String}s and {@link URI}s. Support for additional
     * types is planned in the next releases.
     *
     * @param configuration the {@link Configuration} to retrieve the {@link ExecutionModel} from
     * @return the {@link ExecutionModel} from the provided {@code property}
     * @throws XatkitException      if the provided {@code property} type is not handled, if the
     *                              underlying {@link Resource} cannot be loaded or if it does not contain an
     *                              {@link ExecutionModel} top-level element, or if the loaded
     *                              {@link ExecutionModel} is empty.
     * @throws NullPointerException if the provided {@code property} is {@code null}
     */
    protected ExecutionModel getExecutionModel(Configuration configuration) {
        Object property = configuration.getProperty(EXECUTION_MODEL_KEY);
        checkNotNull(property, "Cannot retrieve the %s from the property %s, please ensure it is " +
                        "set in the %s property of the Xatkit configuration", ExecutionModel.class.getSimpleName(),
                property, EXECUTION_MODEL_KEY);
        if (property instanceof ExecutionModel) {
            return (ExecutionModel) property;
        } else {
            URI uri;
            if (property instanceof String) {
                File executionModelFile = FileUtils.getFile((String) property, configuration);
                uri = URI.createFileURI(executionModelFile.getAbsolutePath());
            } else if (property instanceof URI) {
                uri = (URI) property;
            } else {
                /*
                 * Unknown property type
                 */
                throw new XatkitException(MessageFormat.format("Cannot retrieve the {0} from the " +
                        "provided property {1}, the property type ({2}) is not supported", ExecutionModel.class
                        .getSimpleName(), property, property.getClass().getSimpleName()));
            }
            Resource executionModelResource;
            try {
                executionModelResource = executionResourceSet.getResource(uri, true);
            } catch (Exception e) {
                e.printStackTrace();
                throw new XatkitException(MessageFormat.format("Cannot load the {0} at the given location: {1}",
                        ExecutionModel.class.getSimpleName(), uri.toString()), e);
            }
            if (isNull(executionModelResource)) {
                throw new XatkitException(MessageFormat.format("Cannot load the provided {0} (uri: {1})",
                        ExecutionModel.class.getSimpleName(), uri));
            }
            if (executionModelResource.getContents().isEmpty()) {
                throw new XatkitException(MessageFormat.format("The provided {0} is empty (uri: {1})", ExecutionModel
                        .class.getSimpleName(), executionModelResource.getURI()));
            }
            ExecutionModel executionModel;
            try {
                executionModel = (ExecutionModel) executionModelResource.getContents().get(0);
            } catch (ClassCastException e) {
                throw new XatkitException(MessageFormat.format("The provided {0} does not contain an " +
                                "{0} top-level element (uri: {1})", ExecutionModel.class.getSimpleName(),
                        executionModelResource.getURI()), e);
            }
            return executionModel;
        }
    }

    /**
     * Creates and initializes the {@link ResourceSet} used to load the provided {@link ExecutionModel}.
     * <p>
     * This method registers the Xatkit language's {@link EPackage}s, loads the core <i>library</i> and
     * <i>platform</i> {@link Resource}s, and loads the custom <i>library</i> and <i>platforms</i> specified in the
     * {@link Configuration}.
     * <p>
     * <i>Core platforms</i> loading is done by searching in the classpath the {@code core_platforms/platforms/} folder,
     * and loads each {@code xmi} file it contains as a platform {@link Resource}. Note that this method loads
     * <b>all</b> the <i>core platforms</i> {@link Resource}s, even if they are not used in the application's
     * {@link ExecutionModel}.
     * <p>
     * <b>Note:</b> this method loads the {@code platforms/} folder from the {@code core_platforms} jar file if the
     * application is executed in a standalone mode. In a development environment (i.e. with all the project
     * sources imported) this method will retrieve the {@code platforms/} folder from the local installation, if it
     * exist.
     * <p>
     * <i>Custom platforms</i> loading is done by searching in the provided {@link Configuration} file the entries
     * starting with {@link #CUSTOM_PLATFORMS_KEY_PREFIX}. These entries are handled as absolute paths to the
     * <i>custom platform</i> {@link Resource}s, and are configured to be the target of the corresponding custom
     * platform proxies in the provided {@link ExecutionModel}.
     * <p>
     * <i>Custom libraries</i> loading is done by searching in the provided {@link Configuration} file the entries
     * starting with {@link #CUSTOM_LIBRARIES_KEY_PREFIX}. These entries are handled as absolute paths to the
     * <i>custom library</i> {@link Resource}s, and are configured to be the target of the corresponding custom
     * library proxies in the provided {@link ExecutionModel}.
     * <p>
     * This method ensures that the loaded {@link Resource}s corresponds to the {@code pathmaps} specified in the
     * provided {@code xmi} file. See
     * {@link LibraryLoaderUtils#CUSTOM_LIBRARY_PATHMAP}, {@link PlatformLoaderUtils#CORE_PLATFORM_PATHMAP} and
     * {@link PlatformLoaderUtils#CUSTOM_PLATFORM_PATHMAP} for further information.
     *
     * @throws XatkitException if an error occurred when loading the {@link Resource}s
     * @see #loadCoreLibraries()
     * @see #loadCustomLibraries()
     * @see #loadCorePlatforms()
     * @see #loadCustomPlatforms()
     * @see PlatformLoaderUtils#CORE_PLATFORM_PATHMAP
     * @see PlatformLoaderUtils#CUSTOM_PLATFORM_PATHMAP
     * @see LibraryLoaderUtils#CUSTOM_LIBRARY_PATHMAP
     */
    private void initializeExecutionResourceSet() {
        executionResourceSet = new ResourceSetImpl();
        executionResourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new
                XMIResourceFactoryImpl());
        executionResourceSet.getPackageRegistry().put(ExecutionPackage.eNS_URI, ExecutionPackage.eINSTANCE);
        executionResourceSet.getPackageRegistry().put(IntentPackage.eNS_URI, IntentPackage.eINSTANCE);
        executionResourceSet.getPackageRegistry().put(PlatformPackage.eNS_URI, PlatformPackage.eINSTANCE);
        loadCoreLibraries();
        loadCustomLibraries();
        loadCorePlatforms();
        loadCustomPlatforms();
    }

    /**
     * Loads the core <i>libraries</i> in this class' {@link ResourceSet}.
     * <p>
     * <i>Core libraries</i> are loaded from {@code <xatkit>/plugins/libraries}, where {@code <xatkit>} is the Xatkit
     * installation directory. This method crawls the sub-directories and loads all the {@code .xmi} files as libraries.
     *
     * @see #loadCoreResources(String, String, EClass)
     */
    private void loadCoreLibraries() {
        Log.info("Loading Xatkit core libraries");
        loadCoreResources("plugins" + File.separator + "libraries", LibraryLoaderUtils.CORE_LIBRARY_PATHMAP,
                IntentPackage.eINSTANCE.getLibrary());
    }

    /**
     * Loads the core <i>platforms</i>.
     * <p>
     * <i>Core platforms</i> are loaded from {@code <xatkit>/plugins/platforms}, where {@code <xatkit>} is the Xatkit
     * installation directory. This method crawls the sub-directories and loads all the {@code .xmi} files as platforms.
     *
     * @see #loadCoreResources(String, String, EClass)
     */
    private void loadCorePlatforms() {
        Log.info("Loading Xatkit core platforms");
        loadCoreResources("plugins" + File.separator + "platforms", PlatformLoaderUtils.CORE_PLATFORM_PATHMAP,
                PlatformPackage.eINSTANCE.getPlatformDefinition());
    }

    /**
     * Loads the core {@link Resource}s in the provided {@code directoryPath}, using the specified {@code
     * pathmapPrefix}.
     * <p>
     * This method crawls the content of the provided {@code directoryPath} (including sub-folders), and tries to
     * load each file as an EMF {@link Resource} using the specified {@code pathmapPrefix}. This method also verifies
     * that the root element of the loaded {@link Resource} is an instance of the provided {@code rootElementEClass}
     * in order to prevent invalid resource loading.
     * <p>
     * <b>Note</b>: {@code directoryPath} is relative to the Xatkit installation directory, for example loading
     * resources located in {@code xatkit/foo/bar} is done by using the {@code foo/bar} {@code directoryPath}.
     *
     * @param directoryPath     the path of the directory containing the resources to load (<b>relative</b> to the
     *                          Xatkit
     *                          installation directory)
     * @param pathmapPrefix     the pathmap used to prefix core {@link Resource}'s {@link URI}
     * @param rootElementEClass the expected type of  the root element of the loaded {@link Resource}
     * @throws XatkitException if an error occurred when crawling the directory's content or when loading a core
     *                         {@link Resource}
     */
    private void loadCoreResources(String directoryPath, String pathmapPrefix, EClass rootElementEClass) {
        File xatkitDirectory;
        try {
            xatkitDirectory = FileUtils.getXatkitDirectory();
        } catch (FileNotFoundException e) {
            Log.warn("Xatkit environment variable not set, no core {0} to import. If this is not expected check" +
                    " this tutorial article to see how to install Xatkit: https://github" +
                    ".com/xatkit-bot-platform/xatkit-releases/wiki/Installation", rootElementEClass.getName());
            return;
        }
        try {
            Files.walk(Paths.get(xatkitDirectory.getAbsolutePath() + File.separator + directoryPath), Integer.MAX_VALUE)
                    .filter(filePath ->
                            !Files.isDirectory(filePath) && filePath.toString().endsWith(".xmi")
                    ).forEach(resourcePath -> {
                try {
                    InputStream is = Files.newInputStream(resourcePath);
                    URI resourceURI = URI.createURI(resourcePath.getFileName().toString());
                    URI resourcePathmapURI = URI.createURI(pathmapPrefix + resourcePath.getFileName());
                    executionResourceSet.getURIConverter().getURIMap().put(resourcePathmapURI, resourceURI);
                    Resource resource = executionResourceSet.createResource(resourcePathmapURI);
                    resource.load(is, Collections.emptyMap());
                    is.close();
                    /*
                     * Check that the top level element in the resource is an instance of the provided
                     * rootElementClass.
                     */
                    EObject rootElement = resource.getContents().get(0);
                    if (rootElementEClass.isInstance(rootElement)) {
                        Log.info("\t{0} loaded", EMFUtils.getName(rootElement));
                        Log.debug("\tPath: {0}", resourcePath);
                    } else {
                        throw new XatkitException(MessageFormat.format("Cannot load the resource at {0}, expected" +
                                        " a {1} root element but found {2}", resourcePath, rootElementEClass.getName(),
                                rootElement.eClass().getName()));
                    }
                } catch (IOException e) {
                    throw new XatkitException(MessageFormat.format("An error occurred when loading the {0}, see " +
                            "attached exception", resourcePath), e);
                }
            });
        } catch (IOException e) {
            throw new XatkitException(MessageFormat.format("An error occurred when crawling the core {0} at the " +
                            "location {1}, see attached exception", rootElementEClass.getName(),
                    xatkitDirectory.getAbsolutePath()), e);
        }
    }

    /**
     * Loads the custom <i>libraries</i> specified in the Xatkit {@link Configuration}.
     * <p>
     * <i>Custom libraries</i> loading is done by searching in the provided {@link Configuration} file the entries
     * starting with {@link #CUSTOM_LIBRARIES_KEY_PREFIX}. These entries are handled as absolute paths to the
     * <i>custom library</i> {@link Resource}s, and are loaded using the
     * {@link LibraryLoaderUtils#CUSTOM_LIBRARY_PATHMAP} pathmap prefix, enabling proxy resolution from the provided
     * {@link ExecutionModel}.
     *
     * @see #loadCustomResource(String, URI, Class)
     */
    private void loadCustomLibraries() {
        Log.info("Loading Xatkit custom libraries");
        configuration.getKeys().forEachRemaining(key -> {
            /*
             * Also accept upper case with '.' replaced by '_', this is the case when running Xatkit from environment
             * variables.
             */
            if (key.startsWith(CUSTOM_LIBRARIES_KEY_PREFIX)
                    || key.startsWith(CUSTOM_LIBRARIES_KEY_PREFIX.toUpperCase().replaceAll("\\.", "_"))) {
                String libraryPath = configuration.getString(key);
                /*
                 * This works with XatkitEnvironmentConfiguration because the key length is preserved.
                 * TODO find a better fix
                 */
                String libraryName = key.substring(CUSTOM_LIBRARIES_KEY_PREFIX.length());
                URI pathmapURI = URI.createURI(LibraryLoaderUtils.CUSTOM_LIBRARY_PATHMAP + libraryName);
                loadCustomResource(libraryPath, pathmapURI, Library.class);
            }
        });
    }

    /**
     * Loads the custom <i>platforms</i> specified in the Xatkit {@link Configuration}.
     * <p>
     * <i>Custom platforms</i> loading is done by searching in the provided {@link Configuration} file the entries
     * starting with {@link #CUSTOM_PLATFORMS_KEY_PREFIX}. These entries are handled as absolute paths to the
     * <i>custom platform</i> {@link Resource}s, and are loaded using the
     * {@link PlatformLoaderUtils#CUSTOM_PLATFORM_PATHMAP} pathmap prefix, enabling proxy resolution from the
     * provided {@link ExecutionModel}.
     */
    private void loadCustomPlatforms() {
        Log.info("Loading Xatkit custom platforms");
        configuration.getKeys().forEachRemaining(key -> {
            /*
             * Also accept upper case with '.' replaced by '_', this is the case when running Xatkit from environment
             * variables.
             */
            if (key.startsWith(CUSTOM_PLATFORMS_KEY_PREFIX)
                    || key.startsWith(CUSTOM_PLATFORMS_KEY_PREFIX.toUpperCase().replaceAll("\\.", "_"))) {
                String platformPath = configuration.getString(key);
                /*
                 * This works with XatkitEnvironmentConfiguration because the key length is preserved.
                 * TODO find a better fix
                 */
                String platformName = key.substring(CUSTOM_PLATFORMS_KEY_PREFIX.length());
                URI pathmapURI = URI.createURI(PlatformLoaderUtils.CUSTOM_PLATFORM_PATHMAP + platformName);
                loadCustomResource(platformPath, pathmapURI, PlatformDefinition.class);
            }
        });
    }

    /**
     * Loads the custom {@link Resource} located at the provided {@code path}, using the given {@code pathmapURI}.
     * <p>
     * This method checks that the provided {@code path} is a valid {@link File}, and tries to load it as an EMF
     * {@link Resource} using the specified {@code pathmapURI}. This method also verifies that the top-level element
     * of the loaded {@link Resource} is an instance of the provided {@code topLevelElementType} in order to prevent
     * invalid {@link Resource} loading.
     * <p>
     * Note that the provided {@code path} can be either absolute or relative. Relative paths are resolved against
     * the configuration location.
     * <p>
     * The provided {@code pathmapURI} allows to load custom {@link Resource}s independently of their concrete
     * location. This allows to load custom {@link Resource}s from {@link ExecutionModel} designed in a different
     * environment.
     *
     * @param path                the path of the file to load
     * @param pathmapURI          the pathmap {@link URI} used to load the {@link Resource}
     * @param topLevelElementType the expected type of the top-level element of the loaded {@link Resource}
     * @param <T>                 the type of the top-level element
     * @throws XatkitException if an error occurred when loading the {@link Resource}
     */
    private <T extends EObject> void loadCustomResource(String path, URI pathmapURI, Class<T> topLevelElementType) {
        /*
         * The provided path is handled as a File path. Loading custom resources from external jars is left for a
         * future release.
         */
        String baseConfigurationPath = this.configuration.getString(CONFIGURATION_FOLDER_PATH_KEY, "");
        File resourceFile = new File(path);
        /*
         * '/' comparison is a quickfix for windows, see https://bugs.openjdk.java.net/browse/JDK-8130462
         */
        if (!resourceFile.isAbsolute() && path.charAt(0) != '/') {
            resourceFile = new File(baseConfigurationPath + File.separator + path);
        }
        if (resourceFile.exists() && resourceFile.isFile()) {
            URI resourceFileURI = URI.createFileURI(resourceFile.getAbsolutePath());
            executionResourceSet.getURIConverter().getURIMap().put(pathmapURI, resourceFileURI);
            Resource resource = executionResourceSet.getResource(resourceFileURI, true);
            T topLevelElement = (T) resource.getContents().get(0);
            Log.info("\t{0} loaded", EMFUtils.getName(topLevelElement));
            Log.debug("\tPath: {0}", resourceFile.toString());
        } else {
            throw new XatkitException(MessageFormat.format("Cannot load the custom {0}, the provided path {1} is not " +
                    "a valid file", topLevelElementType.getSimpleName(), path));
        }
    }

    /**
     * Loads the {@link RuntimePlatform} defined by the provided {@link PlatformDefinition} and registers it.
     * <p>
     * This method searches in the classpath a {@link Class} matching the input
     * {@link PlatformDefinition#getRuntimePath()} value and calls its default constructor.
     * <p>
     * The constructed {@link RuntimePlatform} is registered in the {@link RuntimePlatformRegistry} using its {@code
     * name}. Note that if the provided {@code platformDefinition} is {@code abstract} the constructed
     * {@link RuntimePlatform} is registered with the abstract {@link PlatformDefinition}'s name in order to
     * correctly retrieve its actions when executing abstract actions specified in the execution model.
     *
     * @param platformDefinition the xatkit {@link PlatformDefinition} to load
     * @param configuration      the xatkit {@link Configuration} used to retrieve abstract platform bindings
     * @return an instance of the loaded {@link RuntimePlatform}
     * @throws XatkitException if their is no {@link Class} matching the provided {@code platformDefinition} or if the
     *                         {@link RuntimePlatform} can not be constructed
     * @see PlatformDefinition
     * @see RuntimePlatform
     */
    private RuntimePlatform loadRuntimePlatformFromPlatformModel(PlatformDefinition platformDefinition, Configuration
            configuration) throws XatkitException {
        Log.info("Loading RuntimePlatform for {0}", platformDefinition.getName());
        String platformPath = platformDefinition.getRuntimePath();
        if (platformDefinition.isAbstract()) {
            /*
             * The provided platformDefinition is abstract, we need to retrieve its concrete binding from the
             * provided configuration.
             */
            String abstractPlatformDefinitionName = platformDefinition.getName();
            String abstractPlatformBindingKey = ABSTRACT_PLATFORM_BINDINGS_PREFIX + abstractPlatformDefinitionName;
            Log.info("{0} is abstract, retrieving its binding from the configuration", abstractPlatformDefinitionName);
            platformPath = configuration.getString(abstractPlatformBindingKey);
            checkNotNull(platformPath, "Cannot bind the provided path \"%s\" to the abstract platform %s, please " +
                            "provide a non-null path in the configuration with the key %s", platformPath,
                    abstractPlatformDefinitionName, abstractPlatformBindingKey);
        }
        Class<? extends RuntimePlatform> runtimePlatformClass = Loader.loadClass(platformPath, RuntimePlatform.class);
        RuntimePlatform runtimePlatform = Loader.constructRuntimePlatform(runtimePlatformClass, this, configuration);
        this.runtimePlatformRegistry.registerRuntimePlatform(platformDefinition.getName(), runtimePlatform);
        return runtimePlatform;
    }

    /**
     * Returns the underlying {@link ExecutionService}.
     *
     * @return the underlying {@link ExecutionService}
     */
    public ExecutionService getExecutionService() {
        return this.executionService;
    }

    /**
     * Returns the underlying {@link IntentRecognitionProvider}.
     * <p>
     * <b>Note:</b> this method is designed to ease debugging and testing, direct interactions with the
     * {@link IntentRecognitionProvider} API may create consistency issues. In particular, xatkit does not ensure
     * that {@link RuntimeAction}s will be triggered in case of direct queries to the
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
     * {@link ExecutionModel} and provides utility methods to retrieve specific {@link EventDefinition} and
     * {@link IntentDefinition} and clear the cache.
     *
     * @return the {@link EventDefinitionRegistry} associated to this instance
     */
    public EventDefinitionRegistry getEventDefinitionRegistry() {
        return eventDefinitionRegistry;
    }

    /**
     * Returns the {@link RuntimePlatformRegistry} associated to this instance.
     * <p>
     * This registry is used to cache loaded {@link RuntimePlatform}s, and provides utility method to retrieve,
     * unregister, and clear them.
     *
     * @return the {@link RuntimePlatformRegistry} associated to this instance
     */
    public RuntimePlatformRegistry getRuntimePlatformRegistry() {
        return runtimePlatformRegistry;
    }

    /**
     * Returns the {@link XatkitServer} used to capture incoming webhooks.
     *
     * @return the {@link XatkitServer} used to capture incoming webhooks
     */
    public XatkitServer getXatkitServer() {
        return xatkitServer;
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
        Collection<RuntimePlatform> runtimePlatforms = this.getRuntimePlatformRegistry().getRuntimePlatforms();
        for (RuntimePlatform runtimePlatform : runtimePlatforms) {
            runtimePlatform.shutdown();
        }
        this.getRuntimePlatformRegistry().clearRuntimePlatforms();
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
    public XatkitSession getOrCreateXatkitSession(String sessionId) {
        checkNotNull(sessionId, "Cannot create or retrieve the %s from the provided session ID %s", XatkitSession
                .class.getSimpleName(), sessionId);
        XatkitSession session = getXatkitSession(sessionId);
        if (isNull(session)) {
            session = this.intentRecognitionProvider.createSession(sessionId);
            sessions.put(sessionId, session);
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
    public XatkitSession getXatkitSession(String sessionId) {
        checkNotNull(sessionId, "Cannot retrieve a session from null as the session ID");
        return sessions.get(sessionId);
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
