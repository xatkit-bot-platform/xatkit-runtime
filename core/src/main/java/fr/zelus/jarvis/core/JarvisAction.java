package fr.zelus.jarvis.core;

import java.text.MessageFormat;

/**
 * The concrete implementation of an {@link fr.zelus.jarvis.module.Action} definition.
 * <p>
 * A {@link JarvisAction} represents an atomic action that can be executed by the {@link JarvisCore} broker.
 * Instances of this class are created by the associated {@link JarvisModule} to handle DialogFlow's
 * {@link com.google.cloud.dialogflow.v2.Intent}s.
 *
 * @see fr.zelus.jarvis.module.Action
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
