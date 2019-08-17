package com.xatkit.util;

import org.apache.commons.configuration2.MapConfiguration;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A {@link MapConfiguration} initialized with {@link System#getenv()} variables.
 * <p>
 * Unlike {@link org.apache.commons.configuration2.EnvironmentConfiguration}, {@link XatkitEnvironmentConfiguration}
 * allows to add new properties, and remove/clear loaded properties. This behavior is needed by Xatkit internal
 * classes, that dynamically add properties that can be reused by multiple components.
 * <p>
 * Note that updating properties initialized from {@link System#getenv()} can lead to unexpected behavior.
 */
public class XatkitEnvironmentConfiguration extends MapConfiguration {

    /**
     * Creates a {@link XatkitEnvironmentConfiguration} initialized with {@link System#getenv()} values.
     */
    public XatkitEnvironmentConfiguration() {
        super(System.getenv());
    }

    /**
     * Retrieves the value associated to the provided {@code key}.
     * <p>
     * This method first looks for an exact match of the provided {@code key}. If there is no value associated to it
     * (i.e. the method returns {@code null}) an additional lookup for environment variables is performed for {@code
     * key.toUpperCase().replaceAll("\\.", "_")}. See
     * <a href="https://github.com/xatkit-bot-platform/xatkit-runtime/wiki/Xatkit-Options">the documentation</a>
     * for additional information.
     *
     * @param key the key of the property to retrieve
     * @return the retrieved property value if it exists, {@code null} otherwise
     * @see #translateToEnvironmentVariable(String)
     */
    @Override
    protected Object getPropertyInternal(String key) {
        Object value = super.getPropertyInternal(key);
        if (isNull(value)) {
            value = super.getPropertyInternal(this.translateToEnvironmentVariable(key));
        }
        return value;
    }

    /**
     * Returns {@code true} if the configuration contains the provided {@code key}, {@code false} otherwise.
     * <p>
     * This method first looks for an exact match of the provided {@code key}. If there is no value associated to it
     * (i.e. the method returns {@code false}) an additional lookup for environment variables is performed for {@code
     * key.toUpperCase().replaceAll("\\.", "_")}. See
     * <a href="https://github.com/xatkit-bot-platform/xatkit-runtime/wiki/Xatkit-Options">the documentation</a>
     * for additional information.
     *
     * @param key the key of the property to check
     * @return {@code true} if the configuration contains the provided {@code key}, {@code false} otherwise
     */
    @Override
    protected boolean containsKeyInternal(String key) {
        boolean result = super.containsKeyInternal(key);
        if (!result) {
            result = super.containsKeyInternal(this.translateToEnvironmentVariable(key));
        }
        return result;
    }

    /**
     * Translates the provided {@code key} to an environment variable name.
     * <p>
     * This method returns the provided {@code key} in upper case, with its {@code .} replaced by {@code _} ({@code
     * key.toUpperCase().replaceAll("\\.", "_")}). See
     * <a href="https://github.com/xatkit-bot-platform/xatkit-runtime/wiki/Xatkit-Options">the documentation</a>
     * for additional information.
     *
     * @param key the key to translate to an environment variable name
     * @return the translated key
     */
    private String translateToEnvironmentVariable(String key) {
        if (nonNull(key)) {
            return key.toUpperCase().replaceAll("\\.", "_");
        }
        return null;
    }
}
