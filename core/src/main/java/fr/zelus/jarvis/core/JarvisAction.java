package fr.zelus.jarvis.core;

import fr.zelus.jarvis.core.session.JarvisContext;

import java.text.MessageFormat;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

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
 * @param <T> the concrete {@link JarvisModule} subclass type containing the action
 * @see fr.zelus.jarvis.module.Action
 * @see JarvisCore
 * @see JarvisModule
 */
public abstract class JarvisAction<T extends JarvisModule> implements Runnable {

    /**
     * The {@link JarvisModule} subclass containing this action.
     */
    protected T module;

    /**
     * The {@link JarvisContext} associated to this action.
     */
    protected JarvisContext context;

    /**
     * Constructs a new {@link JarvisModule} with the provided {@code containingModule}.
     *
     * @param containingModule the {@link JarvisModule} containing this action
     * @param context          the {@link JarvisContext} associated to this action
     * @throws NullPointerException if the provided {@code containingModule} or {@code context} is {@code null}
     */
    public JarvisAction(T containingModule, JarvisContext context) {
        checkNotNull(containingModule, "Cannot construct a {0} with a null containing module", this.getClass()
                .getSimpleName());
        checkNotNull(context, "Cannot construct a %s with a null %s", this.getClass().getSimpleName(), JarvisContext
                .class.getSimpleName());
        this.module = containingModule;
        this.context = context;
    }

    /**
     * Disable the default constructor, JarvisActions must be constructed with their containing module.
     */
    private JarvisAction() {
        /*
         * Disable the default constructor, JarvisActions must be constructed with their containing module.
         */
    }

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
