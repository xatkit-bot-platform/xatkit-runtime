package com.xatkit.util;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.xatkit.common.CommonPackage;
import com.xatkit.core.RuntimePlatformRegistry;
import com.xatkit.core.XatkitCore;
import com.xatkit.core.XatkitException;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.execution.ExecutionPackage;
import com.xatkit.intent.IntentPackage;
import com.xatkit.intent.Library;
import com.xatkit.language.common.CommonStandaloneSetup;
import com.xatkit.language.execution.ExecutionRuntimeModule;
import com.xatkit.language.execution.ExecutionStandaloneSetup;
import com.xatkit.language.intent.IntentStandaloneSetup;
import com.xatkit.language.platform.PlatformStandaloneSetup;
import com.xatkit.metamodels.utils.LibraryLoaderUtils;
import com.xatkit.metamodels.utils.PlatformLoaderUtils;
import com.xatkit.platform.PlatformDefinition;
import com.xatkit.platform.PlatformPackage;
import com.xatkit.utils.XatkitImportHelper;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.xbase.XbasePackage;

import javax.annotation.Nonnull;
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
import java.util.List;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;

public class ModelLoader {

    /**
     * The {@link Configuration} key to store the {@link ExecutionModel} to use.
     */
    public static String EXECUTION_MODEL_KEY = "xatkit.execution.model";

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

    private RuntimePlatformRegistry platformRegistry;

    private Injector executionInjector;

    private ResourceSet executionResourceSet;

    /**
     * Creates the {@link ModelLoader} and initializes the metamodels used to load Xatkit models.
     * <p>
     * This method registers the Xatkit language's {@link EPackage}s, and loads the core <i>library</i> and
     * <i>platform</i> {@link Resource}s.
     *
     * @throws XatkitException if an error occurred when loading the {@link Resource}s
     */
    public ModelLoader(@Nonnull RuntimePlatformRegistry platformRegistry) {
        checkNotNull(platformRegistry, "Cannot construct a %s instance with the provided %s %s",
                ModelLoader.class.getSimpleName(), RuntimePlatformRegistry.class.getSimpleName(), platformRegistry);
        this.platformRegistry = platformRegistry;
        EPackage.Registry.INSTANCE.put(XbasePackage.eINSTANCE.getNsURI(), XbasePackage.eINSTANCE);
        EPackage.Registry.INSTANCE.put(CommonPackage.eINSTANCE.getNsURI(), CommonPackage.eINSTANCE);
        EPackage.Registry.INSTANCE.put(IntentPackage.eINSTANCE.getNsURI(), IntentPackage.eINSTANCE);
        EPackage.Registry.INSTANCE.put(PlatformPackage.eINSTANCE.getNsURI(), PlatformPackage.eINSTANCE);
        EPackage.Registry.INSTANCE.put(ExecutionPackage.eINSTANCE.getNsURI(), ExecutionPackage.eINSTANCE);

        CommonStandaloneSetup.doSetup();
        IntentStandaloneSetup.doSetup();
        PlatformStandaloneSetup.doSetup();
        ExecutionStandaloneSetup.doSetup();

        executionInjector = Guice.createInjector(new ExecutionRuntimeModule());
        executionResourceSet = executionInjector.getInstance(XtextResourceSet.class);
        /*
         * Share the ResourceSet with the ImportRegistry. This way platforms loaded from both sides can be accessed
         * by the Xatkit runtime component.
         */
        this.loadCoreLibraries();
        this.loadCorePlatforms();
    }

    /**
     * Returns the {@link Injector} created from the {@link ExecutionRuntimeModule}.
     * <p>
     * This {@link Injector} can be used to inject execution-related objects or Xtext-related objects (e.g. Xbase
     * dependencies).
     *
     * @return the {@link Injector} created from the {@link ExecutionRuntimeModule}
     */
    public Injector getExecutionInjector() {
        return this.executionInjector;
    }


