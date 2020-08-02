package com.xatkit.core.session;

import com.xatkit.core.XatkitException;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.platform.io.RuntimeEventProvider;
import com.xatkit.intent.Context;
import com.xatkit.intent.ContextInstance;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.ContextParameterValue;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;

import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

/**
 * A variable container bound to a {@link XatkitSession}.
 * <p>
 * This class stores the different variables that can be set during user input processing and accessed by executed
 * {@link RuntimeAction}. {@link RuntimeContexts} is used to store:
 * <ul>
 * <li><b>{@link RuntimeEventProvider} values</b> such as the user name, the channel where the
 * message was received, etc</li>
 * <li><b>Intent recognition values</b>, that are computed by the Intent recognition engine and used to pass
 * information between messages</li>
 * <li><b>Action values</b>, that are returned by {@link RuntimeAction}s</li>
 * </ul>
 * <p>
 * This class is heavily used by xatkit core component to pass {@link RuntimeAction} parameters,
 * and replace output message variables by their concrete values.
 */
public class RuntimeContexts {

    /**
     * The {@link Configuration} key to store maximum time to spend waiting for a context variable (in seconds).
     */
    public static String VARIABLE_TIMEOUT_KEY = "xatkit.context.variable.timeout";

    /**
     * The default amount of time to spend waiting for a context variable (in seconds).
     */
    public static int DEFAULT_VARIABLE_TIMEOUT_VALUE = 2;

    /**
     * The sub-contexts associated to this class.
     * <p>
     * Sub-contexts are used to characterize the variables stored in the global context. As an example, a sub-context
     * <i>slack</i> hold all the variables related to Slack.
     */
    private Map<String, Map<String, Object>> contexts;

    /**
     * The internal {@link Map} storing context lifespan counts.
     * <p>
     * This map is used to keep track of the number of user interactions that can be handled before removing each
     * context value stored in the current {@link RuntimeContexts}. Lifespan counters are decremented by calling
     * {@link #decrementLifespanCounts()}, that takes care of removing context when there lifespan counter reaches
     * {@code 0}.
     *
     * @see #decrementLifespanCounts()
     */
    private Map<String, Integer> lifespanCounts;

    /**
     * The amount of time to spend waiting for a context variable (in seconds).
     * <p>
     * This attribute is equals to {@link #DEFAULT_VARIABLE_TIMEOUT_VALUE} unless a specific value is provided in this
     * class' {@link Configuration} constructor parameter
     *
     * @see #RuntimeContexts(Configuration)
     */
    private int variableTimeout;

    /**
     * Constructs a new empty {@link RuntimeContexts}.
     * <p>
     * See {@link #RuntimeContexts(Configuration)} to construct a {@link RuntimeContexts} with a given
     * {@link Configuration}.
     *
     * @see #RuntimeContexts(Configuration)
     */
    public RuntimeContexts() {
        this(new BaseConfiguration());
    }

    /**
     * Constructs a new empty {@link RuntimeContexts} with the given {@code configuration}.
     * <p>
     * The provided {@link Configuration} contains information to customize the {@link RuntimeContexts} behavior, such
     * as the {@code timeout} to retrieve {@link Future} variables.
     *
     * @param configuration the {@link Configuration} parameterizing the {@link RuntimeContexts}
     * @throws NullPointerException if the provided {@code configuration} is {@code null}
     */
    public RuntimeContexts(@NonNull Configuration configuration) {
        /*
         * Use ConcurrentHashMaps: the RuntimeContexts may be accessed and modified by multiple threads, in case of
         * multiple input messages or parallel RuntimeAction execution.
         */
        this.contexts = new ConcurrentHashMap<>();
        this.lifespanCounts = new ConcurrentHashMap<>();
        if (configuration.containsKey(VARIABLE_TIMEOUT_KEY)) {
            this.variableTimeout = configuration.getInt(VARIABLE_TIMEOUT_KEY);
            Log.debug("Setting context variable timeout to {0}s", variableTimeout);
        } else {
            this.variableTimeout = DEFAULT_VARIABLE_TIMEOUT_VALUE;
            Log.debug("Using default context variable timeout ({0}s)", DEFAULT_VARIABLE_TIMEOUT_VALUE);
        }
    }

