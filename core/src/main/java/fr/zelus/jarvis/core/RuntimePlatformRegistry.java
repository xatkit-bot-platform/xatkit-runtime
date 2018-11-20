package fr.zelus.jarvis.core;

import fr.zelus.jarvis.platform.PlatformDefinition;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A registry that stores {@link RuntimePlatform}s.
 */
public class RuntimePlatformRegistry {

    /**
     * The {@link Map} used to store the {@link RuntimePlatform}s.
     *
     * @see #registerRuntimePlatform(RuntimePlatform)
     * @see #unregisterRuntimePlatform(RuntimePlatform)
     */
    private Map<String, RuntimePlatform> platformMap;

    /**
     * Constructs a new instance of the registry and initializes its {@link #platformMap}.
     */
    public RuntimePlatformRegistry() {
        this.platformMap = new HashMap<>();
    }

    /**
     * Registers the provided {@code platform} using its {@code name}.
     *
     * @param platform the {@link RuntimePlatform} to register
     */
    public void registerRuntimePlatform(RuntimePlatform platform) {
        this.platformMap.put(platform.getName(), platform);
    }

    /**
     * Unregisters the provided {@code platform}.
     *
     * @param platform the {@link RuntimePlatform} to unregister
     */
    public void unregisterRuntimePlatform(RuntimePlatform platform) {
        RuntimePlatform runtimePlatform = this.platformMap.remove(platform.getName());
        runtimePlatform.disableAllActions();
    }

    /**
     * Unregisters all the {@link RuntimePlatform}s from this registry.
     */
    public void clearRuntimePlatforms() {
        for (RuntimePlatform platform : this.platformMap.values()) {
            platform.disableAllActions();
        }
        this.platformMap.clear();
    }

    /**
     * Returns the {@link RuntimePlatform} associated to the provided {@code platformName}.
     * <p>
     * {@link RuntimePlatform} are registered using the {@link RuntimePlatform#getName()} method.
     *
     * @param platformName the name of the {@link RuntimePlatform} to retrieve
     * @return the {@link RuntimePlatform} associated to the provided {@code platformName}
     */
    public RuntimePlatform getRuntimePlatform(String platformName) {
        return this.platformMap.get(platformName);
    }

    /**
     * Returns the {@link RuntimePlatform} associated to the provided {@code platformDefinition}.
     * <p>
     * The provided {@link PlatformDefinition} should follow jarvis' naming conventions, and provide a
     * {@link PlatformDefinition#getName()}
     * method that returns the name of the concrete {@link RuntimePlatform} class to retrieve.
     *
     * @param platformDefinition the {@link PlatformDefinition} representing the {@link RuntimePlatform} to retrieve
     * @return the {@link RuntimePlatform} associated to the provided {@code platformDefinition}
     */
    public RuntimePlatform getRuntimePlatform(PlatformDefinition platformDefinition) {
        return this.platformMap.get(platformDefinition.getName());
    }

    /**
     * Returns all the {@link RuntimePlatform}s stored in this registry.
     *
     * @return all the {@link RuntimePlatform}s stored in this registry
     */
    public Collection<RuntimePlatform> getRuntimePlatforms() {
        return Collections.unmodifiableCollection(this.platformMap.values());
    }
}
