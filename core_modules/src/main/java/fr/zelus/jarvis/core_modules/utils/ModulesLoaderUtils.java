package fr.zelus.jarvis.core_modules.utils;

/**
 * An interface providing {@code pathmap} utility methods to dynamically load modules.
 */
public interface ModulesLoaderUtils {

    /**
     * The {@code pathmap} prefix used to map core module resources to their concrete URIs.
     * <p>
     * References to core modules can be built using the following code: {@code URI.createURI(CORE_MODULE_PATHMAP +
     * "MyModule.xmi"}. The JarvisCore engine will take care of dynamically loading the modules and mapping their
     * {@code pathmaps} to the concrete resources.
     */
    String CORE_MODULE_PATHMAP = "pathmap://JARVIS_CORE_MODULES/";

    /**
     * The {@code pathmap} prefix used to map custom module resources to their concrete URIs.
     * <p>
     * References to custom modules can be built using the following code : {@code URI.createURI
     * (CUSTOM_MODULE_PATHMAP + "MyModule.xmi"}. The JarvisCore engine will take care of dynamically loading the
     * modules and mapping their {@code pathmaps} to the concrete resources.
     * <p>
     * Custom module resources concrete paths must be specified in the Jarvis configuration using {@code
     * JarvisCore#CUSTOM_MODULES_KEY_PREFIX + <module_name>=<concrete_path>}.
     */
    String CUSTOM_MODULE_PATHMAP = "pathmap://JARVIS_CUSTOM_MODULES/";
}
