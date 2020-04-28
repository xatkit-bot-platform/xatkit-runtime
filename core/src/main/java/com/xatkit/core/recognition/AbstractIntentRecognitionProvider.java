package com.xatkit.core.recognition;

import com.xatkit.core.XatkitCore;
import com.xatkit.core.recognition.processor.InputPreProcessor;
import com.xatkit.core.recognition.processor.IntentPostProcessor;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.RecognizedIntent;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;
import org.apache.commons.configuration2.Configuration;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * An {@link IntentRecognitionProvider} that takes care of applying pre/post processing.
 * <p>
 * This class provides an implementation of {@link #getIntent(String, XatkitSession)} that runs the registered
 * preprocessors, log information regarding the recognition, and applies the registered postprocessors to the
 * retrieved {@link RecognizedIntent}.
 * <p>
 * Subclasses must provide an implementation of {@link #getIntentInternal(String, XatkitSession)}, where the
 * {@link RecognizedIntent} is retrieved from the provided {@code input}.
 */
public abstract class AbstractIntentRecognitionProvider implements IntentRecognitionProvider {

    /**
     * The {@link List} of {@link InputPreProcessor}s set for this provider.
     *
     * @see IntentRecognitionProviderFactory#getIntentRecognitionProvider(XatkitCore, Configuration)
     * @see #getIntent(String, XatkitSession)
     */
    private List<? extends InputPreProcessor> preProcessors = new ArrayList<>();

    /**
     * The {@link List} of {@link IntentPostProcessor}s set for this provider.
     *
     * @see IntentRecognitionProviderFactory#getIntentRecognitionProvider(XatkitCore, Configuration)
     * @see #getIntent(String, XatkitSession)
     */
    private List<? extends IntentPostProcessor> postProcessors = new ArrayList<>();

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the provided {@code preProcessors} is {@code null}
     */
    @Override
    public final void setPreProcessors(@NonNull List<? extends InputPreProcessor> preProcessors) {
        this.preProcessors = preProcessors;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the provided {@code postProcessors} is {@code null}
     */
    @Override
    public final void setPostProcessors(@NonNull List<? extends IntentPostProcessor> postProcessors) {
        this.postProcessors = postProcessors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final List<? extends InputPreProcessor> getPreProcessors() {
        return this.preProcessors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final List<? extends IntentPostProcessor> getPostProcessors() {
        return this.postProcessors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void registerEntityDefinition(@NonNull EntityDefinition entityDefinition) throws IntentRecognitionProviderException;

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void registerIntentDefinition(@NonNull IntentDefinition intentDefinition) throws IntentRecognitionProviderException;

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void deleteEntityDefinition(@NonNull EntityDefinition entityDefinition) throws IntentRecognitionProviderException;

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void deleteIntentDefinition(@NonNull IntentDefinition intentDefinition) throws IntentRecognitionProviderException;

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void trainMLEngine() throws IntentRecognitionProviderException;

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract XatkitSession createSession(@NonNull String sessionId) throws IntentRecognitionProviderException;

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void shutdown() throws IntentRecognitionProviderException;

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract boolean isShutdown();

    /**
     * {@inheritDoc}
     */
    @Override
    public final RecognizedIntent getIntent(@NonNull String input, @NonNull XatkitSession session) throws IntentRecognitionProviderException {
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
     * @throws NullPointerException               if the provided {@code input} or {@code session} is {@code null}
     * @throws IntentRecognitionProviderException if an error occurred when accessing the intent provider
     */
    protected abstract RecognizedIntent getIntentInternal(@NonNull String input, @NonNull XatkitSession session) throws IntentRecognitionProviderException;

    /**
     * Returns the {@link RecognitionMonitor} associated to this intent recognition provider.
     *
     * @return the {@link RecognitionMonitor}, or {@code null} if analytics monitoring is disabled.
     */
    @Override
    public abstract @Nullable
    RecognitionMonitor getRecognitionMonitor();
}
