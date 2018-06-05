package fr.zelus.jarvis.core;

import fr.zelus.jarvis.stubs.StubJarvisModule;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class JarvisCoreTest {

    protected static String VALID_PROJECT_ID = "jarvis-fd96e";

    protected static String VALID_LANGUAGE_CODE = "en-US";

    protected JarvisCore jarvisCore;

    @After
    public void tearDown() {
        if(nonNull(jarvisCore)) {
            jarvisCore.shutdown();
        }
    }

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test(expected = NullPointerException.class)
    public void constructNullProjectId() {
        new JarvisCore(null, VALID_LANGUAGE_CODE, Collections.emptyList());
    }

    @Test(expected = NullPointerException.class)
    public void constructNullLanguageCode() {
        new JarvisCore(VALID_PROJECT_ID, null, Collections.emptyList());
    }

    @Test(expected = NullPointerException.class)
    public void constructNullModuleList() {
        new JarvisCore(VALID_PROJECT_ID, VALID_LANGUAGE_CODE, null);
    }

    @Test
    public void constructValidNoModuleList() {
        jarvisCore = new JarvisCore(VALID_PROJECT_ID, VALID_LANGUAGE_CODE);
        checkJarvisCoreDialogFlowFields(jarvisCore);
        softly.assertThat(jarvisCore.getModules()).as("Empty module list").isEmpty();
    }

    @Test
    public void constructValidNotEmptyModuleList() {
        List<JarvisModule> modules = new ArrayList<>();
        JarvisModule stubJarvisModule = new StubJarvisModule();
        modules.add(stubJarvisModule);
        jarvisCore = new JarvisCore(VALID_PROJECT_ID, VALID_LANGUAGE_CODE, modules);
        checkJarvisCoreDialogFlowFields(jarvisCore);
        softly.assertThat(jarvisCore.getModules()).as("Not empty module list").isNotEmpty();
        softly.assertThat(jarvisCore.getModules()).as("Module list contains input module").contains(stubJarvisModule);
    }

    @Test(expected = NullPointerException.class)
    public void registerNullModule() {
        jarvisCore = new JarvisCore(VALID_PROJECT_ID, VALID_LANGUAGE_CODE);
        jarvisCore.registerModule(null);
    }

    @Test
    public void registerModule() {
        JarvisModule stubJarvisModule = new StubJarvisModule();
        jarvisCore = new JarvisCore(VALID_PROJECT_ID, VALID_LANGUAGE_CODE);
        jarvisCore.registerModule(stubJarvisModule);
        // Don't check whether getModules() is null, it is done in constructor-related tests.
        assertThat(jarvisCore.getModules()).as("Module list contains input module").contains(stubJarvisModule);
    }

    @Test(expected = NullPointerException.class)
    public void unregisterNullModule() {
        jarvisCore = new JarvisCore(VALID_PROJECT_ID, VALID_LANGUAGE_CODE);
        jarvisCore.unregisterModule(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unregisterNotRegisteredModule() {
        JarvisModule stubJarvisModule = new StubJarvisModule();
        jarvisCore = new JarvisCore(VALID_PROJECT_ID, VALID_LANGUAGE_CODE);
        jarvisCore.unregisterModule(stubJarvisModule);
    }

    @Test
    public void unregisterRegisteredModule() {
        JarvisModule stubJarvisModule = new StubJarvisModule();
        jarvisCore = new JarvisCore(VALID_PROJECT_ID, VALID_LANGUAGE_CODE);
        jarvisCore.registerModule(stubJarvisModule);
        jarvisCore.unregisterModule(stubJarvisModule);
        assertThat(jarvisCore.getModules()).as("Module list does not contain input module").doesNotContain
                (stubJarvisModule);
    }

    @Test
    public void clearEmptyModuleList() {
        jarvisCore = new JarvisCore(VALID_PROJECT_ID, VALID_LANGUAGE_CODE);
        jarvisCore.clearModules();
        assertThat(jarvisCore.getModules()).as("Empty module list").isEmpty();
    }

    @Test
    public void clearNotEmptyModuleList() {
        JarvisModule stubJarvisModule = new StubJarvisModule();
        jarvisCore = new JarvisCore(VALID_PROJECT_ID, VALID_LANGUAGE_CODE);
        jarvisCore.registerModule(stubJarvisModule);
        jarvisCore.clearModules();
        assertThat(jarvisCore.getModules()).as("Empty module list").isEmpty();
    }

    /**
     * Computes a set of basic assertions on the provided {@code jarvisCore}.
     *
     * @param jarvisCore the {@link JarvisCore} instance to check
     */
    private void checkJarvisCoreDialogFlowFields(JarvisCore jarvisCore) {
        /*
         * isNotNull() assertions are not soft, otherwise the runner does not print the assertion error and fails on
         * a NullPointerException in the following assertions.
         */
        assertThat(jarvisCore.getDialogFlowApi()).as("Not null DialogFlow API").isNotNull();
        softly.assertThat(jarvisCore.getDialogFlowApi().getProjectId()).as("Valid DialogFlowAPI project ID").isEqualTo
                (VALID_PROJECT_ID);
        softly.assertThat(jarvisCore.getDialogFlowApi().getLanguageCode()).as("Valid DialogFlowAPI language code")
                .isEqualTo(VALID_LANGUAGE_CODE);
        assertThat(jarvisCore.getSessionName()).as("Not null SessionName").isNotNull();
        softly.assertThat(jarvisCore.getSessionName().getProject()).as("Valid SessionName project ID").isEqualTo
                (VALID_PROJECT_ID);
        assertThat(jarvisCore.getModules()).as("Not null module list").isNotNull();
    }

}
