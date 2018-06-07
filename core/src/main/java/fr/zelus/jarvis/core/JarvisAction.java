package fr.zelus.jarvis.core;

import java.text.MessageFormat;

/**
 * Represents an action to execute by the {@link JarvisCore} component.
 * <p>
 * {@link JarvisAction}s are created by {@link JarvisModule}s when handling
 * {@link com.google.cloud.dialogflow.v2.Intent}s.
 *
 * @see JarvisCore
 * @see JarvisModule
 */
public abstract class JarvisAction implements Runnable {

    /**
     * The name of the {@link JarvisAction}.
     * <p>
     * This information is used for debugging purpose, and allows to provide meaningful {@link #toString()} method.
     */
    private String name;

    /**
     * Constructs a new {@link JarvisAction} with the provided {@code name}.
     *
     * @param name the name of the {@link JarvisAction}
     */
    public JarvisAction(String name) {
        this.name = name;
    }

    /**
     * Runs the action.
     * <p>
     * This method should not be called manually, and is handled by the {@link JarvisCore} component, that
     * orchestrates the {@link JarvisAction}s returned by the registered {@link JarvisModule}s.
     *
     * @see JarvisCore
     */
    @Override
    public abstract void run();

    @Override
    public String toString() {
        return MessageFormat.format("{0} ({1})", name, super.toString());
    }
}
