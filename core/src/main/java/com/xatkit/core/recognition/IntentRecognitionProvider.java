package com.xatkit.core.recognition;

import com.xatkit.core.XatkitCore;
import com.xatkit.core.recognition.processor.InputPreProcessor;
import com.xatkit.core.recognition.processor.IntentPostProcessor;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.RecognizedIntent;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * A unified wrapper for concrete intent recognition providers.
 * <p>
 * This interface provides utility methods to connect to a given intent recognition provider, register and delete
 * {@link IntentDefinition}s, create {@link XatkitSession}s, and compute the {@link RecognizedIntent} from a given
 * input text.
 */
public abstract class IntentRecognitionProvider {

    /**
     * The {@link List} of {@link InputPreProcessor}s set for this {@link IntentRecognitionProvider}.
     *
     * @see IntentRecognitionProviderFactory#getIntentRecognitionProvider(XatkitCore, Configuration)
     * @see #getIntent(String, XatkitSession)
     */
    private List<? extends InputPreProcessor> preProcessors = new ArrayList<>();

    /**
     * The {@link List} of {@link IntentPostProcessor}s set for this {@link IntentRecognitionProvider}.
     *
     * @see IntentRecognitionProviderFactory#getIntentRecognitionProvider(XatkitCore, Configuration)
     * @see #getIntent(String, XatkitSession)
     */
    private List<? extends IntentPostProcessor> postProcessors = new ArrayList<>();

    /**
     * Sets the {@link InputPreProcessor}s.
     *
     * @param preProcessors the {@link InputPreProcessor} to set
     */
    public final void setPreProcessors(List<? extends InputPreProcessor> preProcessors) {
        this.preProcessors = preProcessors;
    }

    /**
     * Set the {@link IntentPostProcessor}s.
     *
     * @param postProcessors the {@link IntentPostProcessor} to set
     */
    public final void setPostProcessors(List<? extends IntentPostProcessor> postProcessors) {
        this.postProcessors = postProcessors;
    }

    /**
     * Returns the {@link InputPreProcessor}s associated to this {@link IntentRecognitionProvider}.
     * <p>
     * This method is package-private for testing purposes.
     *
     * @return the {@link InputPreProcessor}s associated to this {@link IntentRecognitionProvider}
     */
    final List<? extends InputPreProcessor> getPreProcessors() {
        return this.preProcessors;
    }

    /**
     * Returns the {@link IntentPostProcessor}s associated to this {@link IntentRecognitionProvider}.
     * <p>
     * This method is package-private for testing purposes.
     *
     * @return the {@link IntentPostProcessor}s associated to this {@link IntentRecognitionProvider}
     */
    final List<? extends IntentPostProcessor> getPostProcessors() {
        return this.postProcessors;
    }

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
    public abstract void registerEntityDefinition(EntityDefinition entityDefinition);

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
    public abstract void registerIntentDefinition(IntentDefinition intentDefinition);

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
    public abstract void deleteEntityDefinition(EntityDefinition entityDefinition);

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
    public abstract void deleteIntentDefinition(IntentDefinition intentDefinition);

    /**
     * Trains the underlying intent recognition provider.
     * <p>
     * <b>Note:</b> this method returns once the intent recognition provider's training is complete. However, the
     * propagation of the training information may not be complete when this method returns.
     */
    public abstract void trainMLEngine();

    /**
     * Creates a new {@link XatkitSession} from the provided {@code sessionId}.
     *
     * @param sessionId the identifier to create a session from
     * @return a new {@link XatkitSession} for the provided {@code sessionId}
     */
    public abstract XatkitSession createSession(String sessionId);

    /**
     * Shuts down the intent recognition provider client and invalidates the remaining sessions.
     * <p>
     * <b>Note:</b> calling this method invalidates the intent recognition provider client connection, and thus this
     * class cannot be used to access the intent recognition provider anymore.
     */
    public abstract void shutdown();

    /**
     * Returns whether the intent recognition provider client is shutdown.
     *
     * @return {@code true} if the intent recognition provider client is shutdown, {@code false} otherwise
     */
    public abstract boolean isShutdown();

    /**
     * Returns the {@link RecognizedIntent} extracted from te provided {@code input}.
     * <p>
     * This method uses the provided {@code session} to extract contextual intents, such as follow-up or
     * context-based intents.
     * <p>
     * This method applies the pre-processing functions associated to this {@link IntentRecognitionProvider} on the
     * given {@code input}, and the post-processing functions on the returned {@link RecognizedIntent}.
     *
     * @param input   the {@link String} representing the textual input to process and extract the intent from
     * @param session the {@link XatkitSession} used to access context information
     * @return the post-processed {@link RecognizedIntent} extracted from the provided {@code input} and {@code session}
     * @see IntentRecognitionProviderFactory#getIntentRecognitionProvider(XatkitCore, Configuration)
     */
    public final RecognizedIntent getIntent(String input, XatkitSession session) {
        String preProcessedInput = input;
        for (InputPreProcessor preProcessor : this.preProcessors) {
            long preStart = System.currentTimeMillis();
            preProcessedInput = preProcessor.process(input, session);
            long preEnd = System.currentTimeMillis();
            Log.debug("Time to execute pre-processor {0}: {1}ms", preProcessor.getClass().getSimpleName(),
                    (preEnd - preStart));
        }
        long recognitionStart = System.currentTimeMillis();
        RecognizedIntent recognizedIntent = getIntentInternal(preProcessedInput, session);
        long recognitionEnd = System.currentTimeMillis();
        Log.debug("Time to recognize the intent with {0}: {1}ms", this.getClass().getSimpleName(),
                (recognitionEnd - recognitionStart));
        for (IntentPostProcessor postProcessor : this.postProcessors) {
            long postStart = System.currentTimeMillis();
            recognizedIntent = postProcessor.process(recognizedIntent, session);
            long postEnd = System.currentTimeMillis();
            Log.debug("Time to execute post-processor {0}: {1}ms", postProcessor.getClass().getSimpleName(),
                    (postEnd - postStart));
        }
        return recognizedIntent;
    }

    /**
     * Returns the raw {@link RecognizedIntent} extracted from the provided {@code input}.
     * <p>
     * This method is called <b>after</b> pre-processing of the {@code input} (i.e. the given {@code input} is
     * already pre-processed), and does not apply any post-processing function (this is done by
     * {@link #getIntent(String, XatkitSession)}). Subclasses implementing this method should not take care of
     * pre/post processing.
     *
     * @param input   the textual input to process and extract the intent from
     * @param session the {@link XatkitSession} used to access context information
     * @return the {@link RecognizedIntent} extracted from the provided {@code input} and {@code session}
     */
    protected abstract RecognizedIntent getIntentInternal(String input, XatkitSession session);

    /**
     * Returns the {@link RecognitionMonitor} associated to this intent recognition provider.
     *
     * @return the {@link RecognitionMonitor}, or {@code null} if analytics monitoring is disabled.
     */
    public abstract @Nullable
    RecognitionMonitor getRecognitionMonitor();
}
