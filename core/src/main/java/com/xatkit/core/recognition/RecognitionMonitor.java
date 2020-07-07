package com.xatkit.core.recognition;

import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.RecognizedIntent;

public interface RecognitionMonitor {
    
    /**
     * Logs the recognition information from the provided {@code recognizedIntent} and {@code session}.
     *
     * @param session          the {@link XatkitSession} from which the {@link RecognizedIntent} has been created
     * @param recognizedIntent the {@link RecognizedIntent} to log
     */
    void logRecognizedIntent(XatkitSession session, RecognizedIntent intent);

    /**
     * If MAPDB: Commit the pending operations on the database and closes the connection. 
     * If INFLUXDB: Closes connection to database. Changes should be commited, but check influxDB doc in case some actions need to be performed!
     */
	void shutdown();
}