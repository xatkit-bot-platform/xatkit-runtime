package com.xatkit.core;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.platform.PlatformDefinition;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.isNull;

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
    private Map<String, RuntimePlatform> platformToRuntime;

    /**
     * The {@link Map} used to store the loaded {@link PlatformDefinition}s.
     */
    private Map<String, PlatformDefinition> loadedPlatforms;

    /**
     * Constructs a new instance of the registry and initializes its {@link #platformToRuntime}.
     */
    public RuntimePlatformRegistry() {
        this.platformToRuntime = new HashMap<>();
        this.loadedPlatforms = new HashMap<>();
    }

    /**
     * Registeres the provided {@code platformDefinition} using its {@code name}.
     * <p>
     * Registered {@link PlatformDefinition}s can be accessed using {@link #getLoadedPlatformDefinitions()}. Note that a
     * registered {@link PlatformDefinition} does not imply that the corresponding {@link RuntimePlatform} has been
     * loaded.
     *
     * @param platformDefinition the {@link PlatformDefinition} to register
     * @see #getLoadedPlatformDefinitions()
     */
    public void registerLoadedPlatformDefinition(PlatformDefinition platformDefinition) {
        this.loadedPlatforms.put(platformDefinition.getName(), platformDefinition);
    }

    /**
     * Returns a {@link Collection} containing all the loaded {@link PlatformDefinition}s.
     *
     * @return a {@link Collection} containing all the loaded {@link PlatformDefinition}s
     */
    public Collection<PlatformDefinition> getLoadedPlatformDefinitions() {
        return this.loadedPlatforms.values();
    }


    /**
     * Registers the provided {@code platform} using its {@code name}.
     *
     * @param platform the {@link RuntimePlatform} to register
     * @see #registerRuntimePlatform(String, RuntimePlatform)
     */
    public void registerRuntimePlatform(RuntimePlatform platform) {
        this.registerRuntimePlatform(platform.getName(), platform);
    }

    /**
     * Registers the provided {@code platform} with the provided {@code name}.
     * <p>
     * This method is used to bind abstract platforms to their concrete implementation. In this case the provided
     * {@code platformName} is typically the name of the abstract platform, and the concrete {@link RuntimePlatform}
     * is the implementation loaded from the Xatkit configuration.
     *
     * @param platformName the name to use to register the {@link RuntimePlatform}
     * @param platform     the {@link RuntimePlatform} to register
     */
    public void registerRuntimePlatform(String platformName, RuntimePlatform platform) {
        this.platformToRuntime.put(platformName, platform);
    }

    /**
     * Unregisters the provided {@code platform}.
     *
     * @param platform the {@link RuntimePlatform} to unregister
     */
    public void unregisterRuntimePlatform(RuntimePlatform platform) {
        RuntimePlatform runtimePlatform = this.platformToRuntime.remove(platform.getName());
        if (isNull(runtimePlatform)) {
            /*
             * The platform may have been register with a different name, remove all the entries that have it as
             * their value.
             */
            this.platformToRuntime.entrySet().removeIf(entry -> entry.getValue().equals(platform));
        }
        runtimePlatform.disableAllActions();
    }

    /**
     * Unregisters all the {@link RuntimePlatform}s from this registry.
     */
    public void clearRuntimePlatforms() {
        for (RuntimePlatform platform : this.platformToRuntime.values()) {
            platform.disableAllActions();
        }
        this.platformToRuntime.clear();
    }

    public PlatformDefinition getPlatformDefinition(String platformName) {
        return this.loadedPlatforms.get(platformName);
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
        return this.platformToRuntime.get(platformName);
    }

    /**
     * Returns the {@link RuntimePlatform} associated to the provided {@code platformDefinition}.
     * <p>
     * The provided {@link PlatformDefinition} should follow xatkit's naming conventions, and provide a
     * {@link PlatformDefinition#getName()}
     * method that returns the name of the concrete {@link RuntimePlatform} class to retrieve.
     *
     * @param platformDefinition the {@link PlatformDefinition} representing the {@link RuntimePlatform} to retrieve
     * @return the {@link RuntimePlatform} associated to the provided {@code platformDefinition}
     */
    public RuntimePlatform getRuntimePlatform(PlatformDefinition platformDefinition) {
        return this.platformToRuntime.get(platformDefinition.getName());
    }

    /**
     * Returns all the {@link RuntimePlatform}s stored in this registry.
     *
     * @return all the {@link RuntimePlatform}s stored in this registry
     */
    public Collection<RuntimePlatform> getRuntimePlatforms() {
        return Collections.unmodifiableCollection(this.platformToRuntime.values());
    }
}
