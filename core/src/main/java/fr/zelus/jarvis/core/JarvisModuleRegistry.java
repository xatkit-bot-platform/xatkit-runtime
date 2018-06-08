package fr.zelus.jarvis.core;

import fr.zelus.jarvis.module.Module;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JarvisModuleRegistry {

    private Map<String, JarvisModule> moduleMap;

    public JarvisModuleRegistry() {
        this.moduleMap = new HashMap<>();
    }

    public void registerJarvisModule(JarvisModule module) {
        this.moduleMap.put(module.getName(), module);
    }

    public void unregisterJarvisModule(JarvisModule module) {
        JarvisModule jarvisModule = this.moduleMap.remove(module.getName());
        jarvisModule.disableAllActions();
    }

    public void clearJarvisModules() {
        for(JarvisModule module : this.moduleMap.values()) {
            module.disableAllActions();
        }
        this.moduleMap.clear();
    }

    public JarvisModule getJarvisModule(String moduleName) {
        return this.moduleMap.get(moduleName);
    }

    public JarvisModule getJarvisModule(Module moduleDefiniton) {
        return this.moduleMap.get(moduleDefiniton.getName());
    }

    public Collection<JarvisModule> getModules() {
        return Collections.unmodifiableCollection(this.moduleMap.values());
    }
}
