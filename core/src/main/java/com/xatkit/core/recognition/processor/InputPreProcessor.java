package com.xatkit.core.recognition.processor;

import com.xatkit.core.session.XatkitSession;

/**
 * Applies a pre-processing function on the provided {@code input}.
 */
public interface InputPreProcessor {

    /**
     * Processes the provided {@code input}.
     * <p>
     * This method is called with the {@code session} associated to the provided {@code input} in order to define
     * advanced pre-processing functions taking into account session's content.
     *
     * @param input   the input to process
     * @param session the {@link XatkitSession} associated to the {@code input}
     * @return the processed {@code input}
     */
    String process(String input, XatkitSession session);
}
