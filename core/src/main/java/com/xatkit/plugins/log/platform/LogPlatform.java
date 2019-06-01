package com.xatkit.plugins.log.platform;

import com.xatkit.plugins.log.platform.action.LogAction;
import com.xatkit.plugins.log.platform.action.LogError;
import com.xatkit.plugins.log.platform.action.LogInfo;
import com.xatkit.plugins.log.platform.action.LogWarning;
import com.xatkit.core.JarvisCore;
import com.xatkit.core.platform.RuntimePlatform;
import org.apache.commons.configuration2.Configuration;

/**
 * A {@link RuntimePlatform} concrete implementation providing logging capabilities.
 * <p>
 * This runtimePlatform defines a set of {@link LogAction}s that log messages
 * with various severity levels:
 * <ul>
 * <li>{@link LogInfo}: logs an information message</li>
 * <li>{@link LogWarning}: logs a warning message</li>
 * <li>{@link LogError}: logs an error message</li>
 * </ul>
 * <p>
 * This class is part of jarvis' core platforms, and can be used in an execution model by importing the
 * <i>LogPlatform</i> package.
 *
 * @see LogAction
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
