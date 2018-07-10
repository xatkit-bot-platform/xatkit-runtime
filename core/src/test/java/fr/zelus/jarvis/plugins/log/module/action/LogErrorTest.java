package fr.zelus.jarvis.plugins.log.module.action;

import fr.zelus.jarvis.core.session.JarvisSession;

public class LogErrorTest extends LogActionTest {

    private static String ERROR_TAG = "[ERROR]";

    @Override
    protected LogAction createLogAction(String message) {
        return new LogError(logModule, new JarvisSession("id"), message);
    }

    @Override
    protected String expectedLogTag() {
        return ERROR_TAG;
    }
}
