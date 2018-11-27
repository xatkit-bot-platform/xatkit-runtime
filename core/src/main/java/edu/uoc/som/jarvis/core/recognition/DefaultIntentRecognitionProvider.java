package edu.uoc.som.jarvis.core.recognition;

import edu.uoc.som.jarvis.core.JarvisCore;
import edu.uoc.som.jarvis.core.session.JarvisSession;
import edu.uoc.som.jarvis.intent.IntentDefinition;
import edu.uoc.som.jarvis.intent.RecognizedIntent;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;

import java.text.MessageFormat;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * A default {@link IntentRecognitionProvider} that manages {@link JarvisSession}s.
 * <p>
 * This class does not implement any {@link IntentDefinition}-related method, and throws
 * {@link UnsupportedOperationException}s when calling them. {@link DefaultIntentRecognitionProvider} should be
 * used when the created bot does not require any {@link RecognizedIntent} computation, and only requires session
 * management. This is for example the case for event-driven bots that only forward received events to a given output.
 * <p>
 * The {@link DefaultIntentRecognitionProvider} will be used by jarvis if the application's {@link Configuration}
 * file does not contain specific {@link IntentRecognitionProvider} properties (see
 * {@link IntentRecognitionProviderFactory#getIntentRecognitionProvider(JarvisCore, Configuration)}).
 *
 * @see UnsupportedOperationException
 * @see IntentRecognitionProviderFactory
 */
public class DefaultIntentRecognitionProvider implements IntentRecognitionProvider {

    /**
     * The application's {@link Configuration}.
     * <p>
     * This {@link Configuration} is used to customize the created {@link JarvisSession}s.
     */
    private Configuration configuration;

    /**
     * A boolean storing whether the provider has been shut down.
     * <p>
     * The {@link DefaultIntentRecognitionProvider} is not connected to any remote API, and calling its
     * {@link #shutdown()} method only sets this value (and the {@link #isShutdown()} return value) to {@code true},
     * allowing to properly close the application.
     */
    private boolean isShutdown;

    /**
     * Constructs a {@link DefaultIntentRecognitionProvider} with the provided {@code configuration}.
     *
     * @param configuration the {@link Configuration} used to customize the created {@link JarvisSession}s
     */
    public DefaultIntentRecognitionProvider(Configuration configuration) {
        checkNotNull(configuration, "Cannot create a %s with the provided %s %s", this.getClass().getSimpleName(),
                Configuration.class.getSimpleName(), configuration);
        Log.info("Starting {0}", this.getClass().getSimpleName());
        this.configuration = configuration;
        this.isShutdown = false;
    }

    /**
     * This method is not implemented and throws an {@link UnsupportedOperationException}.
     * <p>
     * Use valid {@link IntentRecognitionProvider}s to enable {@link IntentDefinition} registration.
     *
     * @param intentDefinition the {@link IntentDefinition} to register to the underlying intent recognition provider
     * @throws IntentRecognitionProviderException when called
     */
    @Override
    public void registerIntentDefinition(IntentDefinition intentDefinition) {
        throw new UnsupportedOperationException(MessageFormat.format("Unsupported operation: cannot register {0}" +
                " to {1}, please use a valid {2} to manage {0}s", IntentDefinition.class.getSimpleName(), this
                .getClass().getSimpleName(), IntentRecognitionProvider.class.getSimpleName()));
    }

    /**
     * This method is not implemented and throws an {@link UnsupportedOperationException}.
     * <p>
     * Use valid {@link IntentRecognitionProvider}s to enable {@link IntentDefinition} deletion.
     *
     * @param intentDefinition the {@link IntentDefinition} to delete from the underlying intent recognition provider
     * @throws IntentRecognitionProviderException when called
     */
    @Override
    public void deleteIntentDefinition(IntentDefinition intentDefinition) {
        throw new UnsupportedOperationException(MessageFormat.format("Unsupported operation: cannot delete {0} " +
                "from {1}, please use a valid {2} to manage {0}s", IntentDefinition.class.getSimpleName(), this
                .getClass().getSimpleName(), IntentRecognitionProvider.class.getSimpleName()));
    }

    /**
     * This method is not implemented and throws an {@link UnsupportedOperationException}.
     * <p>
     * Use valid {@link IntentRecognitionProvider}s to enable ML training.
     *
     * @throws IntentRecognitionProviderException when called
     */
    @Override
    public void trainMLEngine() {
        throw new UnsupportedOperationException(MessageFormat.format("Unsupported operation: cannot train the ML" +
                        " engine of {0}, please use a valid {1} to manage {2}", this.getClass().getSimpleName(),
                IntentRecognitionProvider.class.getSimpleName(), IntentDefinition.class.getSimpleName()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JarvisSession createSession(String sessionId) {
        return new JarvisSession(sessionId, configuration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        this.isShutdown = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isShutdown() {
        return isShutdown;
    }

    /**
     * This method is not implemented and throws an {@link UnsupportedOperationException}.
     * <p>
     * Use valid {@link IntentRecognitionProvider}s to enable {@link RecognizedIntent} computation.
     *
     * @param input   the {@link String} representing the textual input to process and extract the intent from
     * @param session the {@link JarvisSession} used to access context information
     * @return this method throws an {@link IntentRecognitionProviderException}
     * @throws IntentRecognitionProviderException when called
     */
    @Override
    public RecognizedIntent getIntent(String input, JarvisSession session) {
        throw new UnsupportedOperationException(MessageFormat.format("Unsupported operation: cannot compute the " +
                "{0} from {1}, please use a valid {2} to manage {0} computation", RecognizedIntent.class
                .getSimpleName(), this.getClass().getSimpleName(), IntentRecognitionProvider.class.getSimpleName()));
    }
}
