package fr.zelus.jarvis.core_modules;

/**
 * An interface providing {@code pathmap} utility methods to dynamically load core modules.
 */
public interface CoreModulesUtils {

    /**
     * The {@code pathmap} prefix used to map core module resources to their concrete URI.
     * <p>
     * References to core modules can be built using the following code: {@code URI.createURI(CORE_MODULE_PATHMAP +
     * "MyModule.xmi"}. The JarvisCore engine will take care of dynamically loading the modules and mapping their
     * {@code pathmaps} to the concrete resources.
     */
    String CORE_MODULE_PATHMAP = "pathmap://JARVIS_CORE_MODULES/";
}
