package fr.zelus.jarvis.log.module.action;

import fr.zelus.jarvis.core.session.JarvisContext;

public class LogErrorTest extends LogActionTest {

    private static String ERROR_TAG = "[ERROR]";

    @Override
    protected LogAction createLogAction(String message) {
        return new LogError(logModule, new JarvisContext(), message);
    }

    @Override
    protected String expectedLogTag() {
        return ERROR_TAG;
    }
}
