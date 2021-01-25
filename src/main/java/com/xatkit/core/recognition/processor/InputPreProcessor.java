package com.xatkit.core.recognition.processor;

import com.xatkit.execution.StateContext;

/**
 * Applies a pre-processing function on the provided {@code input}.
 * <p>
 * This interface is a functional interface, client code can create an {@link InputPreProcessor} with the following
 * code: {@code (input, context} -> { // pre-processing }}.
 */
@FunctionalInterface
public interface InputPreProcessor {

    /**
     * Initializes the pre-processor.
     * <p>
     * This method is called after the construction of <b>all</b> the pre-processors, and can be used to initialize
     * services used by multiple pre-processors (e.g. a NLP service that needs to be warmed-up).
     * <p>
     * Sub-classes should override this method if they need to perform initialization steps that cannot be performed
     * when constructing the pre-processors.
     */
    default void init() {
    }

    /**
     * Processes the provided {@code input}.
     * <p>
     * This method is called with the {@code session} associated to the provided {@code input} in order to define
     * advanced pre-processing functions taking into account session's content.
     *
     * @param input   the input to process
     * @param context the {@link StateContext} associated to the {@code input}
     * @return the processed {@code input}
     */
    String process(String input, StateContext context);
}
