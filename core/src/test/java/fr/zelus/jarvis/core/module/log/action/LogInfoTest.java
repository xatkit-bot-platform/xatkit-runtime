package fr.zelus.jarvis.core.module.log.action;

public class LogInfoTest extends LogActionTest {

    private static String INFO_TAG = "[INFO]";

    @Override
    protected LogAction createLogAction(String message) {
        return new LogInfo(logModule, message);
    }

    @Override
    protected String expectedLogTag() {
        return INFO_TAG;
    }
}
