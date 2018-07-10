package fr.zelus.jarvis.io;

import fr.zelus.jarvis.core.JarvisCore;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An abstract class representing user input providers.
 * <p>
 * Concrete implementations of this class are dynamically instantiated by the {@link JarvisCore} component, and
 * and use it to notify the engine about new messages to handle. Note that {@link InputProvider} instances are
 * started in a dedicated {@link Thread}.
 * <p>
 * Instances of this class can be configured using the {@link Configuration}-based constructor, that enable to pass
 * additional parameters to the constructor.
 */
public abstract class InputProvider implements Runnable {

    /**
     * The {@link JarvisCore} instance used to handle input messages.
     */
    protected JarvisCore jarvisCore;

    /**
     * Constructs a new {@link InputProvider}.
     * <p>
     * <b>Note</b>: this constructor should be used by {@link InputProvider}s that do not require additional
     * parameters to be initialized. In that case see {@link #InputProvider(JarvisCore, Configuration)}.
     *
     * @param jarvisCore the {@link JarvisCore} instance used to handle input messages
     */
    public InputProvider(JarvisCore jarvisCore) {
        this(jarvisCore, new BaseConfiguration());
    }

    /**
     * Constructs a new {@link InputProvider} from the provided {@link JarvisCore} and {@link Configuration}.
     * <p>
     * <b>Note</b>: this constructor will be called by jarvis internal engine when initializing the
     * {@link fr.zelus.jarvis.core.JarvisCore} component. Subclasses implementing this constructor typically
     * need additional parameters to be initialized, that can be provided in the {@code configuration}.
     *
     * @param jarvisCore    the {@link JarvisCore} instance used to handle input messages
     * @param configuration the {@link Configuration} used to initialize the {@link InputProvider}
     */
    public InputProvider(JarvisCore jarvisCore, Configuration configuration) {
        /*
         * Do nothing with the configuration, it can be used by subclasses that require additional initialization
         * information.
         */
        checkNotNull(jarvisCore, "Cannot construct an instance of %s with a null JarvisCore", this.getClass()
                .getSimpleName());
        this.jarvisCore = jarvisCore;
    }

    public void close() {

    }
}
