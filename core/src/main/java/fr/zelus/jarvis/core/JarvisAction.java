package fr.zelus.jarvis.core;

import java.text.MessageFormat;

/**
 * The concrete implementation of an {@link fr.zelus.jarvis.module.Action} definition.
 * <p>
 * A {@link JarvisAction} represents an atomic action that are automatically executed by the {@link JarvisCore}
 * component. Instances of this class are created by the associated {@link JarvisModule} from an input
 * {@link fr.zelus.jarvis.intent.RecognizedIntent}.
 * <p>
 * Note that {@link JarvisAction}s implementations must be stored in the <i>action</i> package of their associated
 * concrete {@link JarvisModule} implementation to enable their automated loading. For example, the action
 * <i>MyAction</i> defined in the module <i>myModulePackage.MyModule</i> should be stored in the package
 * <i>myModulePackage.action</i>
 *
 * @see fr.zelus.jarvis.module.Action
 * @see JarvisCore
 * @see JarvisModule
 */
public abstract class JarvisAction implements Runnable {

    /**
     * Returns the name of the action.
     * <p>
     * This method returns the value of {@link Class#getName()}, and can not be overridden by concrete subclasses.
     * {@link JarvisAction}'s names are part of jarvis' naming convention, and are used to dynamically load modules
     * and actions.
     *
     * @return the name of the action.
     */
    public final String getName() {
        return this.getClass().getSimpleName();
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
        return MessageFormat.format("{0} ({1})", getName(), super.toString());
    }
}
