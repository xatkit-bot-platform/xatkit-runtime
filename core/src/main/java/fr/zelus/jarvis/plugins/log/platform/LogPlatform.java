package fr.zelus.jarvis.plugins.log.platform;

import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.core.RuntimePlatform;
import org.apache.commons.configuration2.Configuration;

/**
 * A {@link RuntimePlatform} concrete implementation providing logging capabilities.
 * <p>
 * This runtimePlatform defines a set of {@link fr.zelus.jarvis.plugins.log.platform.action.LogAction}s that log messages
 * with various severity levels:
 * <ul>
 * <li>{@link fr.zelus.jarvis.plugins.log.platform.action.LogInfo}: logs an information message</li>
 * <li>{@link fr.zelus.jarvis.plugins.log.platform.action.LogWarning}: logs a warning message</li>
 * <li>{@link fr.zelus.jarvis.plugins.log.platform.action.LogError}: logs an error message</li>
 * </ul>
 * <p>
 * This class is part of jarvis' core platforms, and can be used in an orchestration model by importing the
 * <i>LogPlatform</i> package.
 *
 * @see fr.zelus.jarvis.plugins.log.platform.action.LogAction
 */
public class LogPlatform extends RuntimePlatform {

    /**
     * Constructs a new {@link LogPlatform} instance from the provided {@link JarvisCore} and {@link Configuration}.
     *
     * @param jarvisCore    the {@link JarvisCore} instance associated to this runtimePlatform
     * @param configuration the {@link Configuration} used to initialize the {@link LogPlatform}
     * @throws NullPointerException if the provided {@code jarvisCore} or {@code configuration} is {@code null}
     */
    public LogPlatform(JarvisCore jarvisCore, Configuration configuration) {
        super(jarvisCore, configuration);
    }
}
