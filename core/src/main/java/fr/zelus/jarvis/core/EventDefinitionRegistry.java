package fr.zelus.jarvis.core;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.intent.EventDefinition;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A registry that stores {@link EventDefinition}s.
 */
public class EventDefinitionRegistry {

    /**
     * The {@link Map} used to store the {@link EventDefinition}s.
     *
     * @see #registerEventDefinition(EventDefinition)
     * @see #clearEventDefinitions()
     */
    private Map<String, EventDefinition> eventDefinitionMap;

    /**
     * Constructs a new instance of the registry and initializes its {@link #eventDefinitionMap}.
     */
    public EventDefinitionRegistry() {
        this.eventDefinitionMap = new HashMap<>();
    }

    /**
     * Registers the provided {@code eventDefinition} using its {@code name}.
     *
     * @param eventDefinition the {@link EventDefinition} to register
     */
    public void registerEventDefinition(EventDefinition eventDefinition) {
        String adaptedName = adaptEventName(eventDefinition.getName());
        if (this.eventDefinitionMap.containsKey(adaptedName)) {
            Log.warn("Another EventDefinition is stored with the key {0}, overriding it", adaptedName);
        }
        this.eventDefinitionMap.put(adaptedName, eventDefinition);
    }

    /**
     * Returns the {@link EventDefinition} matching the provided {@code name}.
     *
     * @param name the name of the {@link EventDefinition} to retrieve
     * @return the {@link EventDefinition} matching the provided {@code name}
     * @see #getAllEventDefinitions()
     */
    public EventDefinition getEventDefinition(String name) {
        return this.eventDefinitionMap.get(adaptEventName(name));
    }

    /**
     * Returns an unmodifiable {@link Collection} containing all the registered {@link EventDefinition}s.
     * <p>
     * To retrieve a single {@link EventDefinition} from its {@code name} see {@link #getEventDefinition(String)}.
     *
     * @return an unmodifiable {@link Collection} containing all the registered {@link EventDefinition}s
     */
    public Collection<EventDefinition> getAllEventDefinitions() {
        return Collections.unmodifiableCollection(this.eventDefinitionMap.values());
    }

    /**
     * Unregisters all the {@link EventDefinition}s from this registry.
     */
    public void clearEventDefinitions() {
        this.eventDefinitionMap.clear();
    }

    /**
     * Adapts the provided {@code eventName} by replacing its spaces by {@code _}.
     *
     * @param eventName the event name to adapt
     * @return the adapted name
     */
    private String adaptEventName(String eventName) {
        return eventName.replaceAll(" ", "_");
    }
}
