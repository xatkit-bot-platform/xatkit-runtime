package fr.zelus.jarvis.core.module.log;

import fr.zelus.jarvis.core.JarvisModule;
import org.apache.commons.configuration2.Configuration;

/**
 * A {@link JarvisModule} concrete implementation providing logging capabilities.
 * <p>
 * This module defines a set of {@link fr.zelus.jarvis.core.module.log.action.LogAction}s that log messages
 * with various severity levels.
 * <p>
 * This class is part of jarvis' core modules, and can be used in an orchestration model by importing the
 * <i>core.LogModule</i> package.
 *
 * @see fr.zelus.jarvis.core.module.log.action.LogAction
 */
public class LogModule extends JarvisModule {

    /**
     * Constructs a new {@link LogModule} instance from the provided {@link Configuration}.
     *
     * @param configuration the {@link Configuration} used to initialize the {@link LogModule}
     */
    public LogModule(Configuration configuration) {
        super(configuration);
    }
}
