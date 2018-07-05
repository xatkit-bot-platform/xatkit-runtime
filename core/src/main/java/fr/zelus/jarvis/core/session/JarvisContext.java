package fr.zelus.jarvis.core.session;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.nonNull;

/**
 * A variable container bound to a {@link JarvisSession}.
 * <p>
 * This class stores the different variables that can be set during user input processing and accessed by executed
 * {@link fr.zelus.jarvis.core.JarvisAction}. {@link JarvisContext} is used to store:
 * <ul>
 * <li><b>{@link fr.zelus.jarvis.io.InputProvider} values</b> such as the user name, the channel where the
 * message was received, etc</li>
 * <li><b>Intent recognition values</b>, that are computed by the Intent recognition engine and used to pass
 * information between messages</li>
 * <li><b>Action values</b>, that are returned by {@link fr.zelus.jarvis.core.JarvisAction}s</li>
 * </ul>
 * <p>
 * This class is heavily used by jarvis core component to pass {@link fr.zelus.jarvis.core.JarvisAction} parameters,
 * and replace output message variables by their concrete values.
 */
public class JarvisContext {

    /**
     * The {@link JarvisSession} containing this context.
     */
    private JarvisSession session;

    /**
     * The sub-contexts associated to this class.
     * <p>
     * Sub-contexts are used to characterize the variables stored in the global context. As an example, a sub-context
     * <i>slack</i> hold all the variables related to Slack.
     */
    private Map<String, Map<String, Object>> contexts;

    /**
     * Constructs a new empty {@link JarvisContext}.
     *
     * @param session the {@link JarvisSession} containing this context
     */
    public JarvisContext(JarvisSession session) {
        /*
         * Passing the JarvisSession to the JarvisContext is a quick fix for #48, we should pass JarvisSession to
         * actions instead of the context.
         */
        this.session = session;
        this.contexts = new HashMap<>();
    }

    /**
     * Returns the {@link JarvisSession} containing this context.
     *
     * @return the {@link JarvisSession} containing this context
     */
    public JarvisSession getSession() {
        return session;
    }

    /**
     * Stores the provided {@code value} in the given {@code context} with the provided {@code key}.
     * <p>
     * As an example, calling setContextValue("slack", "username", "myUsername") sets the variable
     * <i>username</i> with the value <i>myUsername</i> in the <i>slack</i> context.
     * <p>
     * To retrieve all the variables of a given sub-context see {@link #getContextVariables(String)}.
     *
     * @param context the sub-context to store the value in
     * @param key     the sub-context key associated to the value
     * @param value   the value to store
     * @see #getContextVariables(String)
     * @see #getContextValue(String, String)
     */
    public void setContextValue(String context, String key, Object value) {
        if (contexts.containsKey(context)) {
            Map<String, Object> contextValues = contexts.get(context);
            contextValues.put(key, value);
        } else {
            Map<String, Object> contextValues = new HashMap<>();
            contextValues.put(key, value);
            contexts.put(context, contextValues);
        }
    }

    /**
     * Returns all the variables stored in the given {@code context}.
     * <p>
     * This method returns an unmodifiable {@link Map} holding the sub-context variables. To retrieve a specific
     * variable from {@link JarvisContext} see {@link #getContextValue(String, String)}.
     *
     * @param context the sub-context to retrieve the variables from
     * @return an unmodifiable {@link Map} holding the sub-context variables
     * @see #getContextValue(String, String)
     */
    public Map<String, Object> getContextVariables(String context) {
        if (contexts.containsKey(context)) {
            return Collections.unmodifiableMap(contexts.get(context));
        } else {
            return Collections.emptyMap();
        }
    }

    /**
     * Returns the {@code context} value associated to the provided {@code key}.
     * <p>
     * As an example, calling getContextValue("slack", "username") returns the value of the <i>username</i> variable
     * stored in the <i>slack</i> sub-context.
     * <p>
     * To retrieve all the variables of a given sub-context see {@link #getContextVariables(String)}.
     *
     * @param context the sub-context to retrieve the variable from
     * @param key     the sub-context key associated to the value
     * @return the {@code context} value associated to the provided {@code key}, or {@code null} if the {@code key}
     * does not exist
     * @see #getContextVariables(String)
     */
    public Object getContextValue(String context, String key) {
        Map<String, Object> contextVariables = getContextVariables(context);
        if (nonNull(contextVariables)) {
            return contextVariables.get(key);
        } else {
            return null;
        }
    }
}
