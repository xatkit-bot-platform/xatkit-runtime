package fr.zelus.jarvis.io;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisException;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;

import java.io.IOException;
import java.io.PipedOutputStream;

/**
 * An abstract class representing user input providers.
 * <p>
 * Concrete implementations of this class are dynamically instantiated by the {@link fr.zelus.jarvis.core.JarvisCore}
 * component, and their {@link #outputStream} is connected to jarvis internal {@link LineInputConsumer}. Note that
 * {@link InputProvider} instances are started in a dedicated {@link Thread}.
 * <p>
 * Instances of this class can be configured using the {@link Configuration}-based constructor, that enable to pass
 * additional parameters to the constructor.
 */
public abstract class InputProvider {

    /**
     * The {@link PipedOutputStream} where inputs are written.
     */
    protected PipedOutputStream outputStream;

    /**
     * Constructs a new {@link InputProvider}.
     * <p>
     * <b>Note</b>: this constructor should be used by {@link InputProvider}s that do not require additional
     * parameters to be initialized. In that case see {@link #InputProvider(Configuration)}.
     */
    public InputProvider() {
        this(new BaseConfiguration());
    }

    /**
     * Constructs a new {@link InputProvider} from the provided {@link Configuration}.
     * <p>
     * <b>Note</b>: this constructor will be called by jarvis internal engine when initializing the
     * {@link fr.zelus.jarvis.core.JarvisCore} component. Subclasses implementing this constructor typically
     * need additional parameters to be initialized, that can be provided in the {@code configuration}.
     *
     * @param configuration the {@link Configuration} used to initialize the {@link InputProvider}
     * @see fr.zelus.jarvis.core.JarvisCore
     */
    public InputProvider(Configuration configuration) {
        /*
         * Do nothing with the configuration, it can be used by subclasses that require additional initialization
         * information.
         */
        outputStream = new PipedOutputStream();
    }

    /**
     * Returns the {@link PipedOutputStream} where inputs are written.
     * <p>
     * This method is used by {@link fr.zelus.jarvis.core.JarvisCore} to construct the corresponding
     * {@link java.io.PipedInputStream} and retrieve user inputs to process.
     *
     * @return the {@link PipedOutputStream} where inputs are written
     */
    public PipedOutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * Closes the {@link InputProvider} and release its resources.
     * <p>
     * This method closes the underlying {@link #outputStream}. Subclasses must override this method to close
     * additional resources such as message handlers, websockets, or additional threads.
     */
    public void close() {
        try {
            this.outputStream.close();
        } catch (IOException e) {
            String errorMessage = "Cannot close the InputProvider's output stream";
            Log.error(errorMessage);
            throw new JarvisException(errorMessage, e);
        }
    }
}
