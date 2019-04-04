package edu.uoc.som.jarvis;

import edu.uoc.som.jarvis.core.JarvisException;
import org.junit.After;
import org.junit.Test;

import java.io.File;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JarvisTest extends AbstractJarvisTest {

    private static String EXECUTION_MODEL_LOADING_MESSAGE = "Cannot retrieve the ExecutionModel from the property " +
            "null, please ensure it is set in the jarvis.execution.model property of the jarvis configuration";

    @After
    public void tearDown() {
        if (nonNull(Jarvis.getJarvisCore()) && !Jarvis.getJarvisCore().isShutdown()) {
            Jarvis.getJarvisCore().shutdown();
        }
    }

    @Test(expected = NullPointerException.class)
    public void mainNullArgs() {
        Jarvis.main(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mainEmptyArgs() {
        Jarvis.main(new String[0]);
    }

    @Test(expected = JarvisException.class)
    public void mainInvalidConfigurationPath() {
        String[] args = {"/test"};
        Jarvis.main(args);
    }

    @Test
    public void mainValidRelativePath() {
        String[] args = {"src/test/resources/empty-configuration.properties"};
        assertThatThrownBy(() -> Jarvis.main(args)).isInstanceOf(NullPointerException.class)
                .hasMessage(EXECUTION_MODEL_LOADING_MESSAGE);
    }

    @Test
    public void mainValidRelativePathFolderPrefix() {
        String[] args = {"./src/test/resources/empty-configuration.properties"};
        assertThatThrownBy(() -> Jarvis.main(args)).isInstanceOf(NullPointerException.class)
                .hasMessage(EXECUTION_MODEL_LOADING_MESSAGE);
    }

    @Test
    public void mainValidAbsolutePath() {
        File f = new File("/src/test/resources/empty-configuration.properties");
        assertThatThrownBy(() -> Jarvis.main(new String[]{f.getAbsolutePath()})).isInstanceOf(NullPointerException.class)
                .hasMessage(EXECUTION_MODEL_LOADING_MESSAGE);
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
