package fr.zelus.jarvis.core;

import fr.zelus.jarvis.platform.Platform;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A registry that stores {@link JarvisModule}s.
 */
public class JarvisModuleRegistry {

    /**
     * The {@link Map} used to store the {@link JarvisModule}s.
     *
     * @see #registerJarvisModule(JarvisModule)
     * @see #unregisterJarvisModule(JarvisModule)
     */
    private Map<String, JarvisModule> moduleMap;

    /**
     * Constructs a new instance of the registry and initializes its {@link #moduleMap}.
     */
    public JarvisModuleRegistry() {
        this.moduleMap = new HashMap<>();
    }

    /**
     * Registers the provided {@code module} using its {@code name}.
     *
     * @param module the {@link JarvisModule} to register
     */
    public void registerJarvisModule(JarvisModule module) {
        this.moduleMap.put(module.getName(), module);
    }

    /**
     * Unregisters the provided {@code module}.
     *
     * @param module the {@link JarvisModule} to unregister
     */
    public void unregisterJarvisModule(JarvisModule module) {
        JarvisModule jarvisModule = this.moduleMap.remove(module.getName());
        jarvisModule.disableAllActions();
    }

    /**
     * Unregisters all the {@link JarvisModule}s from this registry.
     */
    public void clearJarvisModules() {
        for (JarvisModule module : this.moduleMap.values()) {
            module.disableAllActions();
        }
        this.moduleMap.clear();
    }

    /**
     * Returns the {@link JarvisModule} associated to the provided {@code moduleName}.
     * <p>
     * {@link JarvisModule} are registered using the {@link JarvisModule#getName()} method.
     *
     * @param moduleName the name of the {@link JarvisModule} to retrieve
     * @return the {@link JarvisModule} associated to the provided {@code moduleName}
     */
    public JarvisModule getJarvisModule(String moduleName) {
        return this.moduleMap.get(moduleName);
    }

    /**
     * Returns the {@link JarvisModule} associated to the provided {@code moduleDefinition}.
     * <p>
     * The provided {@link Platform} should follow jarvis' naming conventions, and provide a {@link Platform#getName()}
     * method that returns the name of the concrete {@link JarvisModule} class to retrieve.
     *
     * @param platformDefinition the {@link Platform} representing the {@link JarvisModule} to retrieve
     * @return the {@link JarvisModule} associated to the provided {@code moduleDefinition}
     */
    public JarvisModule getJarvisModule(Platform platformDefinition) {
        return this.moduleMap.get(platformDefinition.getName());
    }

    /**
     * Returns all the {@link JarvisModule}s stored in this registry.
     *
     * @return all the {@link JarvisModule}s stored in this registry
     */
    public Collection<JarvisModule> getModules() {
        return Collections.unmodifiableCollection(this.moduleMap.values());
    }
}
