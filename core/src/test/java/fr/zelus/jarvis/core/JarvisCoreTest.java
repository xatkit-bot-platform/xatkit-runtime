package fr.zelus.jarvis.core;

import fr.zelus.jarvis.dialogflow.DialogFlowException;
import fr.zelus.jarvis.intent.IntentDefinition;
import fr.zelus.jarvis.intent.IntentFactory;
import fr.zelus.jarvis.module.Action;
import fr.zelus.jarvis.module.Module;
import fr.zelus.jarvis.module.ModuleFactory;
import fr.zelus.jarvis.orchestration.ActionInstance;
import fr.zelus.jarvis.orchestration.OrchestrationFactory;
import fr.zelus.jarvis.orchestration.OrchestrationLink;
import fr.zelus.jarvis.orchestration.OrchestrationModel;
import fr.zelus.jarvis.stubs.StubJarvisModule;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.assertj.core.api.JUnitSoftAssertions;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.*;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class JarvisCoreTest {

    protected static String VALID_PROJECT_ID = "jarvis-fd96e";

    protected static String VALID_LANGUAGE_CODE = "en-US";

    protected static OrchestrationModel VALID_ORCHESTRATION_MODEL;

    protected JarvisCore jarvisCore;

    @BeforeClass
    public static void setUpBeforeClass() throws IOException {
        Module stubModule = ModuleFactory.eINSTANCE.createModule();
        stubModule.setName("StubJarvisModule");
        stubModule.setJarvisModulePath("fr.zelus.jarvis.stubs.StubJarvisModule");
        Action stubAction = ModuleFactory.eINSTANCE.createAction();
        stubAction.setName("StubJarvisAction");
        // No parameters, keep it simple
        stubModule.getActions().add(stubAction);
        IntentDefinition stubIntentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        stubIntentDefinition.setName("Default Welcome Intent");
        // No parameters, keep it simple
        stubModule.getIntentDefinitions().add(stubIntentDefinition);
        VALID_ORCHESTRATION_MODEL = OrchestrationFactory.eINSTANCE.createOrchestrationModel();
        OrchestrationLink link = OrchestrationFactory.eINSTANCE.createOrchestrationLink();
        link.setIntent(stubIntentDefinition);
        ActionInstance actionInstance = OrchestrationFactory.eINSTANCE.createActionInstance();
        actionInstance.setAction(stubAction);
        link.getActions().add(actionInstance);
        VALID_ORCHESTRATION_MODEL.getOrchestrationLinks().add(link);
        /*
         * Create the Resource used to store the valid orchestration model.
         */
        ResourceSet testResourceSet = new ResourceSetImpl();
        testResourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl
                ());

        Resource testIntentResource = testResourceSet.createResource(URI.createURI("/tmp/jarvisTestIntentResource" +
                ".xmi"));
        testIntentResource.getContents().clear();
        testIntentResource.getContents().add(stubModule);
        testIntentResource.save(Collections.emptyMap());

        Resource testOrchestrationResource = testResourceSet.createResource(URI.createURI
                ("/tmp/jarvisTestOrchestrationResource.xmi"));
        testOrchestrationResource.getContents().clear();
        testOrchestrationResource.getContents().add(VALID_ORCHESTRATION_MODEL);
        testOrchestrationResource.save(Collections.emptyMap());
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
        return new JarvisCore(VALID_PROJECT_ID, VALID_LANGUAGE_CODE, VALID_ORCHESTRATION_MODEL);
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        new JarvisCore(null);
    }

    @Test(expected = NullPointerException.class)
    public void constructMissingProjectIdInConfiguration() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JarvisCore.LANGUAGE_CODE_KEY, VALID_LANGUAGE_CODE);
        configuration.addProperty(JarvisCore.ORCHESTRATION_MODEL_KEY, VALID_ORCHESTRATION_MODEL.eResource().getURI());
        new JarvisCore(configuration);
    }

    @Test(expected = NullPointerException.class)
    public void constructMissingLanguageCodeInConfiguration() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JarvisCore.PROJECT_ID_KEY, VALID_PROJECT_ID);
        configuration.addProperty(JarvisCore.ORCHESTRATION_MODEL_KEY, VALID_ORCHESTRATION_MODEL.eResource().getURI());
        new JarvisCore(configuration);
    }

    @Test(expected = NullPointerException.class)
    public void constructMissingOrchestrationPathInConfiguration() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JarvisCore.PROJECT_ID_KEY, VALID_PROJECT_ID);
        configuration.addProperty(JarvisCore.LANGUAGE_CODE_KEY, VALID_LANGUAGE_CODE);
        new JarvisCore(configuration);
    }

    @Test
    public void constructValidConfiguration() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JarvisCore.PROJECT_ID_KEY, VALID_PROJECT_ID);
        configuration.addProperty(JarvisCore.LANGUAGE_CODE_KEY, VALID_LANGUAGE_CODE);
        configuration.addProperty(JarvisCore.ORCHESTRATION_MODEL_KEY, VALID_ORCHESTRATION_MODEL.eResource().getURI());
        JarvisCore jarvisCore = new JarvisCore(configuration);
        checkJarvisCore(jarvisCore);
    }

    @Test(expected = NullPointerException.class)
    public void constructNullProjectId() {
        new JarvisCore(null, VALID_LANGUAGE_CODE, VALID_ORCHESTRATION_MODEL);
    }

    @Test(expected = NullPointerException.class)
    public void constructNullLanguageCode() {
        new JarvisCore(VALID_PROJECT_ID, null, VALID_ORCHESTRATION_MODEL);
    }

    @Test(expected = NullPointerException.class)
    public void constructNullOrchestrationModel() {
        new JarvisCore(VALID_PROJECT_ID, VALID_LANGUAGE_CODE, null);
    }

    @Test
    public void constructValid() {
        JarvisCore jarvisCore = new JarvisCore(VALID_PROJECT_ID, VALID_LANGUAGE_CODE, VALID_ORCHESTRATION_MODEL);
        checkJarvisCore(jarvisCore);
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
        jarvisCore.shutdown();
        softly.assertThat(jarvisCore.getExecutorService().isShutdown()).as("ExecutorService is shutdown");
        softly.assertThat(jarvisCore.getDialogFlowApi().isShutdown()).as("DialogFlow API is shutdown");
        softly.assertThat(jarvisCore.getSessionName()).as("Null DialogFlow session").isNull();
        softly.assertThatThrownBy(() -> JarvisCore.getInstance()).as("Null JarvisCore Instance").isInstanceOf
                (NullPointerException.class).hasMessage("Cannot retrieve the JarvisCore instance, make sure to " +
                "initialize it first");
    }

    @Test(expected = NullPointerException.class)
    public void handleMessageNullMessage() {
        jarvisCore = getValidJarvisCore();
        jarvisCore.handleMessage(null);
    }

    @Test
    public void handleMessageValidMessage() throws InterruptedException {
        jarvisCore = getValidJarvisCore();
        /*
         * It is not necessary to check the the module list is not null and contains at least one element, this is
         * done in loadModule test.
         */
        StubJarvisModule stubJarvisModule = (StubJarvisModule) jarvisCore.getJarvisModuleRegistry().getJarvisModule
                ("StubJarvisModule");
        jarvisCore.handleMessage("hello");
        /*
         * Ask the executor to shutdown an await for the termination of the tasks. This ensures that the action
         * created by the stub module has been executed.
         */
        jarvisCore.getExecutorService().shutdown();
        jarvisCore.getExecutorService().awaitTermination(2, TimeUnit.SECONDS);
        softly.assertThat(stubJarvisModule.getAction().isActionProcessed()).as("Action processed").isTrue();
    }

    @Test(expected = DialogFlowException.class)
    public void handleMessageNotHandledMessage() throws InterruptedException {
        jarvisCore = getValidJarvisCore();
        StubJarvisModule stubJarvisModule = (StubJarvisModule) jarvisCore.getJarvisModuleRegistry().getJarvisModule
                ("StubJarvisModule");
        jarvisCore.handleMessage("bye");
    }

    /**
     * Computes a set of basic assertions on the provided {@code jarvisCore}.
     *
     * @param jarvisCore the {@link JarvisCore} instance to check
     */
    private void checkJarvisCore(JarvisCore jarvisCore) {
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
        softly.assertThat(jarvisCore.isShutdown()).as("Not shutdown").isFalse();
    }

}