    /**
     * Returns the amount of time the {@link RuntimeContexts} can spend waiting for a context variable (in seconds).
     * <p>
     * This value can be set in this class' {@link Configuration} constructor parameter.
     *
     * @return the amount of time the {@link RuntimeContexts} can spend waiting for a context variable (in seconds)
     */
    public int getVariableTimeout() {
        return variableTimeout;
    }

    /**
     * Sets the context associated to the provided {@code contextInstance}.
     * <p>
     * This method retrieves the name of the context to set from the provided {@link ContextInstance} by navigating
     * its {@code definition} reference. Note that passing a {@code contextInstance} without definition to this
     * method will throw an {@link IllegalArgumentException}.
     * <p>
     * This method is used as syntactic sugar to register {@link ContextInstance}s, see
     * {@link #setContext(String, int)} to register a context from {@link String} values.
     *
     * @param contextInstance the {@link ContextInstance} to set
     * @return the {@link Map} containing the variables of the set context
     * @throws NullPointerException     if the provided {@code contextInstance} is {@code null}
     * @throws IllegalArgumentException if the provided {@code contextInstance}'s {@code definition} is not set
     */
    public Map<String, Object> setContext(@NonNull ContextInstance contextInstance) {
        checkArgument(nonNull(contextInstance.getDefinition()), "Cannot set the context from the provided %s %s, the " +
                        "provided %s does not have a definition", ContextInstance.class.getSimpleName(),
                contextInstance, ContextInstance.class.getSimpleName());
        String contextName = contextInstance.getDefinition().getName();
        int lifespanCount = contextInstance.getLifespanCount();
        return this.setContext(contextName, lifespanCount);
    }

    /**
     * Sets the provided {@code context} with the given {@code lifespanCount}.
     * <p>
     * <b>Note:</b> the context lifespan count is only updated if {@code lifespanCount > #getContextLifespanCount
     * (context)}. Context lifespan counts can only be decremented by calling {@link #decrementLifespanCounts()}.
     * This architecture ensures the global lifespan count consistency among context variables.
     * <p>
     * As an example, calling setContextValue("slack", 5, "username", "myUsername") sets the variable
     * <i>username</i> with the value <i>myUsername</i> in the <i>slack</i> context, and sets its lifespan
     * count to {@code 5}.
     * <p>
     * To retrieve all the variables of a given sub-context see {@link #getContextVariables(String)}.
     *
     * @param context       the name of the context to set
     * @param lifespanCount the lifespan count of the context to set
     * @return the {@link Map} containing the variables of the set context
     * @throws NullPointerException     if the provided {@code context} is {@code null}
     * @throws IllegalArgumentException if the provided {@code lifespanCount <= 0}
     * @see #setContext(ContextInstance)
     * @see #setContextValue(ContextParameterValue)
     * @see #setContextValue(String, int, String, Object)
     * @see #decrementLifespanCounts()
     */
    public Map<String, Object> setContext(@NonNull String context, int lifespanCount) {
        checkArgument(lifespanCount > 0, "Cannot set the context lifespan count to %s, the lifespan count should be " +
                "strictly greater than 0", lifespanCount);
        Map<String, Object> contextMap;
        if (contexts.containsKey(context)) {
            contextMap = contexts.get(context);
        } else {
            contextMap = new HashMap<>();
            this.contexts.put(context, contextMap);
        }
        if (lifespanCounts.containsKey(context)) {
            int currentLifespan = lifespanCounts.get(context);
            if (currentLifespan <= lifespanCount) {
                /*
                 * The provided lifespanCount is greater than the stored one, this means that we are dealing with a
                 * new context (i.e. a new Intent recognized from the IntentRecognitionProvider or a new Event
                 * received by an RuntimeEventProvider). Override the current value to keep the variable alive.
                 */
                Log.debug("Overriding context {0} lifespanCount (previous: {1}, new: {2})", context, currentLifespan,
                        lifespanCount);
                lifespanCounts.put(context, lifespanCount);
            } else {
                /*
                 * We should not support lifespan count decrement when setting context values. Lifespan counts are
                 * globally handled by the decrementLifespanCounts() method.
                 */
                Log.warn("Ignoring the provided lifespan for context {0} ({1}): the lifespan is lower than the stored" +
                        " one ({2}). Context lifespan counts can only be decremented through RuntimeContexts" +
                        ".decrementLifespanCounts()", context, lifespanCount, currentLifespan);
            }
        } else {
            Log.debug("Setting context {0} lifespanCount to {1}", context, lifespanCount);
            lifespanCounts.put(context, lifespanCount);
        }
        return contextMap;
    }

