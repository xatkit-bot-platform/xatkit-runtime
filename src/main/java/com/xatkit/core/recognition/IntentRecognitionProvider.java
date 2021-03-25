package com.xatkit.core.recognition;

import com.xatkit.core.recognition.processor.InputPreProcessor;
import com.xatkit.core.recognition.processor.IntentPostProcessor;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.RecognizedIntent;
import lombok.NonNull;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A unified wrapper to configure and query an intent recognition service.
 * <p>
 * This interface allows to specify pre/post processors, register intents and entities to the intent recognition
 * service, and retrieve {@link RecognizedIntent} instances from input text.
 */
public interface IntentRecognitionProvider {

    /**
     * The Default fallback intent that is returned when the user input does not match any registered intent.
     */
    DefaultFallbackIntent DEFAULT_FALLBACK_INTENT = new DefaultFallbackIntent();
    /*
     * We use a package private class here because we cannot statically initialize fields in interfaces (and we need
     * to set the name of the IntentDefinition).
     */

    /**
     * Sets the {@link InputPreProcessor}s.
     *
     * @param preProcessors the {@link InputPreProcessor} to set
     */
    void setPreProcessors(@NonNull List<InputPreProcessor> preProcessors);

    /**
     * Returns the {@link InputPreProcessor}s associated to this provider.
     *
     * @return the {@link InputPreProcessor}s associated to this provider
     */
    List<InputPreProcessor> getPreProcessors();

    /**
     * Set the {@link IntentPostProcessor}s.
     *
     * @param postProcessors the {@link IntentPostProcessor} to set
     */
    void setPostProcessors(@NonNull List<IntentPostProcessor> postProcessors);

    /**
     * Returns the {@link IntentPostProcessor}s associated to this provider.
     *
     * @return the {@link IntentPostProcessor}s associated to this provider
     */
    List<IntentPostProcessor> getPostProcessors();

    /**
     * Registers the provided {@code entityDefinition} in the underlying intent recognition provider.
     * <p>
     * <b>Note:</b> unless explicitly stated in subclasses, this method does not train the underlying machine
     * learning engine, so multiple entity registrations does not generate multiple training calls. Once all the
     * {@link EntityDefinition}s have been registered in the underlying intent recognition provider use
     * {@link #trainMLEngine()} to train the ML engine.
     *
     * @param entityDefinition the {@link EntityDefinition} to delete from the underlying intent recognition provider
     * @see #trainMLEngine()
     */
    void registerEntityDefinition(@NonNull EntityDefinition entityDefinition) throws IntentRecognitionProviderException;

    /**
     * Registers the provided {@code intentDefinition} in the underlying intent recognition provider.
     * <p>
     * <b>Note:</b> unless explicitly stated in subclasses, this method does not train the underlying machine
     * learning engine, so multiple intent registrations does not generate multiple training calls. Once all the
     * {@link IntentDefinition}s have been registered in the underlying intent recognition provider use
     * {@link #trainMLEngine()} to train the ML engine.
     *
     * @param intentDefinition the {@link IntentDefinition} to register to the underlying intent recognition provider
     * @throws IntentRecognitionProviderException if an error occurred when accessing the intent provider
     * @see #trainMLEngine()
     */
    void registerIntentDefinition(@NonNull IntentDefinition intentDefinition) throws IntentRecognitionProviderException;

    /**
     * Deletes the provided {@code entityDefinition} from the underlying intent recognition provider.
     * <p>
     * <b>Note:</b> unless explicitly stated in subclasses, this method does not train the underlying machine
     * learning engine, so multiple entity deletion does not generate multiple training calls. Once all the
     * {@link EntityDefinition}s have been deleted from the underlying intent recognition provider use
     * {@link #trainMLEngine()} to train the ML engine.
     *
     * @param entityDefinition the {@link EntityDefinition} to delete from the underlying intent recognition provider
     * @throws IntentRecognitionProviderException if an error occurred when accessing the intent provider
     * @see #trainMLEngine()
     */
    void deleteEntityDefinition(@NonNull EntityDefinition entityDefinition) throws IntentRecognitionProviderException;

    /**
     * Deletes the provided {@code intentDefinition} from the underlying intent recognition provider.
     * <p>
     * <b>Note:</b> unless explicitly stated in subclasses, this method does not train the underlying machine
     * learning engine, so multiple intent deletion does not generate multiple training calls. Once all the
     * {@link IntentDefinition}s have been deleted from the underlying intent recognition provider use
     * {@link #trainMLEngine()} to train the ML engine.
     *
     * @param intentDefinition the {@link IntentDefinition} to delete from the underlying intent recognition provider
     * @throws IntentRecognitionProviderException if an error occurred when accessing the intent provider
     * @see #trainMLEngine()
     */
    void deleteIntentDefinition(@NonNull IntentDefinition intentDefinition) throws IntentRecognitionProviderException;

    /**
     * Trains the underlying intent recognition provider.
     * <p>
     * <b>Note:</b> this method returns once the intent recognition provider's training is complete. However, the
     * propagation of the training information may not be complete when this method returns.
     *
     * @throws IntentRecognitionProviderException if an error occurred when accessing the intent provider
     */
    void trainMLEngine() throws IntentRecognitionProviderException;

    /**
     * Creates a new {@link StateContext} from the provided {@code contextId}.
     *
     * @param contextId the identifier of the context to create
     * @return a new {@link StateContext} for the provided {@code contextId}
     * @throws IntentRecognitionProviderException if an error occurred when accessing the intent provider
     */
    StateContext createContext(@NonNull String contextId) throws IntentRecognitionProviderException;

    /**
     * Shuts down the intent recognition provider client and invalidates the remaining sessions.
     * <p>
     * <b>Note:</b> calling this method invalidates the intent recognition provider client connection, and thus this
     * class cannot be used to access the intent recognition provider anymore.
     *
     * @throws IntentRecognitionProviderException if an error occurred when accessing the intent provider
     */
    void shutdown() throws IntentRecognitionProviderException;

    /**
     * Returns whether the intent recognition provider client is shutdown.
     *
     * @return {@code true} if the intent recognition provider client is shutdown, {@code false} otherwise
     */
    boolean isShutdown();

    /**
     * Returns the {@link RecognizedIntent} extracted from te provided {@code input}.
     * <p>
     * This method uses the provided {@code session} to extract contextual intents, such as follow-up or
     * context-based intents.
     * <p>
     * This method applies the pre-processing functions associated to this provider on the given {@code input}, and
     * the post-processing functions on the returned {@link RecognizedIntent}.
     * <p>
     * This method returns a {@link RecognizedIntent} with {@code RecognizedIntent#getDefinition() ==
     * DEFAULT_FALLBACK_INTENT} if the provided {@code input} does not match any intent.
     *
     * @param input   the {@link String} representing the textual input to process and extract the intent from
     * @param context the {@link StateContext} used to access context information
     * @return the post-processed {@link RecognizedIntent} extracted from the provided {@code input} and {@code session}
     * @throws IntentRecognitionProviderException if an error occurred when accessing the intent provider
     * @see #DEFAULT_FALLBACK_INTENT
     */
    @NonNull RecognizedIntent getIntent(@NonNull String input, @NonNull StateContext context)
            throws IntentRecognitionProviderException;

    /**
     * Returns the {@link RecognitionMonitor} associated to this intent recognition provider.
     *
     * @return the {@link RecognitionMonitor}, or {@code null} if analytics monitoring is disabled.
     */
    @Nullable
    RecognitionMonitor getRecognitionMonitor();

}
