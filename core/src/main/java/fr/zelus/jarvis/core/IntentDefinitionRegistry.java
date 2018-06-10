package fr.zelus.jarvis.core;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.intent.IntentDefinition;

import java.util.HashMap;
import java.util.Map;

/**
 * A registry that stores {@link IntentDefinition}s.
 */
public class IntentDefinitionRegistry {

    /**
     * The {@link Map} used to store the {@link IntentDefinition}s.
     *
     * @see #registerIntentDefinition(IntentDefinition)
     * @see #clearIntentDefinitions()
     */
    private Map<String, IntentDefinition> intentDefinitionMap;

    /**
     * Constructs a new instance of the registry and initializes its {@link #intentDefinitionMap}.
     */
    public IntentDefinitionRegistry() {
        this.intentDefinitionMap = new HashMap<>();
    }

    /**
     * Registers the provided {@code intentDefinition} using its {@code name}.
     *
     * @param intentDefinition the {@link IntentDefinition} to register
     */
    public void registerIntentDefinition(IntentDefinition intentDefinition) {
        if (this.intentDefinitionMap.containsKey(intentDefinition.getName())) {
            Log.warn("Another IntentDefinition is stored with the key {0}, overriding it", intentDefinition.getName());
        }
        this.intentDefinitionMap.put(intentDefinition.getName(), intentDefinition);
    }

    /**
     * Returns the {@link IntentDefinition} matching the provided {@code name}.
     *
     * @param name the name of the {@link IntentDefinition} to retrieve
     * @return the {@link IntentDefinition} matching the provided {@code name}
     */
    public IntentDefinition getIntentDefinition(String name) {
        return this.intentDefinitionMap.get(name);
    }

    /**
     * Unregisters all the {@link IntentDefinition}s from this registry.
     */
    public void clearIntentDefinitions() {
        this.intentDefinitionMap.clear();
    }
}
