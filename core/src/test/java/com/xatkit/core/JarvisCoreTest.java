package edu.uoc.som.jarvis.core;

import edu.uoc.som.jarvis.AbstractJarvisTest;
import edu.uoc.som.jarvis.core.recognition.DefaultIntentRecognitionProvider;
import edu.uoc.som.jarvis.core.recognition.IntentRecognitionProviderFactory;
import edu.uoc.som.jarvis.core.recognition.dialogflow.DialogFlowApi;
import edu.uoc.som.jarvis.core.recognition.dialogflow.DialogFlowApiTest;
import edu.uoc.som.jarvis.core.session.JarvisSession;
import edu.uoc.som.jarvis.core_resources.utils.LibraryLoaderUtils;
import edu.uoc.som.jarvis.core_resources.utils.PlatformLoaderUtils;
import edu.uoc.som.jarvis.execution.ExecutionFactory;
import edu.uoc.som.jarvis.execution.ExecutionModel;
import edu.uoc.som.jarvis.platform.EventProviderDefinition;
import edu.uoc.som.jarvis.platform.PlatformDefinition;
import edu.uoc.som.jarvis.platform.PlatformFactory;
import edu.uoc.som.jarvis.stubs.io.StubJsonWebhookEventProvider;
import edu.uoc.som.jarvis.test.util.models.TestExecutionModel;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.JUnitSoftAssertions;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class JarvisCoreTest extends AbstractJarvisTest {

    protected static ExecutionModel VALID_EXECUTION_MODEL;

    @BeforeClass
    public static void setUpBeforeClass() throws IOException {
        TestExecutionModel testExecutionModel = new TestExecutionModel();
        VALID_EXECUTION_MODEL = testExecutionModel.getExecutionModel();
        /*
         * Create the Resource used to store the valid execution model.
         */
        ResourceSet testResourceSet = new ResourceSetImpl();
        testResourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl
                ());

        Resource testIntentResource = testResourceSet.createResource(URI.createURI("/tmp/jarvisTestIntentResource" +
                ".xmi"));
        testIntentResource.getContents().clear();
        testIntentResource.getContents().add(testExecutionModel.getTestIntentModel().getIntentLibrary());
        testIntentResource.save(Collections.emptyMap());

        Resource testPlatformResource = testResourceSet.createResource(URI.createURI("/tmp/jarvisTestPlatformResource" +
                ".xmi"));
        testPlatformResource.getContents().clear();
        testPlatformResource.getContents().add(testExecutionModel.getTestPlatformModel().getPlatformDefinition());
        testPlatformResource.save(Collections.emptyMap());

        Resource testExecutionResource = testResourceSet.createResource(URI.createURI
                ("/tmp/jarvisTestExecutionResource.xmi"));
        testExecutionResource.getContents().clear();
        testExecutionResource.getContents().add(VALID_EXECUTION_MODEL);
        testExecutionResource.save(Collections.emptyMap());
    }

    protected JarvisCore jarvisCore;

    public static Configuration buildConfiguration() {
        return buildConfiguration(VALID_EXECUTION_MODEL);
    }

    public static Configuration buildConfiguration(Object executionModel) {
        Configuration configuration = DialogFlowApiTest.buildConfiguration();
        configuration.addProperty(JarvisCore.EXECUTION_MODEL_KEY, executionModel);
        configuration.addProperty(IntentRecognitionProviderFactory.ENABLE_RECOGNITION_ANALYTICS, false);
        return configuration;
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
     * <p>
     * The returned {@link JarvisCore} instance contains an empty {@link ExecutionModel}, see
     * {@link ExecutionServiceTest} for execution-related test cases.
     *
     * @return a valid {@link JarvisCore} instance
     */
    private JarvisCore getValidJarvisCore() {
        Configuration configuration = buildConfiguration();
        jarvisCore = new JarvisCore(configuration);
        return jarvisCore;
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        jarvisCore = new JarvisCore(null);
    }

    @Test(expected = NullPointerException.class)
    public void constructMissingExecutionPathInConfiguration() {
        Configuration configuration = new BaseConfiguration();
        jarvisCore = new JarvisCore(configuration);
    }

    @Test(expected = JarvisException.class)
    public void constructInvalidCustomPlatformPathInConfiguration() {
        Configuration configuration = buildConfiguration();
        configuration.addProperty(JarvisCore.CUSTOM_PLATFORMS_KEY_PREFIX + "Example", "test");
        jarvisCore = new JarvisCore(configuration);
    }

    @Test
    public void constructValidCustomPlatformPathInConfiguration() {
        Configuration configuration = buildConfiguration();
        File validFile = new File(this.getClass().getClassLoader().getResource("Test_Platforms/ExamplePlatform.xmi")
                .getFile());
        configuration.addProperty(JarvisCore.CUSTOM_PLATFORMS_KEY_PREFIX + "Example", validFile.getAbsolutePath());
        jarvisCore = new JarvisCore(configuration);
        checkJarvisCore(jarvisCore);
        URI expectedURI = URI.createFileURI(validFile.getAbsolutePath());
        List<URI> registeredResourceURIs = jarvisCore.executionResourceSet.getResources().stream().map(r -> r.getURI
                ()).collect(Collectors.toList());
        assertThat(registeredResourceURIs).as("Custom runtimePlatform URI contained in the registered resource URIs")
                .contains(expectedURI);
        URI expectedPathmapURI = URI.createURI(PlatformLoaderUtils.CUSTOM_PLATFORM_PATHMAP + "Example");
        assertThat(jarvisCore.executionResourceSet.getURIConverter().getURIMap().keySet()).as("Custom runtimePlatform" +
                "pathmap contained in the ResourceSet's URI map").contains(expectedPathmapURI);
        assertThat(jarvisCore.executionResourceSet.getURIConverter().getURIMap().get(expectedPathmapURI)).as
                ("Valid concrete URI associated to the registered pathmap URI").isEqualTo(expectedURI);
    }

    @Test(expected = JarvisException.class)
    public void constructInvalidCustomLibraryPathInConfiguration() {
        Configuration configuration = buildConfiguration();
        configuration.addProperty(JarvisCore.CUSTOM_LIBRARIES_KEY_PREFIX + "Example", "test");
        jarvisCore = new JarvisCore(configuration);
    }

    @Test
    public void constructValidCustomPlatformFromPathInConfiguration() {
        Configuration configuration = buildConfiguration();
        File validFile = new File(this.getClass().getClassLoader().getResource("Test_Libraries/ExampleLibrary.xmi")
                .getFile());
        configuration.addProperty(JarvisCore.CUSTOM_LIBRARIES_KEY_PREFIX + "Example", validFile.getAbsolutePath());
        jarvisCore = new JarvisCore(configuration);
        checkJarvisCore(jarvisCore);
        URI expectedURI = URI.createFileURI(validFile.getAbsolutePath());
        List<URI> registeredResourceURIs = jarvisCore.executionResourceSet.getResources().stream().map(r -> r.getURI
                ()).collect(Collectors.toList());
        assertThat(registeredResourceURIs).as("Custom library URI contained in the registered resource URIs")
                .contains(expectedURI);
        URI expectedPathmapURI = URI.createURI(LibraryLoaderUtils.CUSTOM_LIBRARY_PATHMAP + "Example");
        assertThat(jarvisCore.executionResourceSet.getURIConverter().getURIMap().keySet()).as("Custom library pathmap" +
                " contained in the ResourceSet's URI map").contains(expectedPathmapURI);
        assertThat(jarvisCore.executionResourceSet.getURIConverter().getURIMap().get(expectedPathmapURI)).as("Valid " +
                "concrete URI associated to the registered pathmap URI").isEqualTo(expectedURI);
    }

    @Test(expected = JarvisException.class)
    public void constructInvalidPlatformFromExecutionModel() {
        TestExecutionModel testExecutionModel = new TestExecutionModel();
        ExecutionModel executionModel = testExecutionModel.getExecutionModel();
        PlatformDefinition platformDefinition = testExecutionModel.getTestPlatformModel().getPlatformDefinition();
        platformDefinition.setName("InvalidPlatform");
        platformDefinition.setRuntimePath("edu.uoc.som.jarvis.stubs.InvalidPlatform");
        Configuration configuration = buildConfiguration(executionModel);
        jarvisCore = new JarvisCore(configuration);
    }

    @Test
    public void constructValidConfiguration() {
        Configuration configuration = buildConfiguration();
        jarvisCore = new JarvisCore(configuration);
        checkJarvisCore(jarvisCore);
    }

    @Test
    public void constructValidDefaultRuntimePlatformConstructor() {
        /*
         * Use another ExecutionModel linking to the StubRuntimePlatformJarvisCoreConstructor stub class, that only
         * defines a default constructor.
         */
        TestExecutionModel testExecutionModel = new TestExecutionModel();
        PlatformDefinition platformDefinition = testExecutionModel.getTestPlatformModel().getPlatformDefinition();
        platformDefinition.setName("StubRuntimePlatformJarvisCoreConstructor");
        platformDefinition.setRuntimePath("edu.uoc.som.jarvis.stubs.StubRuntimePlatformJarvisCoreConstructor");

        ExecutionModel executionModel = testExecutionModel.getExecutionModel();

        jarvisCore = new JarvisCore(buildConfiguration(executionModel));
        checkJarvisCore(jarvisCore, executionModel);
    }

    @Test
    public void constructValidWebhookEventProvider() {
        PlatformDefinition stubPlatformDefinition = PlatformFactory.eINSTANCE.createPlatformDefinition();
        stubPlatformDefinition.setName("EmptyRuntimePlatform");
        stubPlatformDefinition.setRuntimePath("edu.uoc.som.jarvis.stubs.EmptyRuntimePlatform");
        EventProviderDefinition stubWebhookEventProviderDefinition = PlatformFactory.eINSTANCE
                .createEventProviderDefinition();
        stubWebhookEventProviderDefinition.setName("StubJsonWebhookEventProvider");
        stubPlatformDefinition.getEventProviderDefinitions().add(stubWebhookEventProviderDefinition);
        ExecutionModel executionModel = ExecutionFactory.eINSTANCE.createExecutionModel();
        executionModel.getEventProviderDefinitions().add(stubWebhookEventProviderDefinition);
        jarvisCore = new JarvisCore(buildConfiguration(executionModel));
        checkJarvisCore(jarvisCore, executionModel);
        Assertions.assertThat(jarvisCore.getJarvisServer().getRegisteredWebhookEventProviders()).as("Server " +
                "WebhookEventProvider" +
                " collection is not empty").isNotEmpty();
        Assertions.assertThat(jarvisCore.getJarvisServer().getRegisteredWebhookEventProviders().iterator().next()).as("Valid " +
                "registered WebhookEventProvider").isInstanceOf(StubJsonWebhookEventProvider.class);
    }

    @Test
    public void constructDefaultIntentRecognitionProviderEmptyExecutionModel() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JarvisCore.EXECUTION_MODEL_KEY, ExecutionFactory.eINSTANCE.createExecutionModel());
        jarvisCore = new JarvisCore(configuration);
        Assertions.assertThat(jarvisCore.getIntentRecognitionProvider()).as("JarvisCore uses " +
                "DefaultIntentRecognitionProvider")
                .isInstanceOf(DefaultIntentRecognitionProvider.class);
    }

    @Test
    public void constructDefaultIntentRecognitionProviderIntentDefinitionInExecutionModel() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JarvisCore.EXECUTION_MODEL_KEY, VALID_EXECUTION_MODEL);
        jarvisCore = new JarvisCore(configuration);
        assertThat(jarvisCore.getIntentRecognitionProvider()).as("JarvisCore uses " +
                "DefaultIntentRecognitionProvider")
                .isInstanceOf(DefaultIntentRecognitionProvider.class);
    }

    @Test(expected = JarvisException.class)
    public void getExecutionModelInvalidType() {
        jarvisCore = getValidJarvisCore();
        ExecutionModel executionModel = jarvisCore.getExecutionModel(buildExecutionModelConfiguration(new Integer(2)));
    }

    @Test(expected = JarvisException.class)
    public void getExecutionModelFromInvalidString() {
        jarvisCore = getValidJarvisCore();
        ExecutionModel executionModel = jarvisCore.getExecutionModel(buildExecutionModelConfiguration("/tmp/test.xmi"));
    }

    @Test(expected = JarvisException.class)
    public void getExecutionModelFromInvalidURI() {
        jarvisCore = getValidJarvisCore();
        ExecutionModel executionModel = jarvisCore.getExecutionModel(buildExecutionModelConfiguration(URI.createURI(
                "/tmp/test.xmi")));
    }

    @Test
    public void getExecutionModelFromValidInMemory() {
        jarvisCore = getValidJarvisCore();
        ExecutionModel executionModel =
                jarvisCore.getExecutionModel(buildExecutionModelConfiguration(VALID_EXECUTION_MODEL));
        assertThat(executionModel).as("Valid ExecutionModel").isEqualTo(VALID_EXECUTION_MODEL);
    }

    @Test
    public void getExecutionModelFromValidString() {
        jarvisCore = getValidJarvisCore();
        ExecutionModel executionModel =
                jarvisCore.getExecutionModel(buildExecutionModelConfiguration(VALID_EXECUTION_MODEL.eResource().getURI()
                        .toString()));
        assertThat(executionModel).as("Not null ExecutionModel").isNotNull();
        /*
         * Not enough, but comparing the entire content of the model is more complicated than it looks like.
         */
        assertThat(executionModel.getExecutionRules()).as("Valid ExecutionRule size").hasSize
                (VALID_EXECUTION_MODEL.getExecutionRules().size());
    }

    @Test
    public void getExecutionModelFromValidURI() {
        jarvisCore = getValidJarvisCore();
        ExecutionModel executionModel =
                jarvisCore.getExecutionModel(buildExecutionModelConfiguration(VALID_EXECUTION_MODEL.eResource
                        ().getURI()));
        assertThat(executionModel).as("Not null ExecutionModel").isNotNull();
        /*
         * Not enough, but comparing the entire content of the model is more complicated than it looks like.
         */
        assertThat(executionModel.getExecutionRules()).as("Valid ExecutionRule size").hasSize
                (VALID_EXECUTION_MODEL.getExecutionRules().size());
    }

    private Configuration buildExecutionModelConfiguration(Object value) {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JarvisCore.EXECUTION_MODEL_KEY, value);
        return configuration;
    }

    @Test(expected = JarvisException.class)
    public void shutdownAlreadyShutdown() {
        jarvisCore = getValidJarvisCore();
        jarvisCore.shutdown();
        jarvisCore.shutdown();
    }

    @Test(expected = NullPointerException.class)
    public void getOrCreateJarvisSessionNullSessionId() {
        jarvisCore = getValidJarvisCore();
        jarvisCore.getOrCreateJarvisSession(null);
    }

    @Test
    public void getOrCreateJarvisSessionValidSessionId() {
        jarvisCore = getValidJarvisCore();
        JarvisSession session = jarvisCore.getOrCreateJarvisSession("sessionID");
        assertThat(session).as("Not null JarvisSession").isNotNull();
        /*
         * Use contains because the underlying DialogFlow API add additional identification information in the
         * returned JarvisSession.
         */
        assertThat(session.getSessionId()).as("Valid session ID").contains("sessionID");
        assertThat(session.getRuntimeContexts()).as("Not null session context").isNotNull();
        assertThat(session.getRuntimeContexts().getContextMap()).as("Empty session context").isEmpty();
    }

    @Test
    public void shutdown() {
        jarvisCore = getValidJarvisCore();
        jarvisCore.shutdown();
        softly.assertThat(jarvisCore.getExecutionService().isShutdown()).as("ExecutorService is shutdown");
        softly.assertThat(jarvisCore.getIntentRecognitionProvider().isShutdown()).as("DialogFlow API is shutdown");
        softly.assertThat(jarvisCore.getRuntimePlatformRegistry().getRuntimePlatforms()).as("Empty runtimePlatform " +
                "registry").isEmpty();
    }

    /**
     * Computes a set of basic assertions on the provided {@code jarvisCore} using the
     * {@link #VALID_EXECUTION_MODEL}.
     *
     * @param jarvisCore the {@link JarvisCore} instance to check
     */
    private void checkJarvisCore(JarvisCore jarvisCore) {
        checkJarvisCore(jarvisCore, VALID_EXECUTION_MODEL);
    }

    /**
     * Computes a set of basic assertions on the provided {@code jarvisCore} using the provided {@code
     * executionModel}.
     *
     * @param jarvisCore     the {@link JarvisCore} instance to check
     * @param executionModel the {@link ExecutionModel} to check
     */
    private void checkJarvisCore(JarvisCore jarvisCore, ExecutionModel executionModel) {
        /*
         * isNotNull() assertions are not soft, otherwise the runner does not print the assertion error and fails on
         * a NullPointerException in the following assertions.
         */
        Assertions.assertThat(jarvisCore.getIntentRecognitionProvider()).as("Not null IntentRecognitionProvider").isNotNull();
        Assertions.assertThat(jarvisCore.getIntentRecognitionProvider()).as("IntentRecognitionProvider is a " +
                "DialogFlowApi " +
                "instance").isInstanceOf(DialogFlowApi.class);
        DialogFlowApi dialogFlowApi = (DialogFlowApi) jarvisCore.getIntentRecognitionProvider();
        softly.assertThat(dialogFlowApi.getProjectId()).as("Valid DialogFlowAPI project ID").isEqualTo
                (DialogFlowApiTest.VALID_PROJECT_ID);
        softly.assertThat(dialogFlowApi.getLanguageCode()).as("Valid DialogFlowAPI language code").isEqualTo
                (DialogFlowApiTest.VALID_LANGUAGE_CODE);
        assertThat(jarvisCore.getExecutionService().getExecutionModel()).as("Not null ExecutionModel")
                .isNotNull();
        softly.assertThat(jarvisCore.getExecutionService().getExecutionModel()).as("Valid " +
                "ExecutionModel").isEqualTo(executionModel);
        softly.assertThat(jarvisCore.isShutdown()).as("Not shutdown").isFalse();
        Assertions.assertThat(jarvisCore.getJarvisServer()).as("Not null JarvisServer").isNotNull();
        URI corePlatformPathmapURI = URI.createURI(PlatformLoaderUtils.CORE_PLATFORM_PATHMAP + "CorePlatform.xmi");
        assertThat(jarvisCore.executionResourceSet.getResource(corePlatformPathmapURI, false)).as("CorePlatform " +
                "pathmap resolved").isNotNull();
        URI discordPlatformPathmapURI = URI.createURI(PlatformLoaderUtils.CORE_PLATFORM_PATHMAP + "DiscordPlatform" +
                ".xmi");
        assertThat(jarvisCore.executionResourceSet.getResource(discordPlatformPathmapURI, false)).as("DiscordPlatform" +
                " pathmap resolved").isNotNull();
        URI githubPlatformPathmapURI = URI.createURI(PlatformLoaderUtils.CORE_PLATFORM_PATHMAP + "GithubPlatform.xmi");
        assertThat(jarvisCore.executionResourceSet.getResource(githubPlatformPathmapURI, false)).as("GithubPlatform " +
                "pathmap resolved").isNotNull();
        URI logPlatformPathmapURI = URI.createURI(PlatformLoaderUtils.CORE_PLATFORM_PATHMAP + "LogPlatform.xmi");
        assertThat(jarvisCore.executionResourceSet.getResource(logPlatformPathmapURI, false)).as("LogPlatform " +
                "pathmap resolved").isNotNull();
        URI slackPlatformPathmapURI = URI.createURI(PlatformLoaderUtils.CORE_PLATFORM_PATHMAP + "SlackPlatform.xmi");
        assertThat(jarvisCore.executionResourceSet.getResource(slackPlatformPathmapURI, false)).as("SlackPlatform " +
                "pathmap resolved").isNotNull();
        URI coreLibraryPathmapURI = URI.createURI(LibraryLoaderUtils.CORE_LIBRARY_PATHMAP + "CoreLibrary.xmi");
        assertThat(jarvisCore.executionResourceSet.getResource(coreLibraryPathmapURI, false)).as("CoreLibrary pathmap" +
                " resolved").isNotNull();

    }

}