    /**
     * Stores the provided {@code value} in the given {@code context} with the provided {@code key} and {@code
     * lifespanCount}.
     * <p>
     * <b>Note:</b> the context lifespan count is only updated if {@code lifespanCount > #getContextLifespanCount
     * (context)}. Context lifespan counts can only be decremented by calling {@link #decrementLifespanCounts()}.
     * This architecture ensures the global lifespan count consistency among context variables.
     * <p>
     * As an example, calling setContextValue("slack", 5, "username", "myUsername") sets the variable
     * <i>username</i> with the value <i>myUsername</i> in the <i>slack</i> context, and sets its lifespan count to
     * {@code 5}.
     * <p>
     * To retrieve all the variables of a given sub-context see {@link #getContextVariables(String)}.
     *
     * @param context       the name of the context to set
     * @param lifespanCount the lifespan count of the context to set
     * @param key           the sub-context key associated to the value
     * @param value         the value to store
     * @throws NullPointerException     if the provided {@code context} or {@code key} is {@code null}
     * @throws IllegalArgumentException if the provided {@code lifespanCount <= 0}
     * @see #getContextVariables(String)
     * @see #getContextValue(String, String)
     * @see #getContextLifespanCount(String)
     * @see #decrementLifespanCounts()
     */
    public void setContextValue(@NonNull String context, int lifespanCount, @NonNull String key, Object value) {
        checkArgument(lifespanCount > 0, "Cannot set the context lifespan count to %s, the lifespan count should be " +
                "strictly greater than 0", lifespanCount);
        Log.debug("Setting context variable {0}.{1} to {2}", context, key, value);
        Map<String, Object> contextMap = setContext(context, lifespanCount);
        contextMap.put(key, value);
    }

    /**
     * Stores the provided {@code contextParameterValue} in the context.
     * <p>
     * This method extracts the context name and parameter key from the provided {@link ContextParameterValue}, by
     * navigating its {@link ContextParameter} and {@link Context} references. This method is
     * used as syntactic sugar to register {@link ContextParameterValue}s received from {@link RuntimeEventProvider}
     * s, see
     * {@link #setContextValue(String, int, String, Object)} to register a context value from {@link String} values.
     *
     * @param contextParameterValue the {@link ContextParameterValue} to store in the context.
     * @throws NullPointerException if the provided {@code contextParameterValue} is {@code null}
     * @see #setContextValue(String, int, String, Object)
     */
    public void setContextValue(@NonNull ContextParameterValue contextParameterValue) {
        String contextName = ((Context) contextParameterValue.getContextParameter().eContainer()).getName();
        String parameterName = contextParameterValue.getContextParameter().getName();
        Object parameterValue = contextParameterValue.getValue();
        int lifespanCount = contextParameterValue.getContextInstance().getLifespanCount();
        this.setContextValue(contextName, lifespanCount, parameterName, parameterValue);
    }

