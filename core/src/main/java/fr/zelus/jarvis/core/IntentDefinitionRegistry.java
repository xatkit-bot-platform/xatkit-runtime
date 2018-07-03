package fr.zelus.jarvis.core;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.intent.IntentDefinition;

import java.util.Collection;
import java.util.Collections;
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
        String adaptedName = adaptIntentName(intentDefinition.getName());
        if (this.intentDefinitionMap.containsKey(adaptedName)) {
            Log.warn("Another IntentDefinition is stored with the key {0}, overriding it", adaptedName);
        }
        this.intentDefinitionMap.put(adaptedName, intentDefinition);
    }

    /**
     * Returns the {@link IntentDefinition} matching the provided {@code name}.
     *
     * @param name the name of the {@link IntentDefinition} to retrieve
     * @return the {@link IntentDefinition} matching the provided {@code name}
     * @see #getAllIntentDefinitions()
     */
    public IntentDefinition getIntentDefinition(String name) {
        return this.intentDefinitionMap.get(adaptIntentName(name));
    }

    /**
     * Returns an unmodifiable {@link Collection} containing all the registered {@link IntentDefinition}s.
     * <p>
     * To retrieve a single {@link IntentDefinition} from its {@code name} see {@link #getIntentDefinition(String)}.
     *
     * @return an unmodifiable {@link Collection} containing all the registered {@link IntentDefinition}s
     */
    public Collection<IntentDefinition> getAllIntentDefinitions() {
        return Collections.unmodifiableCollection(this.intentDefinitionMap.values());
    }

    /**
     * Unregisters all the {@link IntentDefinition}s from this registry.
     */
    public void clearIntentDefinitions() {
        this.intentDefinitionMap.clear();
    }

    /**
     * Adapts the provided {@code intentName} by replacing its spaces by {@code _}.
     *
     * @param intentName the intent name to adapt
     * @return the adapted name
     */
    private String adaptIntentName(String intentName) {
        return intentName.replaceAll(" ", "_");
    }
}
