package fr.zelus.jarvis.core.module.log.action;

public class LogWarningTest extends LogActionTest {

    private static String WARNING_TAG = "[WARN]";

    @Override
    protected LogAction createLogAction(String message) {
        return new LogWarning(message);
    }

    @Override
    protected String expectedLogTag() {
        return WARNING_TAG;
    }
}
