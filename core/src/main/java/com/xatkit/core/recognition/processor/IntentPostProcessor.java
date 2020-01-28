package com.xatkit.core.recognition.processor;

import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.RecognizedIntent;

/**
 * Applies a post-processing function on the provided {@code recognizedIntent}.
 */
public interface IntentPostProcessor {

    /**
     * Processes the provided {@code recognizedIntent}.
     * <p>
     * This method is called with the {@code session} associated to the provided {@code recognizedIntent} in order to
     * define advanced post-processing functions taking into account session's content.
     *
     * @param recognizedIntent the {@link RecognizedIntent} to process
     * @param session          the {@link XatkitSession} associated to the {@code recognizedIntent}
     * @return the processed {@code recognizedIntent}
     */
    RecognizedIntent process(RecognizedIntent recognizedIntent, XatkitSession session);
}