    /**
     * Returns all the variables stored in the given {@code context}.
     * <p>
     * This method returns an unmodifiable {@link Map} holding the sub-context variables. To retrieve a specific
     * variable from {@link RuntimeContexts} see {@link #getContextValue(String, String)}.
     *
     * @param context the sub-context to retrieve the variables from
     * @return an unmodifiable {@link Map} holding the sub-context variables
     * @throws NullPointerException if the provided {@code context} is {@code null}
     * @see #getContextValue(String, String)
     */
    public @Nullable
    Map<String, Object> getContextVariables(@NonNull String context) {
        if (contexts.containsKey(context)) {
            return Collections.unmodifiableMap(contexts.get(context));
        } else {
            return null;
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
    public @Nullable
    Object getContextValue(@NonNull String context, @NonNull String key) {
        Map<String, Object> contextVariables = getContextVariables(context);
        if (nonNull(contextVariables)) {
            return contextVariables.get(key);
        } else {
            return null;
        }
    }

    /**
     * Returns the lifespan count of the provided {@code context}.
     * <p>
     * Context lifespan counts are set by {@link #setContextValue(String, int, String, Object)}, and decrementing after
     * each user input by {@link #decrementLifespanCounts()}, and are synchronized with the
     * {@link com.xatkit.core.recognition.IntentRecognitionProvider}'s remote context lifespan counts.
     *
     * @param context the context to retrieve the lifespan count of
     * @return the lifespan count of the provided {@code context}
     * @see #setContextValue(ContextParameterValue)
     * @see #setContextValue(String, int, String, Object)
     * @see #decrementLifespanCounts()
     */
    public int getContextLifespanCount(@NonNull String context) {
        Integer lifespanCount = this.lifespanCounts.get(context);
        if (nonNull(lifespanCount)) {
            return lifespanCount;
        } else {
            throw new XatkitException(MessageFormat.format("Cannot retrieve the lifespan count for the provided " +
                    "context {0} the context is not registered", context));
        }
    }

    /**
     * Decrements the lifespanCount of all the stored contexts, and remove them if there lifespanCount decreases to 0.
     * <p>
     * LifespanCounts corresponds to the number of user inputs that can be handled before removing the variable from
     * the current context. This method is called after each user input to take into account the new interaction and
     * decrement the lifespan counters from the live contexts.
     */
    public void decrementLifespanCounts() {
        Log.debug("Decrementing RuntimeContexts lifespanCounts");
        Iterator<Map.Entry<String, Integer>> it = lifespanCounts.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = it.next();
            if (entry.getValue() - 1 == 0) {
                it.remove();
                contexts.remove(entry.getKey());
            } else {
                entry.setValue(entry.getValue() - 1);
            }
        }
    }

    /**
     * Increments the lifespanCount of all the stored contexts.
     * <p>
     * This method is typically called after an invalid intent matching to make sure context parameters remain
     * unchanged for the next match. If the context are not incremented the automated call to
     * {@link #decrementLifespanCounts()} may delete them, making the intent recognition inconsistent.
     */
    public void incrementLifespanCounts() {
        Log.debug("Incrementing RuntimeContexts lifespanCounts");
        for (Map.Entry<String, Integer> entry : lifespanCounts.entrySet()) {
            entry.setValue(entry.getValue() + 1);
        }
    }

    /**
     * Merges the provided {@code other} {@link RuntimeContexts} into this one.
     * <p>
     * This method adds all the {@code contexts} and {@code values} of the provided {@code other}
     * {@link RuntimeContexts} to this one, performing a deep copy of the underlying {@link Map} structure, ensuring
     * that future updates on the {@code other} {@link RuntimeContexts} will not be applied on this one (such as value
     * and context additions/deletions).
     * <p>
     * Lifespan counts are also copied from the {@code other} {@link RuntimeContexts} to this one, ensuring that their
     * lifespan remains consistent in all the {@link RuntimeContexts}s containing them.
     * <p>
     * However, note that the values stored in the context {@link Map}s are not cloned, meaning that
     * {@link RuntimeAction}s updating existing values will update them for all the merged
     * context (see #129).
     *
     * @param other the {@link RuntimeContexts} to merge into this one
     * @throws XatkitException if the provided {@link RuntimeContexts} defines at least one {@code context} with the
     *                         same name as one of the {@code contexts} stored in this {@link RuntimeContexts}
     */
    public void merge(@NonNull RuntimeContexts other) {
        other.getContextMap().forEach((k, v) -> {
            Map<String, Object> variableMap = new HashMap<>();
            if (this.contexts.containsKey(k)) {
                Log.debug("Overriding existing context: {0}", k);
                /*
                 * Add all the variables that are already stored in the context, they may be overridden by variables
                 * from {@code other}.
                 */
                variableMap.putAll(this.contexts.get(k));
            }
            /*
             * v1 is not cloned here, so concrete values are shared between the contexts. This may be an
             * issue if some actions update existing context variables. In this case we'll need to implement
             * a deep copy of the variables themselves. (see #129)
             */
            variableMap.putAll(v);
            this.contexts.put(k, variableMap);
            /*
             * Merge the lifespan counts in the current context.
             */
            this.lifespanCounts.put(k, other.getContextLifespanCount(k));
        });
    }

    /**
     * Returns an unmodifiable {@link Map} representing the stored context values.
     *
     * @return an unmodifiable {@link Map} representing the stored context values
     */
    public Map<String, Map<String, Object>> getContextMap() {
        return this.contexts;
    }

    /**
     * Returns an unmodifiable {@link Map} representing the stored lifespan counts.
     *
     * @return an unmodifiable {@link Map} representing the stored lifespan counts
     */
    public Map<String, Integer> getLifespanCountsMap() {
        return Collections.unmodifiableMap(lifespanCounts);
    }
}
