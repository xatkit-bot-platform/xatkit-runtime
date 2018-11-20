package fr.zelus.jarvis.core_resources.utils;

/**
 * An interface providing {@code pathmap} utility methods to dynamically load libraries.
 */
public interface LibraryLoaderUtils {

    /**
     * The {@code pathmap} prefix used to map core library resources to their concrete URIs.
     * <p>
     * References to core libraries can be built using the following code: {@code URI.createURI(CORE_LIBRARY_PATHMAP
     * + "MyLibrary.xmi"}. The JarvisCore engine will take care of dynamically loading the libraries and mapping
     * their {@code pathmap} to the concrete resources.
     */
    String CORE_LIBRARY_PATHMAP = "pathmap://JARVIS_CORE_LIBRARY/";

    /**
     * The {@code pathmap} prefix used to map custom library resources to their concrete URIs.
     * <p>
     * References to custom libraries can be built using the following code: {@code URI.createURI
     * (CUSTOM_LIBRARY_PATHMAP + "MyLibrary.xmi"}. The JarvisCore engine will take care of dynamically loading the
     * libraries and mapping their {@code pathmaps} to the concrete resources.
     * <p>
     * Custom library resource concrete paths must be specified in the Jarvis configuration using {@code
     * JarvisCore#CUSTOM_LIBRARY_KEY_PREFIX + <library_name>=<concrete_path>}.
     */
    String CUSTOM_LIBRARY_PATHMAP = "pathmap://JARVIS_CUSTOM_LIBRARY/";
}
