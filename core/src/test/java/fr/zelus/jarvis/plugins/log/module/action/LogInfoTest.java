package fr.zelus.jarvis.plugins.log.module.action;

import fr.zelus.jarvis.core.session.JarvisSession;

public class LogInfoTest extends LogActionTest {

    private static String INFO_TAG = "[INFO]";

    @Override
    protected LogAction createLogAction(String message) {
        return new LogInfo(logModule, new JarvisSession("id"), message);
    }

    @Override
    protected String expectedLogTag() {
        return INFO_TAG;
    }
}
