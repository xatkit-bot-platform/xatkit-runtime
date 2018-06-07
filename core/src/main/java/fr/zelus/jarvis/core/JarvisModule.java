package fr.zelus.jarvis.core;

import com.google.cloud.dialogflow.v2.Intent;

import java.util.List;

/**
 * The concrete implementation of a {@link fr.zelus.jarvis.module.Module} definition.
 * <p>
 * A {@link JarvisModule} defines a set of {@link JarvisAction}s that are concrete implementation of
 * {@link fr.zelus.jarvis.module.Module} actions that can be processed by the {@link JarvisCore} broker. It also
 * provides a set of utility methods to check whether a specific {@link Intent} can be processed, and retrieve
 * {@link JarvisAction}s to compute from a given {@link Intent}.
 *
 * @see fr.zelus.jarvis.module.Module
 * @see JarvisCore
 * @see JarvisAction
 */
public interface JarvisModule {

    String getName();

    /**
     * Checks whether the module can process the provided {@code intent}.
     * <p>
     * <b>Note:</b> this method should not modify the provided {@code intent}.
     *
     * @param intent the DialogFlow {@link Intent} to check
     * @return {@code true} if the module can process the provided {@code intent}, {@code false} otherwise
     */
    boolean acceptIntent(final Intent intent);

    /**
     * Processes the provided {@code intent}.
     * <p>
     * Processing an {@link Intent} does not have a direct impact on other {@link JarvisModule}s: each {@link Intent}
     * is processed by all the {@link JarvisModule} that accept it.
     * <p>
     * <b>Note:</b> this method should not modify the provided {@code intent}.
     *
     * @param intent
     */
    JarvisAction handleIntent(final Intent intent);

    List<Class<JarvisAction>> getRegisteredActions();

    Class<JarvisAction> getActionWithName(String name);

}
