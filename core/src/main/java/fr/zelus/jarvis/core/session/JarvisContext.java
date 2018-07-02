package fr.zelus.jarvis.core.session;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.nonNull;

public class JarvisContext {

    private Map<String, Map<String, Object>> contexts;

    public JarvisContext() {
        this.contexts = new HashMap<>();
    }

    public void setContextValue(String context, String key, Object value) {
        if(contexts.containsKey(context)) {
            Map<String, Object> contextValues = contexts.get(context);
            contextValues.put(key, value);
        } else {
            Map<String, Object> contextValues = new HashMap<>();
            contextValues.put(key, value);
            contexts.put(context, contextValues);
        }
    }

    public Map<String, Object> getContextVariables(String context) {
        if(contexts.containsKey(context)) {
            return Collections.unmodifiableMap(contexts.get(context));
        } else {
            return Collections.emptyMap();
        }
    }

    public Object getContextValue(String context, String key) {
        Map<String, Object> contextVariables = getContextVariables(context);
        if(nonNull(contextVariables)) {
            return contextVariables.get(key);
        } else {
            return null;
        }
    }
}
