package fr.zelus.jarvis.plugins.wordpress.module;

import fr.zelus.jarvis.core.JarvisModule;
import org.apache.commons.configuration2.Configuration;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * A {@link JarvisModule} class that connects and interacts with the WordPress REST API.
 */
public class WordPressModule extends JarvisModule {

    /**
     * Constructs a new {@link WordPressModule} from the provided {@link Configuration}.
     *
     * @param configuration the {@link Configuration} used to retrieve the WordPress REST API information
     * @throws NullPointerException if the provided {@code configuration} is {@code null}
     */
    public WordPressModule(Configuration configuration) {
        super(configuration);
        checkNotNull(configuration, "Cannot construct a WordPressModule from a null configuration");
    }
}
