package com.xatkit.core.interpreter;

import com.xatkit.core.session.XatkitSession;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds contextual information defined during a Xatkit common program execution.
 */
public class ExecutionContext {

    /**
     * Contains the values set by a Xatkit common program.
     */
    private Map<String, Object> values;

    private XatkitSession session;

    /**
     * Constructs an empty {@link ExecutionContext}.
     */
    public ExecutionContext() {
        this.values = new HashMap<>();
    }

    public void setSession(XatkitSession session) {
        this.session = session;
    }

    public XatkitSession getSession() {
        return this.session;
    }

    /**
     * Sets the {@code name} value with the provided {@code value}.
     * <p>
     * <b>Note:</b> the Xatkit common language doesn't allow to update the value of a variable, thus declared variables
     * using the {@code def} keyword are final and immutable.
     *
     * @param name  the name of the value to set
     * @param value the raw value to store
     * @throws IllegalArgumentException if a value with the provided {@code name} is already defined in this
     *                                  {@link ExecutionContext}
     */
    public void setValue(String name, Object value) {
        if (values.containsKey(name)) {
            throw new IllegalArgumentException(MessageFormat.format("Cannot set the value {0}, the value is already " +
                    "defined in this scope", name));
        }
        this.values.put(name, value);
    }

    /**
     * Returns the value defined with the provided {@code name}.
     *
     * @param name the name of the value to retrieve
     * @return the retrieved value if it exists, {@code null} otherwise
     * @throws IllegalArgumentException if there is no value defined with the provided {@code name}
     */
    public Object getValue(String name) {
        if (values.containsKey(name)) {
            return this.values.get(name);
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Cannot access the value {0}, the value is not " +
                    "defined in this scope", name));
        }
    }

    /**
     * Returns the number of values defined in this {@link ExecutionContext}.
     *
     * @return the number of values defined in this {@link ExecutionContext}
     */
    public int getValueCount() {
        return this.values.keySet().size();
    }

    /**
     * Clears the {@link ExecutionContext}.
     * <p>
     * <b>Note:</b> the cleared {@link ExecutionContext} is similar to the one built by calling
     * {@link ExecutionContext#ExecutionContext()} and can be reused to evaluate new programs.
     */
    public void clear() {
        this.values.clear();
    }
}
