package fr.zelus.jarvis.core.module.log.action;

public class LogErrorTest extends LogActionTest {

    private static String ERROR_TAG = "[ERROR]";

    @Override
    protected LogAction createLogAction(String message) {
        return new LogError(logModule, message);
    }

    @Override
    protected String expectedLogTag() {
        return ERROR_TAG;
    }
}
