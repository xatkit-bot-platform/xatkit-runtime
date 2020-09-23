package com.xatkit.core.recognition;

import com.xatkit.execution.StateContext;
import com.xatkit.intent.RecognizedIntent;

public interface RecognitionMonitor {

    /**
     * Logs the recognition information from the provided {@code recognizedIntent} and {@code session}.
     *
     * @param context the {@link StateContext} from which the {@link RecognizedIntent} has been created
     * @param intent  the {@link RecognizedIntent} to log
     */
    void logRecognizedIntent(StateContext context, RecognizedIntent intent);

    /**
     * Closes the connection to the database.
     */
    void shutdown();
}