    /**
     * Loads the{@link ExecutionModel} from the provided {@code configuration}.
     * <p>
     * This method loads the custom <i>platforms</i> and <i>libraries</i> specified in the {@code configuration}, and
     * loads the {@link ExecutionModel} located at {@link ModelLoader#EXECUTION_MODEL_KEY}.
     * <p>
     * <b>Note</b>: the value set for {@link ModelLoader#EXECUTION_MODEL_KEY} can be either a path to a {@link File},
     * an {@link URI}, or an in-memory {@link ExecutionModel}.
     * <p>
     * <i>Custom platforms</i> loading is done by searching in the provided {@link Configuration} file the entries
     * starting with {@link #CUSTOM_PLATFORMS_KEY_PREFIX}. These entries are handled as paths to the
     * <i>custom platform</i> {@link Resource}s, and are configured to be the target of the corresponding custom
     * platform proxies in the provided {@link ExecutionModel}.
     * <p>
     * <i>Custom libraries</i> loading is done by searching in the provided {@link Configuration} file the entries
     * starting with {@link #CUSTOM_LIBRARIES_KEY_PREFIX}. These entries are handled as paths to the
     * <i>custom library</i> {@link Resource}s, and are configured to be the target of the corresponding custom
     * library proxies in the provided {@link ExecutionModel}.
     *
     * @param configuration the {@link Configuration} to retrieve the {@link ExecutionModel} from
     * @return the {@link ExecutionModel} loaded from the provided {@code configuration}
     * @throws XatkitException      if an error occurred while loading the {@link ExecutionModel}
     * @throws NullPointerException if the provided {@code property} is {@code null}
     */
    public ExecutionModel loadExecutionModel(Configuration configuration) {
        this.loadCustomLibraries(configuration);
        this.loadCustomPlatforms(configuration);
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
                throw new XatkitException(MessageFormat.format("Cannot load the {0} at the given location: {1}",
                        ExecutionModel.class.getSimpleName(), uri.toString()), e);
            }
            executionInjector.injectMembers(executionModelResource);
            EcoreUtil.resolveAll(executionModelResource);
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
     * Loads the core <i>libraries</i> in this class' {@link ResourceSet}.
     * <p>
     * <i>Core libraries</i> are loaded from {@code <xatkit>/plugins/libraries}, where {@code <xatkit>} is the Xatkit
     * installation directory. This method crawls the sub-directories and loads all the {@code .xmi} files as libraries.
     *
     * @see #loadCoreResources(String, String, String, Class)
     */
    private void loadCoreLibraries() {
        Log.info("Loading Xatkit core libraries");
        loadCoreResources("plugins" + File.separator + "libraries", LibraryLoaderUtils.CORE_LIBRARY_PATHMAP, ".intent"
                , Library.class);
    }

