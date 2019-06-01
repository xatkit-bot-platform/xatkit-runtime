package com.xatkit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class XatkitTest extends AbstractJarvisTest {

    private static String EXECUTION_MODEL_LOADING_MESSAGE = "Cannot retrieve the ExecutionModel from the property " +
            "null, please ensure it is set in the jarvis.execution.model property of the jarvis configuration";

    private static final String ERROR_PREFIX = "[ERROR]";

    protected ListAppender listAppender;

    @Before
    public void setUp() throws InterruptedException {
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        listAppender = loggerContext.getConfiguration().getAppender("List");
        /*
         * Clear before the test, this logger is used by all the test cases and may contain messages before the first
         * test of this class. We also need to wait in case some messages are pending in the logger.
         */
        Thread.sleep(200);
        listAppender.clear();
    }

    @After
    public void tearDown() {
        listAppender.clear();
        if (nonNull(Xatkit.getJarvisCore()) && !Xatkit.getJarvisCore().isShutdown()) {
            Xatkit.getJarvisCore().shutdown();
        }
    }

    @Test
    public void mainNullArgs() {
        Xatkit.main(null);
        assertThat(getLastMessage()).as("Logger message is an error").contains(ERROR_PREFIX);
        /*
         * Do not check the content, it may change later and the important point is to ensure we log an error
         */
    }

    @Test
    public void mainEmptyArgs() {
        Xatkit.main(new String[0]);
        assertThat(getLastMessage()).as("Logger message is an error").contains(ERROR_PREFIX);
    }

    @Test
    public void mainInvalidConfigurationPath() {
        String[] args = {"/test"};
        Xatkit.main(args);
        assertThat(getLastMessage()).as("Logger message is an error").contains(ERROR_PREFIX);

    }

    @Test
    public void mainValidRelativePath() {
        String[] args = {"src/test/resources/empty-configuration.properties"};
        assertThatThrownBy(() -> Xatkit.main(args)).isInstanceOf(NullPointerException.class)
                .hasMessage(EXECUTION_MODEL_LOADING_MESSAGE);
    }

    @Test
    public void mainValidRelativePathFolderPrefix() {
        String[] args = {"./src/test/resources/empty-configuration.properties"};
        assertThatThrownBy(() -> Xatkit.main(args)).isInstanceOf(NullPointerException.class)
                .hasMessage(EXECUTION_MODEL_LOADING_MESSAGE);
    }

    @Test
    public void mainValidAbsolutePath() {
        File f = new File("/src/test/resources/empty-configuration.properties");
        assertThatThrownBy(() -> Xatkit.main(new String[]{f.getAbsolutePath()})).isInstanceOf(NullPointerException.class)
                .hasMessage(EXECUTION_MODEL_LOADING_MESSAGE);
    }

    private String getLastMessage() {
        /*
         * Wait before looking at the messages, they are asynchronously sent to the logger.
         */
        try {
            Thread.sleep(200);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
        return listAppender.getMessages().get(listAppender.getMessages().size() - 1);
    }

    /*
     * We cannot properly test relative paths such as ./configuration.properties, the tests are executed with jarvis/
     * as their base folder. We need to find a way to test it without polluting the repository.
     */

    /*
     * Do not test that the created JarvisCore instance corresponds to the provided Configuration: once the
     * Configuration is loaded the construction of the JarvisCore instance is similar to the ones tested in
     * JarvisCoreTest.
     */
}
