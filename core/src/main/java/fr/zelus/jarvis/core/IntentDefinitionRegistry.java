package fr.zelus.jarvis.core;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.intent.IntentDefinition;

import java.util.HashMap;
import java.util.Map;

public class IntentDefinitionRegistry {

    private Map<String, IntentDefinition> intentDefinitionMap;

    public IntentDefinitionRegistry() {
        this.intentDefinitionMap = new HashMap<>();
    }

    public void registerIntentDefinition(IntentDefinition intentDefinition) {
        if(this.intentDefinitionMap.containsKey(intentDefinition.getName())) {
            Log.warn("Another IntentDefinition is stored with the key {0}, overriding it", intentDefinition.getName());
        }
        this.intentDefinitionMap.put(intentDefinition.getName(), intentDefinition);
    }

    public IntentDefinition getIntentDefinition(String name) {
        return this.intentDefinitionMap.get(name);
    }

    public void clearIntentDefinitions() {
        this.intentDefinitionMap.clear();
    }
}
