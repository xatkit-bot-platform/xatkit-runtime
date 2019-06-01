package com.xatkit.plugins.log.platform.action;

import com.xatkit.core.session.XatkitSession;
import fr.inria.atlanmod.commons.log.Log;

public class LogWarningTest extends LogActionTest {

    private static String WARNING_TAG = "[WARN]";

    @Override
    protected LogAction createLogAction(String message) {
        LogAction action = new LogWarning(logPlatform, new XatkitSession("id"), message);
        /*
         * Clear the appender if the action initialization generated logs.
         */
        try {
            Thread.sleep(200);
        } catch(InterruptedException e) {
            Log.error("An error occurred while waiting for new logged messages");
        }
        listAppender.clear();
        return action;
    }

    @Override
    protected String expectedLogTag() {
        return WARNING_TAG;
    }
}
