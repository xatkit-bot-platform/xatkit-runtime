package com.xatkit.core.recognition.processor;

import com.xatkit.execution.StateContext;
import com.xatkit.intent.RecognizedIntent;

/**
 * Applies a post-processing function on the provided {@code recognizedIntent}.
 * <p>
 * This interface is a functional interface, client code can create an {@link IntentPostProcessor} with the following
 * code: {@code (recognizedIntent, context) -> { // post-processing }}.
 */
@FunctionalInterface
public interface IntentPostProcessor {

    /**
     * Initializes the post-processor.
     * <p>
     * This method is called after the construction of <b>all</b> the post-processors, and can be used to initialize
     * services used by multiple post-processors (e.g. a NLP service that needs to be warmed-up).
     * <p>
     * Sub-classes should override this method if they need to perform initialization steps that cannot be performed
     * when constructing the post-processor.
     */
    default void init() {
    }

    /**
     * Processes the provided {@code recognizedIntent}.
     * <p>
     * This method is called with the {@code session} associated to the provided {@code recognizedIntent} in order to
     * define advanced post-processing functions taking into account session's content.
     *
     * @param recognizedIntent the {@link RecognizedIntent} to process
     * @param context          the {@link StateContext} associated to the {@code recognizedIntent}
     * @return the processed {@code recognizedIntent}
     */
    RecognizedIntent process(RecognizedIntent recognizedIntent, StateContext context);
}
