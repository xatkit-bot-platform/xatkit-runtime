package fr.zelus.jarvis.plugins.wordpress.module;

import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.core.JarvisModule;
import org.apache.commons.configuration2.Configuration;

/**
 * A {@link JarvisModule} class that connects and interacts with the WordPress REST API.
 */
public class WordPressModule extends JarvisModule {

    /**
     * Constructs a new {@link WordPressModule} from the provided {@link JarvisCore} and {@link Configuration}.
     *
     * @param jarvisCore    the {@link JarvisCore} instance associated to this module
     * @param configuration the {@link Configuration} used to retrieve the WordPress REST API information
     * @throws NullPointerException if the provided {@code jarvisCore} or {@code configuration} is {@code null}
     */
    public WordPressModule(JarvisCore jarvisCore, Configuration configuration) {
        super(jarvisCore, configuration);
    }
}
