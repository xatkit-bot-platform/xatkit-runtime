package com.xatkit.core.recognition;

import com.xatkit.core.session.JarvisSession;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.RecognizedIntent;

import javax.annotation.Nullable;

/**
 * A unified wrapper for concrete intent recognition providers.
 * <p>
 * This interface provides utility methods to connect to a given intent recognition provider, register and delete
 * {@link IntentDefinition}s, create {@link JarvisSession}s, and compute the {@link RecognizedIntent} from a given
 * input text.
 */
public interface IntentRecognitionProvider {

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
    void registerEntityDefinition(EntityDefinition entityDefinition);

    /**
     * Registers the provided {@code intentDefinition} in the underlying intent recognition provider.
     * <p>
     * <b>Note:</b> unless explicitly stated in subclasses, this method does not train the underlying machine
     * learning engine, so multiple intent registrations does not generate multiple training calls. Once all the
     * {@link IntentDefinition}s have been registered in the underlying intent recognition provider use
     * {@link #trainMLEngine()} to train the ML engine.
     *
     * @param intentDefinition the {@link IntentDefinition} to register to the underlying intent recognition provider
     * @see #trainMLEngine()
     */
    void registerIntentDefinition(IntentDefinition intentDefinition);

    /**
     * Deletes the provided {@code entityDefinition} from the underlying intent recognition provider.
     * <p>
     * <b>Note:</b> unless explicitly stated in subclasses, this method does not train the underlying machine
     * learning engine, so multiple entity deletion does not generate multiple training calls. Once all the
     * {@link EntityDefinition}s have been deleted from the underlying intent recognition provider use
     * {@link #trainMLEngine()} to train the ML engine.
     *
     * @param entityDefinition the {@link EntityDefinition} to delete from the underlying intent recognition provider
     * @see #trainMLEngine()
     */
    void deleteEntityDefinition(EntityDefinition entityDefinition);

    /**
     * Deletes the provided {@code intentDefinition} from the underlying intent recognition provider.
     * <p>
     * <b>Note:</b> unless explicitly stated in subclasses, this method does not train the underlying machine
     * learning engine, so multiple intent deletion does not generate multiple training calls. Once all the
     * {@link IntentDefinition}s have been deleted from the underlying intent recognition provider use
     * {@link #trainMLEngine()} to train the ML engine.
     *
     * @param intentDefinition the {@link IntentDefinition} to delete from the underlying intent recognition provider
     * @see #trainMLEngine()
     */
    void deleteIntentDefinition(IntentDefinition intentDefinition);

    /**
     * Trains the underlying intent recognition provider.
     * <p>
     * <b>Note:</b> this method returns once the intent recognition provider's training is complete. However, the
     * propagation of the training information may not be complete when this method returns.
     */
    void trainMLEngine();

    /**
     * Creates a new {@link JarvisSession} from the provided {@code sessionId}.
     *
     * @param sessionId the identifier to create a session from
     * @return a new {@link JarvisSession} for the provided {@code sessionId}
     */
    JarvisSession createSession(String sessionId);

    /**
     * Shuts down the intent recognition provider client and invalidates the remaining sessions.
     * <p>
     * <b>Note:</b> calling this method invalidates the intent recognition provider client connection, and thus this
     * class cannot be used to access the intent recognition provider anymore.
     */
    void shutdown();

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
     *
     * @param input   the {@link String} representing the textual input to process and extract the intent from
     * @param session the {@link JarvisSession} used to access context information
     * @return the {@link RecognizedIntent} extracted from the provided {@code input} and {@code session}
     */
    RecognizedIntent getIntent(String input, JarvisSession session);

    /**
     * Returns the {@link RecognitionMonitor} associated to this intent recognition provider.
     *
     * @return the {@link RecognitionMonitor}, or {@code null} if analytics monitoring is disabled.
     */
    @Nullable RecognitionMonitor getRecognitionMonitor();
}
