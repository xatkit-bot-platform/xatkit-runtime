package fr.zelus.jarvis.core.session;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisException;
import fr.zelus.jarvis.io.EventProvider;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;

/**
 * A variable container bound to a {@link JarvisSession}.
 * <p>
 * This class stores the different variables that can be set during user input processing and accessed by executed
 * {@link fr.zelus.jarvis.core.JarvisAction}. {@link JarvisContext} is used to store:
 * <ul>
 * <li><b>{@link EventProvider} values</b> such as the user name, the channel where the
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
     * The sub-contexts associated to this class.
     * <p>
     * Sub-contexts are used to characterize the variables stored in the global context. As an example, a sub-context
     * <i>slack</i> hold all the variables related to Slack.
     */
    private Map<String, Map<String, Object>> contexts;

    /**
     * Constructs a new empty {@link JarvisContext}.
     */
    public JarvisContext() {
        this.contexts = new HashMap<>();
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
     * @throws NullPointerException if the provided {@code context} or {@code key} is {@code null}
     * @see #getContextVariables(String)
     * @see #getContextValue(String, String)
     */
    public void setContextValue(String context, String key, Object value) {
        checkNotNull(context, "Cannot set the value to the context null");
        checkNotNull(key, "Cannot set the value to the context %s with the key null", context);
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
     * @throws NullPointerException if the provided {@code context} is {@code null}
     * @see #getContextValue(String, String)
     */
    public Map<String, Object> getContextVariables(String context) {
        checkNotNull(context, "Cannot retrieve the context variables from the null context");
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
     * @throws NullPointerException if the provided {@code context} or {@code key} is {@code null}
     * @see #getContextVariables(String)
     */
    public Object getContextValue(String context, String key) {
        checkNotNull(context, "Cannot find the context value from the null context");
        checkNotNull(key, "Cannot find the value of the context %s with the key null", context);
        Map<String, Object> contextVariables = getContextVariables(context);
        if (nonNull(contextVariables)) {
            return contextVariables.get(key);
        } else {
            return null;
        }
    }

    /**
     * Returns an unmodifiable {@link Map} representing the stored context values.
     * <p>
     * <b>Note:</b> this method is protected for testing purposes, and should not be called by client code.
     *
     * @return an unmodifiable {@link Map} representing the stored context values
     */
    protected Map<String, Map<String, Object>> getContextMap() {
        return Collections.unmodifiableMap(contexts);
    }

    /**
     * Replace declared variables from {@code message} by their context values.
     * <p>
     * This method searches for variable patterns in the provided {@code message} and retrieves the corresponding
     * values from the context. Variables accessing a context value should be declared following this template:
     * {@code {$contextName.variableName}}.
     * <p>
     * If a variable cannot be replaced the variable pattern is left unchanged.
     *
     * @param message the message to replace the variables from
     * @return the provided {@code message} with its declared variables replaced by their context values.
     * @throws JarvisException if an error occurred when retrieving a value from a previous action
     */
    public String fillContextValues(String message) {
        checkNotNull(message, "Cannot fill the context values of the null message");
        Log.info("Processing message {0}", message);
        String outMessage = message;
        Matcher m = Pattern.compile("\\{\\$\\S+\\}").matcher(message);
        while (m.find()) {
            String group = m.group();
            Log.info("Found context variable {0}", group);
            /*
             * Cannot be empty.
             */
            String filteredGroup = group.substring(2);
            String[] splitGroup = filteredGroup.split("\\.");
            if (splitGroup.length == 2) {
                Log.info("Looking for context \"{0}\"", splitGroup[0]);
                Map<String, Object> variables = this.getContextVariables(splitGroup[0]);
                if (nonNull(variables)) {
                    String variableIdentifier = splitGroup[1].substring(0, splitGroup[1].length() - 1);
                    Object value = variables.get(variableIdentifier);
                    Log.info("Looking for variable \"{0}\"", variableIdentifier);
                    if (nonNull(value)) {
                        String printedValue = null;
                        if (value instanceof Future) {
                            try {
                                printedValue = ((Future) value).get().toString();
                                Log.info("found value {0} for {1}.{2}", printedValue, splitGroup[0],
                                        variableIdentifier);
                            } catch (InterruptedException | ExecutionException e) {
                                String errorMessage = MessageFormat.format("An error occured when retrieving the " +
                                        "value of the variable {0}", variableIdentifier);
                                Log.error(errorMessage);
                                throw new JarvisException(e);
                            }
                        } else {
                            printedValue = value.toString();
                            Log.info("found value {0} for {1}.{2}", printedValue, splitGroup[0],
                                    variableIdentifier);
                        }
                        outMessage = outMessage.replace(group, printedValue);
                    } else {
                        Log.error("The context variable {0} is null", group);
                    }
                } else {
                    Log.error("The context variable {0} does not exist", group);
                }
            } else {
                Log.error("Invalid context variable access: {0}", group);
            }
        }
        return outMessage;
    }
}
