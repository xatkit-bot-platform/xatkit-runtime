package com.xatkit.core;

import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.IntentDefinition;
import fr.inria.atlanmod.commons.log.Log;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A registry that stores {@link EventDefinition}s and provide utility methods to retrieve them.
 * <p>
 * This class provides methods to retrieve specific {@link EventDefinition}s, and filter {@link IntentDefinition}s.
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
     * Registers the provided {@code eventDefinition}.
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
     * Unregisters the provided {@code eventDefinition}.
     *
     * @param eventDefinition the {@link EventDefinition} to unregister
     */
    public void unregisterEventDefinition(EventDefinition eventDefinition) {
        String adaptedName = adaptEventName(eventDefinition.getName());
        this.eventDefinitionMap.remove(adaptedName);
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
     * Returns the {@link IntentDefinition} matching the provided {@code name}.
     *
     * @param name the name of the {@link IntentDefinition} to retrieve
     * @return the {@link IntentDefinition} matching the provided {@code name}
     * @see #getAllIntentDefinitions()
     */
    public IntentDefinition getIntentDefinition(String name) {
        EventDefinition eventDefinition = this.eventDefinitionMap.get(adaptEventName(name));
        if (eventDefinition instanceof IntentDefinition) {
            return (IntentDefinition) eventDefinition;
        } else {
            return null;
        }
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
     * Returns an unmodifiable {@link Collection} containing all the registered {@link IntentDefinition}s.
     * <p>
     * This method returns a subset of the {@link Collection} returned by {@link #getAllEventDefinitions()} that
     * contains only {@link IntentDefinition} instances.
     * <p>
     * To retrieve a single {@link IntentDefinition} from its {@code name} see {@link #getIntentDefinition(String)}.
     *
     * @return an unmodifiable {@link Collection} containing all the registered {@link IntentDefinition}s
     */
    public Collection<IntentDefinition> getAllIntentDefinitions() {
        List<IntentDefinition> intentDefinitions = this.eventDefinitionMap.values().stream().filter(e -> e instanceof
                IntentDefinition).map(i -> (IntentDefinition) i).collect(Collectors.toList());
        return Collections.unmodifiableCollection(intentDefinitions);
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