    /**
     * Loads the core <i>platforms</i>.
     * <p>
     * <i>Core platforms</i> are loaded from {@code <xatkit>/plugins/platforms}, where {@code <xatkit>} is the Xatkit
     * installation directory. This method crawls the sub-directories and loads all the {@code .xmi} files as platforms.
     *
     * @see #loadCoreResources(String, String, String, Class)
     */
    private void loadCorePlatforms() {
        Log.info("Loading Xatkit core platforms");
        Collection<PlatformDefinition> loadedPlatforms = loadCoreResources("plugins" + File.separator + "platforms",
                PlatformLoaderUtils.CORE_PLATFORM_PATHMAP, ".platform", PlatformDefinition.class);
        for (PlatformDefinition platformDefinition : loadedPlatforms) {
            this.platformRegistry.registerLoadedPlatformDefinition(platformDefinition);
        }
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
     * @param directoryPath    the path of the directory containing the resources to load (<b>relative</b> to the
     *                         Xatkit
     *                         installation directory)
     * @param pathmapPrefix    the pathmap used to prefix core {@link Resource}'s {@link URI}
     * @param rootElementClass the expected type of  the root element of the loaded {@link Resource}
     * @throws XatkitException if an error occurred when crawling the directory's content or when loading a core
     *                         {@link Resource}
     */
    private <T> Collection<T> loadCoreResources(String directoryPath, String pathmapPrefix, String fileExtension,
                                                Class<T> rootElementClass) {
        File xatkitDirectory;
        try {
            xatkitDirectory = FileUtils.getXatkitDirectory();
        } catch (FileNotFoundException e) {
            Log.warn("Xatkit environment variable not set, no core {0} to import. If this is not expected check" +
                    " this tutorial article to see how to install Xatkit: https://github" +
                    ".com/xatkit-bot-platform/xatkit-releases/wiki/Installation", rootElementClass.getSimpleName());
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>();
        try {
            Files.walk(Paths.get(xatkitDirectory.getAbsolutePath() + File.separator + directoryPath), Integer.MAX_VALUE)
                    .filter(filePath ->
                            !Files.isDirectory(filePath) && filePath.toString().endsWith(fileExtension)
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
                    if (rootElementClass.isInstance(rootElement)) {
                        Log.info("\t{0} loaded", EMFUtils.getName(rootElement));
                        Log.debug("\tPath: {0}", resourcePath);
                        result.add((T) rootElement);
                    } else {
                        throw new XatkitException(MessageFormat.format("Cannot load the resource at {0}, expected" +
                                        " a {1} root element but found {2}", resourcePath,
                                rootElementClass.getSimpleName(), rootElement.eClass().getName()));
                    }
                } catch (IOException e) {
                    throw new XatkitException(MessageFormat.format("An error occurred when loading the {0}, see " +
                            "attached exception", resourcePath), e);
                }
            });
            return result;
        } catch (IOException e) {
            throw new XatkitException(MessageFormat.format("An error occurred when crawling the core {0} at the " +
                            "location {1}, see attached exception", rootElementClass.getSimpleName(),
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
     * @param configuration the {@link Configuration} to retrieve the custom libraries from
     * @see #loadCustomResource(Configuration, String, URI, Class)
     */
    private void loadCustomLibraries(Configuration configuration) {
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
                URI pathmapURI = URI.createURI(LibraryLoaderUtils.CUSTOM_LIBRARY_PATHMAP + libraryName + ".intent");
                Library library = loadCustomResource(configuration, libraryPath, pathmapURI, Library.class);
                XatkitImportHelper.getInstance().ignoreAlias(executionResourceSet, libraryName);
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
     *
     * @param configuration the {@link Configuration} to retrieve the custom platforms from
     * @see #loadCustomResource(Configuration, String, URI, Class)
     */
    private void loadCustomPlatforms(Configuration configuration) {
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
                URI pathmapURI = URI.createURI(PlatformLoaderUtils.CUSTOM_PLATFORM_PATHMAP + platformName +
                        ".platform");
                PlatformDefinition platformDefinition = loadCustomResource(configuration, platformPath, pathmapURI,
                        PlatformDefinition.class);
                XatkitImportHelper.getInstance().ignoreAlias(executionResourceSet, platformName);
                this.platformRegistry.registerLoadedPlatformDefinition(platformDefinition);
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
     * @param configuration       the {@link Configuration} to retrieve the file from
     * @param path                the path of the file to load
     * @param pathmapURI          the pathmap {@link URI} used to load the {@link Resource}
     * @param topLevelElementType the expected type of the top-level element of the loaded {@link Resource}
     * @param <T>                 the type of the top-level element
     * @throws XatkitException if an error occurred when loading the {@link Resource}
     */
    private <T extends EObject> T loadCustomResource(Configuration configuration, String path, URI pathmapURI,
                                                     Class<T> topLevelElementType) {
        /*
         * The provided path is handled as a File path. Loading custom resources from external jars is left for a
         * future release.
         */
        String baseConfigurationPath = configuration.getString(XatkitCore.CONFIGURATION_FOLDER_PATH_KEY, "");
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
            Resource resource = executionResourceSet.getResource(pathmapURI, true);
            T topLevelElement = (T) resource.getContents().get(0);
            Log.info("\t{0} loaded", EMFUtils.getName(topLevelElement));
            Log.debug("\tPath: {0}", resourceFile.toString());
            return topLevelElement;
        } else {
            throw new XatkitException(MessageFormat.format("Cannot load the custom {0}, the provided path {1} is not " +
                    "a valid file", topLevelElementType.getSimpleName(), path));
        }
    }

}
