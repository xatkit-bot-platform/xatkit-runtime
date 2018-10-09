package fr.zelus.jarvis;

import fr.zelus.jarvis.core.JarvisException;
import org.junit.After;
import org.junit.Test;

import static java.util.Objects.nonNull;

public class JarvisTest extends AbstractJarvisTest {

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

    /*
     * Do not test that the created JarvisCore instance corresponds to the provided Configuration: once the
     * Configuration is loaded the construction of the JarvisCore instance is similar to the ones tested in
     * JarvisCoreTest.
     */
}
