package fr.zelus.jarvis.core.module.log.action;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class LogActionTest<T extends LogAction> {

    protected static String VALID_MESSAGE = "test";

    protected ByteArrayOutputStream outContent;

    protected PrintStream outPrintStream;

    @Before
    public void setUp() {
        outContent = new ByteArrayOutputStream();
        outPrintStream = new PrintStream(outContent);
    }

    @After
    public void tearDown() {
        System.setOut(System.out);
    }

    protected abstract T createLogAction(String message);

    protected abstract String expectedLogTag();

    @Test(expected = NullPointerException.class)
    public void constructLogActionNullMessage() {
        createLogAction(null);
    }

    @Test
    public void constructLogActionValidMessage() {
        LogAction logAction = createLogAction(VALID_MESSAGE);
        assertThat(logAction.getMessage()).as("Not null message").isNotNull();
        assertThat(logAction.getMessage()).as("Valid message").isEqualTo(VALID_MESSAGE);
    }

    @Test
    public void runValidLogAction() throws IOException, InterruptedException {
        System.setOut(outPrintStream);
        LogAction logAction = createLogAction(VALID_MESSAGE);
        logAction.run();
        /*
         * The underlying logger is asynchronous, wait to ensure that the message has been processed at the logger
         * level.
         */
        Thread.sleep(100);
        /**
         * Read from the ByteArrayOutputStream used to print logs.
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(outContent
                .toByteArray())));
        String line = reader.readLine();
        /*
         * Reset the default System.out to enable assertion error print.
         */
        System.setOut(System.out);
        assertThat(line).as(expectedLogTag() + " tag").contains(expectedLogTag());
        assertThat(line).as("Action message in log").contains(VALID_MESSAGE);
    }

}
