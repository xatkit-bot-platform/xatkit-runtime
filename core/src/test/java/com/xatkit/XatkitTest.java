package com.xatkit;

import com.xatkit.util.XatkitEnvironmentConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.junit.After;
import org.junit.Test;

import java.io.File;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class XatkitTest extends AbstractXatkitTest {

    @After
    public void tearDown() {
        if (nonNull(Xatkit.getXatkitCore()) && !Xatkit.getXatkitCore().isShutdown()) {
            Xatkit.getXatkitCore().shutdown();
        }
    }

    @Test
    public void mainNullArgs() {
        Xatkit.main(null);
        assertThat(Xatkit.getConfiguration()).as("Using XatkitEnvironmentConfiguration")
                .isInstanceOf(XatkitEnvironmentConfiguration.class);
    }

    @Test
    public void mainEmptyArgs() {
        Xatkit.main(new String[0]);
        assertThat(Xatkit.getConfiguration()).as("Using XatkitEnvironmentConfiguration")
                .isInstanceOf(XatkitEnvironmentConfiguration.class);
    }

    @Test
    public void mainInvalidConfigurationPath() {
        Xatkit.main(new String[]{"/test"});
        assertThat(Xatkit.getConfiguration()).as("Configuration not loaded").isNull();
    }

    @Test
    public void mainValidRelativePath() {
        Xatkit.main(new String[]{"src/test/resources/empty-configuration.properties"});
        assertThat(Xatkit.getConfiguration()).as("Configuration loaded").isInstanceOf(PropertiesConfiguration.class);
    }

    @Test
    public void mainValidRelativePathFolderPrefix() {
        Xatkit.main(new String[]{"./src/test/resources/empty-configuration.properties"});
        assertThat(Xatkit.getConfiguration()).as("Configuration loaded").isInstanceOf(PropertiesConfiguration.class);
    }

    @Test
    public void mainValidAbsolutePath() {
        File f = new File("/src/test/resources/empty-configuration.properties");
        Xatkit.main(new String[]{f.getAbsolutePath()});
        assertThat(Xatkit.getConfiguration()).as("Configuration loaded").isInstanceOf(PropertiesConfiguration.class);
    }

    /*
     * We cannot properly test relative paths such as ./configuration.properties, the tests are executed with xatkit/
     * as their base folder. We need to find a way to test it without polluting the repository.
     */

    /*
     * Do not test that the created XatkitCore instance corresponds to the provided Configuration: once the
     * Configuration is loaded the construction of the XatkitCore instance is similar to the ones tested in
     * XatkitCoreTest.
     */
}
