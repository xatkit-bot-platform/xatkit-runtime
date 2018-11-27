package edu.uoc.som.jarvis.core.session;

import edu.uoc.som.jarvis.core.JarvisException;
import edu.uoc.som.jarvis.core.platform.action.RuntimeAction;
import edu.uoc.som.jarvis.core.platform.io.RuntimeEventProvider;
import edu.uoc.som.jarvis.intent.Context;
import edu.uoc.som.jarvis.intent.ContextParameterValue;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;

/**
 * A variable container bound to a {@link JarvisSession}.
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
 * This class is heavily used by jarvis core component to pass {@link RuntimeAction} parameters,
 * and replace output message variables by their concrete values.
 */
public class RuntimeContexts {

    /**
     * The {@link Configuration} key to store maximum time to spend waiting for a context variable (in seconds).
     */
    public static String VARIABLE_TIMEOUT_KEY = "jarvis.context.variable.timeout";

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
    public RuntimeContexts(Configuration configuration) {
        checkNotNull(configuration, "Cannot construct a %s from the provided %s: %s", RuntimeContexts.class
                .getSimpleName(), Configuration.class.getSimpleName(), configuration);
        /*
         * Use ConcurrentHashMaps: the RuntimeContexts may be accessed and modified by multiple threads, in case of
         * multiple input messages or parallel RuntimeAction execution.
         */
        this.contexts = new ConcurrentHashMap<>();
        this.lifespanCounts = new ConcurrentHashMap<>();
        if (configuration.containsKey(VARIABLE_TIMEOUT_KEY)) {
            this.variableTimeout = configuration.getInt(VARIABLE_TIMEOUT_KEY);
            Log.info("Setting context variable timeout to {0}s", variableTimeout);
        } else {
            this.variableTimeout = DEFAULT_VARIABLE_TIMEOUT_VALUE;
            Log.info("Using default context variable timeout ({0}s)", DEFAULT_VARIABLE_TIMEOUT_VALUE);
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
     * @param context       the sub-context to store the value in
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
    public void setContextValue(String context, int lifespanCount, String key, Object value) {
        checkNotNull(context, "Cannot set the context value from the provided context %s", context);
        checkNotNull(key, "Cannot set the context vaule value from the provided key %s", key);
        checkArgument(lifespanCount > 0, "Cannot set the context lifespan count to %s, the lifespan count should be " +
                "strictly greater than 0", lifespanCount);
        Log.info("Setting context variable {0}.{1} to {2}", context, key, value);
        if (contexts.containsKey(context)) {
            Map<String, Object> contextValues = contexts.get(context);
            contextValues.put(key, value);
        } else {
            Map<String, Object> contextValues = new HashMap<>();
            contextValues.put(key, value);
            contexts.put(context, contextValues);
        }
        if (lifespanCounts.containsKey(context)) {
            int currentLifespan = lifespanCounts.get(context);
            if (currentLifespan < lifespanCount) {
                /*
                 * The provided lifespanCount is greater than the stored one, this means that we are dealing with a
                 * new context (i.e. a new Intent recognized from the IntentRecognitionProvider or a new Event
                 * received by an RuntimeEventProvider). Override the current value to keep the variable alive.
                 */
                Log.info("Overriding context {0} lifespanCount (previous: {1}, new: {2})", context, currentLifespan,
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
            Log.info("Setting context {0} lifespanCount to {1}", context, lifespanCount);
            lifespanCounts.put(context, lifespanCount);
        }
    }

    /**
     * Stores the provided {@code contextParameterValue} in the context.
     * <p>
     * This method extracts the context name and parameter key from the provided {@link ContextParameterValue}, by
     * navigating its {@link edu.uoc.som.jarvis.intent.ContextParameter} and {@link Context} references. This method is
     * used as syntactic sugar to register {@link ContextParameterValue}s received from {@link RuntimeEventProvider}s, see
     * {@link #setContextValue(String, int, String, Object)} to register a context value from {@link String} values.
     *
     * @param contextParameterValue the {@link ContextParameterValue} to store in the context.
     * @throws NullPointerException if the provided {@code contextParameterValue} is {@code null}
     * @see #setContextValue(String, int, String, Object)
     */
    public void setContextValue(ContextParameterValue contextParameterValue) {
        checkNotNull(contextParameterValue, "Cannot set the context value from the provided %s %s",
                ContextParameterValue.class.getSimpleName(), contextParameterValue);
        String contextName = ((Context) contextParameterValue.getContextParameter().eContainer()).getName();
        String parameterName = contextParameterValue.getContextParameter().getName();
        String parameterValue = contextParameterValue.getValue();
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
     * Returns the lifespan count of the provided {@code context}.
     * <p>
     * Context lifespan counts are set by {@link #setContextValue(String, int, String, Object)}, and decrementing after
     * each user input by {@link #decrementLifespanCounts()}, and are synchronized with the
     * {@link edu.uoc.som.jarvis.core.recognition.IntentRecognitionProvider}'s remote context lifespan counts.
     *
     * @param context the context to retrieve the lifespan count of
     * @return the lifespan count of the provided {@code context}
     * @see #setContextValue(ContextParameterValue)
     * @see #setContextValue(String, int, String, Object)
     * @see #decrementLifespanCounts()
     */
    public int getContextLifespanCount(String context) {
        checkNotNull(context, "Cannot find the lifespan count of context %s", context);
        Integer lifespanCount = this.lifespanCounts.get(context);
        if (nonNull(lifespanCount)) {
            return lifespanCount;
        } else {
            throw new JarvisException(MessageFormat.format("Cannot retrieve the lifespan count for the provided " +
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
        Log.info("Decrementing RuntimeContexts lifespanCounts");
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
     * @throws JarvisException if the provided {@link RuntimeContexts} defines at least one {@code context} with the
     *                         same name as one of the {@code contexts} stored in this {@link RuntimeContexts}
     */
    public void merge(RuntimeContexts other) {
        checkNotNull(other, "Cannot merge the provided %s %s", RuntimeContexts.class.getSimpleName(), other);
        other.getContextMap().forEach((k, v) -> {
            if (this.contexts.containsKey(k)) {
                throw new JarvisException(MessageFormat.format("Cannot merge the provided {0}, duplicated value for " +
                        "context {1}", RuntimeContexts.class.getSimpleName(), k));
            } else {
                Map<String, Object> variableMap = new HashMap<>();
                v.forEach((k1, v1) -> {
                    /*
                     * v1 is not cloned here, so concrete values are shared between the contexts. This may be an
                     * issue if some actions update existing context variables. In this case we'll need to implement
                     * a deep copy of the variables themselves. (see #129)
                     */
                    variableMap.put(k1, v1);
                });
                this.contexts.put(k, variableMap);
                /*
                 * Merge the lifespan counts in the current context.
                 */
                this.lifespanCounts.put(k, other.getContextLifespanCount(k));
            }
        });
    }

    /**
     * Returns an unmodifiable {@link Map} representing the stored context values.
     *
     * @return an unmodifiable {@link Map} representing the stored context values
     */
    public Map<String, Map<String, Object>> getContextMap() {
        return Collections.unmodifiableMap(contexts);
    }

    /**
     * Returns an unmodifiable {@link Map} representing the stored lifespan counts.
     *
     * @return an unmodifiable {@link Map} representing the stored lifespan counts
     */
    public Map<String, Integer> getLifespanCountsMap() {
        return Collections.unmodifiableMap(lifespanCounts);
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
        Log.info("Filling context values for message {0}", message);
        String outMessage = message;
        Matcher m = Pattern.compile("\\{\\$[^\\s\\}]+\\}").matcher(message);
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
                                printedValue = ((Future) value).get(variableTimeout, TimeUnit.SECONDS).toString();
                                Log.info("Found value {0} for {1}.{2}", printedValue, splitGroup[0],
                                        variableIdentifier);
                            } catch (InterruptedException | ExecutionException e) {
                                String errorMessage = MessageFormat.format("An error occurred when retrieving the " +
                                        "value of the variable {0}", variableIdentifier);
                                Log.error(errorMessage);
                                throw new JarvisException(e);
                            } catch (TimeoutException e) {
                                /*
                                 * The Future takes too long to compute, return a placeholder (see https://github
                                 * .com/gdaniel/jarvis/wiki/Troubleshooting#my-bot-sends-task-takes-too-long-to
                                 * -compute-messages).
                                 */
                                Log.error("The value for {0}.{1} took too long to complete, stopping it and returning" +
                                        " a placeholder", splitGroup[0], variableIdentifier);
                                ((Future) value).cancel(true);
                                printedValue = "<Task took too long to complete>";
                            } catch (CancellationException e) {
                                Log.error("Cannot retrieve the value for {0}.{1}: the task has been cancelled, " +
                                        "returning a placeholder", splitGroup[0], variableIdentifier);
                                printedValue = "<Task has been cancelled>";
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
