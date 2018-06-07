package fr.zelus.jarvis.core;

import fr.zelus.jarvis.module.Module;
import fr.zelus.jarvis.module.ModuleFactory;
import fr.zelus.jarvis.stubs.StubJarvisModule;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class JarvisCoreTest {

    protected static String VALID_PROJECT_ID = "jarvis-fd96e";

    protected static String VALID_LANGUAGE_CODE = "en-US";

    protected static Module VALID_MODULE;

    protected static List<Module> VALID_MODULE_LIST;

    protected JarvisCore jarvisCore;

    @BeforeClass
    public static void setUpBeforeClass() {
        VALID_MODULE = ModuleFactory.eINSTANCE.createModule();
        VALID_MODULE.setName("StubJarvisModule");
        VALID_MODULE.setJarvisModulePath("fr.zelus.jarvis.stubs.StubJarvisModule");
        VALID_MODULE_LIST = new ArrayList<>();
        VALID_MODULE_LIST.add(VALID_MODULE);
    }

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {
        if (nonNull(jarvisCore) && !jarvisCore.isShutdown()) {
            jarvisCore.shutdown();
        }
    }

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    /**
     * Returns a valid {@link JarvisCore} instance.
     *
     * @return a valid {@link JarvisCore} instance
     */
    private static JarvisCore getValidJarvisCore() {
        return new JarvisCore(VALID_PROJECT_ID, VALID_LANGUAGE_CODE);
    }

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
        jarvisCore = new JarvisCore(VALID_PROJECT_ID, VALID_LANGUAGE_CODE, VALID_MODULE_LIST);
        checkJarvisCoreDialogFlowFields(jarvisCore);
        softly.assertThat(jarvisCore.getModules()).as("Module list size is 1").hasSize(1);
        softly.assertThat(jarvisCore.getModules().get(0)).as("Module list contains the loaded StubJarvisModule")
                .isInstanceOf(StubJarvisModule.class);
    }

    @Test(expected = NullPointerException.class)
    public void loadNullModule() {
        jarvisCore = getValidJarvisCore();
        jarvisCore.loadModule(null);
    }

    @Test
    public void loadModule() {
        jarvisCore = getValidJarvisCore();
        jarvisCore.loadModule(VALID_MODULE);
        // Don't check whether getModules() is null, it is done in constructor-related tests.
        softly.assertThat(jarvisCore.getModules()).as("Module list size is 1").hasSize(1);
        softly.assertThat(jarvisCore.getModules().get(0)).as("Module list contains the loaded StubJarvisModule")
                .isInstanceOf(StubJarvisModule.class);
    }

    @Test(expected = NullPointerException.class)
    public void unloadNullModule() {
        jarvisCore = getValidJarvisCore();
        jarvisCore.unloadModule(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unloadNotRegisteredModule() {
        jarvisCore = getValidJarvisCore();
        jarvisCore.unloadModule(VALID_MODULE);
    }

    @Test
    public void unloadRegisteredModule() {
        jarvisCore = getValidJarvisCore();
        jarvisCore.loadModule(VALID_MODULE);
        jarvisCore.unloadModule(VALID_MODULE);
        assertThat(jarvisCore.getModules()).as("Module list is empty").isEmpty();
    }

    @Test
    public void clearEmptyModuleList() {
        jarvisCore = getValidJarvisCore();
        jarvisCore.clearModules();
        assertThat(jarvisCore.getModules()).as("Empty module list").isEmpty();
    }

    @Test
    public void clearNotEmptyModuleList() {
        jarvisCore = getValidJarvisCore();
        jarvisCore.loadModule(VALID_MODULE);
        jarvisCore.clearModules();
        assertThat(jarvisCore.getModules()).as("Module list is empty").isEmpty();
    }

    @Test(expected = JarvisException.class)
    public void shutdownAlreadyShutdown() {
        jarvisCore = getValidJarvisCore();
        jarvisCore.shutdown();
        jarvisCore.shutdown();
    }

    @Test
    public void shutdown() {
        jarvisCore = getValidJarvisCore();
        // Register a module to check that the module list has been cleaned after shutdown.
        jarvisCore.loadModule(VALID_MODULE);
        jarvisCore.shutdown();
        softly.assertThat(jarvisCore.getExecutorService().isShutdown()).as("ExecutorService is shutdown");
        softly.assertThat(jarvisCore.getDialogFlowApi().isShutdown()).as("DialogFlow API is shutdown");
        softly.assertThat(jarvisCore.getSessionName()).as("Null DialogFlow session").isNull();
        softly.assertThat(jarvisCore.getModules()).as("Empty module list").isEmpty();
    }

    @Test(expected = NullPointerException.class)
    public void handleMessageNullMessage() {
        jarvisCore = getValidJarvisCore();
        jarvisCore.handleMessage(null);
    }

    @Test
    public void handleMessageValidMessage() throws InterruptedException {
        jarvisCore = getValidJarvisCore();
        jarvisCore.loadModule(VALID_MODULE);
        /*
         * It is not necessary to check the the module list is not null and contains at least one element, this is
         * done in loadModule test.
         */
        StubJarvisModule stubJarvisModule = (StubJarvisModule) jarvisCore.getModules().get(0);
        jarvisCore.handleMessage("hello");
        /*
         * Ask the executor to shutdown an await for the termination of the tasks. This ensures that the action
         * created by the stub module has been executed.
         */
        jarvisCore.getExecutorService().shutdown();
        jarvisCore.getExecutorService().awaitTermination(2, TimeUnit.SECONDS);
        softly.assertThat(stubJarvisModule.isIntentHandled()).as("Intent handled").isTrue();
        softly.assertThat(stubJarvisModule.isActionProcessed()).as("Action processed").isTrue();
    }

    @Test
    public void handleMessageNotHandledMessage() throws InterruptedException {
        StubJarvisModule stubJarvisModule = new StubJarvisModule();
        jarvisCore = getValidJarvisCore();
        jarvisCore.loadModule(VALID_MODULE);
        jarvisCore.handleMessage("bye");
        /*
         * Ask the executor to shutdown an await for the termination of the tasks. This ensures that any action
         * created by the stub module has been executed.
         */
        jarvisCore.getExecutorService().shutdown();
        jarvisCore.getExecutorService().awaitTermination(2, TimeUnit.SECONDS);
        softly.assertThat(stubJarvisModule.isIntentHandled()).as("Intent not handled").isFalse();
        softly.assertThat(stubJarvisModule.isActionProcessed()).as("Action not processed").isFalse();
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
        softly.assertThat(jarvisCore.isShutdown()).as("Not shutdown").isFalse();
    }

}
