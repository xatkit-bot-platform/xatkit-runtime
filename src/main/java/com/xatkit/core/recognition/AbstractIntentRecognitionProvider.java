package com.xatkit.core.recognition;

import com.xatkit.core.recognition.processor.InputPreProcessor;
import com.xatkit.core.recognition.processor.IntentPostProcessor;
import com.xatkit.execution.State;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.RecognizedIntent;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.isNull;

/**
 * An {@link IntentRecognitionProvider} that takes care of applying pre/post processing.
 * <p>
 * This class provides an implementation of {@link #getIntent(String, StateContext)} that runs the registered
 * preprocessors, log information regarding the recognition, and applies the registered postprocessors to the
 * retrieved {@link RecognizedIntent}.
 * <p>
 * Subclasses must provide an implementation of {@link #getIntentInternal(String, StateContext)}, where the
 * {@link RecognizedIntent} is retrieved from the provided {@code input}.
 */
public abstract class AbstractIntentRecognitionProvider implements IntentRecognitionProvider {

    /**
     * The {@link List} of {@link InputPreProcessor}s set for this provider.
     */
    private List<InputPreProcessor> preProcessors = new ArrayList<>();

    /**
     * The {@link List} of {@link IntentPostProcessor}s set for this provider.
     */
    private List<IntentPostProcessor> postProcessors = new ArrayList<>();

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the provided {@code preProcessors} is {@code null}
     */
    @Override
    public final void setPreProcessors(@NonNull List<InputPreProcessor> preProcessors) {
        /*
         * Use addAll(preProcessors) instead of direct affectation (this.preProcessors = preProcessors) to be safe if
         * the subclass needs to register additional processors.
         * For example: a IntentRecognitionProvider may need to process inputs in a specific way. Depending on the
         * execution order this additional processor may be overriden by a call to setProcessors occurring after
         * initialization.
         */
        this.preProcessors.addAll(preProcessors);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the provided {@code postProcessors} is {@code null}
     */
    @Override
    public final void setPostProcessors(@NonNull List<IntentPostProcessor> postProcessors) {
        /*
         * Use addAll(postProcessors) instead of direct affectation (this.postProcessors = postProcessors) to be safe
         * if the subclass needs to register additional processors.
         * For example: a IntentRecognitionProvider may need to process recognized intents in a specific way.
         * Depending on the execution order this additional processor may be overriden by a call to setProcessors
         * occurring after initialization.
         */
        this.postProcessors.addAll(postProcessors);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final List<InputPreProcessor> getPreProcessors() {
        return this.preProcessors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final List<IntentPostProcessor> getPostProcessors() {
        return this.postProcessors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void registerEntityDefinition(@NonNull EntityDefinition entityDefinition)
            throws IntentRecognitionProviderException;

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void registerIntentDefinition(@NonNull IntentDefinition intentDefinition)
            throws IntentRecognitionProviderException;

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void deleteEntityDefinition(@NonNull EntityDefinition entityDefinition)
            throws IntentRecognitionProviderException;

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void deleteIntentDefinition(@NonNull IntentDefinition intentDefinition)
            throws IntentRecognitionProviderException;

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void trainMLEngine() throws IntentRecognitionProviderException;

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract StateContext createContext(@NonNull String sessionId)
            throws IntentRecognitionProviderException;

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
    public final @NonNull RecognizedIntent getIntent(@NonNull String input, @NonNull StateContext context)
            throws IntentRecognitionProviderException {
        String preProcessedInput = input;
        for (InputPreProcessor preProcessor : this.preProcessors) {
            long preStart = System.currentTimeMillis();
            preProcessedInput = preProcessor.process(input, context);
            long preEnd = System.currentTimeMillis();
            Log.debug("Time to execute pre-processor {0}: {1}ms", preProcessor.getClass().getSimpleName(),
                    (preEnd - preStart));
        }
        long recognitionStart = System.currentTimeMillis();
        RecognizedIntent recognizedIntent = getIntentInternal(preProcessedInput, context);
        long recognitionEnd = System.currentTimeMillis();
        Log.debug("Time to recognize the intent with {0}: {1}ms", this.getClass().getSimpleName(),
                (recognitionEnd - recognitionStart));
        for (IntentPostProcessor postProcessor : this.postProcessors) {
            long postStart = System.currentTimeMillis();
            recognizedIntent = postProcessor.process(recognizedIntent, context);
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
     * {@link #getIntent(String, StateContext)}). Subclasses implementing this method should not take care of
     * pre/post processing.
     *
     * @param input   the textual input to process and extract the intent from
     * @param context the {@link StateContext} used to access context information
     * @return the {@link RecognizedIntent} extracted from the provided {@code input} and {@code session}
     * @throws NullPointerException               if the provided {@code input} or {@code session} is {@code null}
     * @throws IntentRecognitionProviderException if an error occurred when accessing the intent provider
     */
    protected abstract RecognizedIntent getIntentInternal(@NonNull String input, @NonNull StateContext context)
            throws IntentRecognitionProviderException;

    /**
     * Returns the {@link RecognizedIntent} that matches best the current {@code context}.
     * <p>
     * This method looks for the {@link RecognizedIntent} with the highest confidence <b>and</b> that can be accepted
     * by the current context (i.e. there is a {@code Enable[IntentName]} context defined in {@code context
     * .getNlpSession()}.
     * <p>
     * A {@link RecognizedIntent} with the {@link #DEFAULT_FALLBACK_INTENT} definition is returned if doesn't find
     * any acceptable candidate.
     *
     * @param recognizedIntents the {@link Collection} of {@link RecognizedIntent} to get the best candidate from
     * @param context           the current {@link StateContext}
     * @return the best {@link RecognizedIntent} if it exists, or a {@link #DEFAULT_FALLBACK_INTENT}
     * @throws IllegalArgumentException if the provided {@code recognizedIntents} is empty
     */
    protected RecognizedIntent getBestCandidate(@NonNull Collection<RecognizedIntent> recognizedIntents,
                                                @NonNull StateContext context) {
        checkArgument(!recognizedIntents.isEmpty(), "Cannot get the best candidate from the provided collection: the "
                + "collection is empty");
        RecognizedIntent bestCandidate =
                recognizedIntents.stream()
                        .sorted(Comparator.comparingDouble(RecognizedIntent::getRecognitionConfidence).reversed())
                        .filter(intent -> {
                            State state = context.getState();
                            /*
                             * We cast intent.getDefinition() here to avoid suspicious contains call detection.
                             */
                            return state.getAllAccessedIntents().contains((IntentDefinition) intent.getDefinition());
                        })
                        .findFirst().orElse(null);
        if (isNull(bestCandidate)) {
            bestCandidate = IntentFactory.eINSTANCE.createRecognizedIntent();
            bestCandidate.setDefinition(DEFAULT_FALLBACK_INTENT);
            bestCandidate.setRecognitionConfidence(0);
        }
        return bestCandidate;
    }

    /**
     * Returns the {@link RecognitionMonitor} associated to this intent recognition provider.
     *
     * @return the {@link RecognitionMonitor}, or {@code null} if analytics monitoring is disabled.
     */
    @Override
    public abstract @Nullable
    RecognitionMonitor getRecognitionMonitor();
}
