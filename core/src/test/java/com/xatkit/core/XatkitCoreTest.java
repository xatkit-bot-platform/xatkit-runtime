package com.xatkit.core;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.recognition.DefaultIntentRecognitionProvider;
import com.xatkit.core.recognition.IntentRecognitionProviderFactory;
import com.xatkit.core.recognition.dialogflow.DialogFlowApi;
import com.xatkit.core.recognition.dialogflow.DialogFlowApiTest;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.core_resources.utils.LibraryLoaderUtils;
import com.xatkit.core_resources.utils.PlatformLoaderUtils;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.platform.EventProviderDefinition;
import com.xatkit.platform.PlatformDefinition;
import com.xatkit.platform.PlatformFactory;
import com.xatkit.stubs.io.StubJsonWebhookEventProvider;
import com.xatkit.test.util.models.TestExecutionModel;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
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

public class XatkitCoreTest extends AbstractXatkitTest {

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

    protected XatkitCore xatkitCore;

    public static Configuration buildConfiguration() {
        return buildConfiguration(VALID_EXECUTION_MODEL);
    }

    public static Configuration buildConfiguration(Object executionModel) {
        Configuration configuration = DialogFlowApiTest.buildConfiguration();
        configuration.addProperty(XatkitCore.EXECUTION_MODEL_KEY, executionModel);
        configuration.addProperty(IntentRecognitionProviderFactory.ENABLE_RECOGNITION_ANALYTICS, false);
        return configuration;
    }

    @After
    public void tearDown() {
        if (nonNull(xatkitCore) && !xatkitCore.isShutdown()) {
            xatkitCore.shutdown();
        }
    }

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    /**
     * Returns a valid {@link XatkitCore} instance.
     * <p>
     * The returned {@link XatkitCore} instance contains an empty {@link ExecutionModel}, see
     * {@link ExecutionServiceTest} for execution-related test cases.
     *
     * @return a valid {@link XatkitCore} instance
     */
    private XatkitCore getValidXatkitCore() {
        Configuration configuration = buildConfiguration();
        xatkitCore = new XatkitCore(configuration);
        return xatkitCore;
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        xatkitCore = new XatkitCore(null);
    }

    @Test(expected = NullPointerException.class)
    public void constructMissingExecutionPathInConfiguration() {
        Configuration configuration = new BaseConfiguration();
        xatkitCore = new XatkitCore(configuration);
    }

    @Test(expected = XatkitException.class)
    public void constructInvalidCustomPlatformPathInConfiguration() {
        Configuration configuration = buildConfiguration();
        configuration.addProperty(XatkitCore.CUSTOM_PLATFORMS_KEY_PREFIX + "Example", "test");
        xatkitCore = new XatkitCore(configuration);
    }

    @Test
    public void constructValidCustomPlatformPathInConfiguration() {
        Configuration configuration = buildConfiguration();
        File validFile = new File(this.getClass().getClassLoader().getResource("Test_Platforms/ExamplePlatform.xmi")
                .getFile());
        configuration.addProperty(XatkitCore.CUSTOM_PLATFORMS_KEY_PREFIX + "Example", validFile.getAbsolutePath());
        xatkitCore = new XatkitCore(configuration);
        checkXatkitCore(xatkitCore);
        URI expectedURI = URI.createFileURI(validFile.getAbsolutePath());
        List<URI> registeredResourceURIs = xatkitCore.executionResourceSet.getResources().stream().map(r -> r.getURI
                ()).collect(Collectors.toList());
        assertThat(registeredResourceURIs).as("Custom runtimePlatform URI contained in the registered resource URIs")
                .contains(expectedURI);
        URI expectedPathmapURI = URI.createURI(PlatformLoaderUtils.CUSTOM_PLATFORM_PATHMAP + "Example");
        assertThat(xatkitCore.executionResourceSet.getURIConverter().getURIMap().keySet()).as("Custom runtimePlatform" +
                "pathmap contained in the ResourceSet's URI map").contains(expectedPathmapURI);
        assertThat(xatkitCore.executionResourceSet.getURIConverter().getURIMap().get(expectedPathmapURI)).as
                ("Valid concrete URI associated to the registered pathmap URI").isEqualTo(expectedURI);
    }

    @Test(expected = XatkitException.class)
    public void constructInvalidCustomLibraryPathInConfiguration() {
        Configuration configuration = buildConfiguration();
        configuration.addProperty(XatkitCore.CUSTOM_LIBRARIES_KEY_PREFIX + "Example", "test");
        xatkitCore = new XatkitCore(configuration);
    }

    @Test
    public void constructValidCustomPlatformFromPathInConfiguration() {
        Configuration configuration = buildConfiguration();
        File validFile = new File(this.getClass().getClassLoader().getResource("Test_Libraries/ExampleLibrary.xmi")
                .getFile());
        configuration.addProperty(XatkitCore.CUSTOM_LIBRARIES_KEY_PREFIX + "Example", validFile.getAbsolutePath());
        xatkitCore = new XatkitCore(configuration);
        checkXatkitCore(xatkitCore);
        URI expectedURI = URI.createFileURI(validFile.getAbsolutePath());
        List<URI> registeredResourceURIs = xatkitCore.executionResourceSet.getResources().stream().map(r -> r.getURI
                ()).collect(Collectors.toList());
        assertThat(registeredResourceURIs).as("Custom library URI contained in the registered resource URIs")
                .contains(expectedURI);
        URI expectedPathmapURI = URI.createURI(LibraryLoaderUtils.CUSTOM_LIBRARY_PATHMAP + "Example");
        assertThat(xatkitCore.executionResourceSet.getURIConverter().getURIMap().keySet()).as("Custom library pathmap" +
                " contained in the ResourceSet's URI map").contains(expectedPathmapURI);
        assertThat(xatkitCore.executionResourceSet.getURIConverter().getURIMap().get(expectedPathmapURI)).as("Valid " +
                "concrete URI associated to the registered pathmap URI").isEqualTo(expectedURI);
    }

    @Test(expected = XatkitException.class)
    public void constructInvalidPlatformFromExecutionModel() {
        TestExecutionModel testExecutionModel = new TestExecutionModel();
        ExecutionModel executionModel = testExecutionModel.getExecutionModel();
        PlatformDefinition platformDefinition = testExecutionModel.getTestPlatformModel().getPlatformDefinition();
        platformDefinition.setName("InvalidPlatform");
        platformDefinition.setRuntimePath("com.xatkit.stubs.InvalidPlatform");
        Configuration configuration = buildConfiguration(executionModel);
        xatkitCore = new XatkitCore(configuration);
    }

    @Test
    public void constructValidConfiguration() {
        Configuration configuration = buildConfiguration();
        xatkitCore = new XatkitCore(configuration);
        checkXatkitCore(xatkitCore);
    }

    @Test
    public void constructValidDefaultRuntimePlatformConstructor() {
        /*
         * Use another ExecutionModel linking to the StubRuntimePlatformXatkitCoreConstructor stub class, that only
         * defines a default constructor.
         */
        TestExecutionModel testExecutionModel = new TestExecutionModel();
        PlatformDefinition platformDefinition = testExecutionModel.getTestPlatformModel().getPlatformDefinition();
        platformDefinition.setName("StubRuntimePlatformXatkitCoreConstructor");
        platformDefinition.setRuntimePath("com.xatkit.stubs.StubRuntimePlatformXatkitCoreConstructor");

        ExecutionModel executionModel = testExecutionModel.getExecutionModel();

        xatkitCore = new XatkitCore(buildConfiguration(executionModel));
        checkXatkitCore(xatkitCore, executionModel);
    }

    @Test
    public void constructValidWebhookEventProvider() {
        PlatformDefinition stubPlatformDefinition = PlatformFactory.eINSTANCE.createPlatformDefinition();
        stubPlatformDefinition.setName("EmptyRuntimePlatform");
        stubPlatformDefinition.setRuntimePath("com.xatkit.stubs.EmptyRuntimePlatform");
        EventProviderDefinition stubWebhookEventProviderDefinition = PlatformFactory.eINSTANCE
                .createEventProviderDefinition();
        stubWebhookEventProviderDefinition.setName("StubJsonWebhookEventProvider");
        stubPlatformDefinition.getEventProviderDefinitions().add(stubWebhookEventProviderDefinition);
        ExecutionModel executionModel = ExecutionFactory.eINSTANCE.createExecutionModel();
        executionModel.getEventProviderDefinitions().add(stubWebhookEventProviderDefinition);
        xatkitCore = new XatkitCore(buildConfiguration(executionModel));
        checkXatkitCore(xatkitCore, executionModel);
        assertThat(xatkitCore.getXatkitServer().getRegisteredWebhookEventProviders()).as("Server " +
                "WebhookEventProvider collection is not empty").isNotEmpty();
        assertThat(xatkitCore.getXatkitServer().getRegisteredWebhookEventProviders().iterator().next()).as("Valid " +
                "registered WebhookEventProvider").isInstanceOf(StubJsonWebhookEventProvider.class);
    }

    @Test
    public void constructDefaultIntentRecognitionProviderEmptyExecutionModel() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(XatkitCore.EXECUTION_MODEL_KEY, ExecutionFactory.eINSTANCE.createExecutionModel());
        xatkitCore = new XatkitCore(configuration);
        assertThat(xatkitCore.getIntentRecognitionProvider()).as("XatkitCore uses " +
                "DefaultIntentRecognitionProvider")
                .isInstanceOf(DefaultIntentRecognitionProvider.class);
    }

    @Test
    public void constructDefaultIntentRecognitionProviderIntentDefinitionInExecutionModel() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(XatkitCore.EXECUTION_MODEL_KEY, VALID_EXECUTION_MODEL);
        xatkitCore = new XatkitCore(configuration);
        assertThat(xatkitCore.getIntentRecognitionProvider()).as("XatkitCore uses " +
                "DefaultIntentRecognitionProvider")
                .isInstanceOf(DefaultIntentRecognitionProvider.class);
    }

    @Test(expected = XatkitException.class)
    public void getExecutionModelInvalidType() {
        xatkitCore = getValidXatkitCore();
        ExecutionModel executionModel = xatkitCore.getExecutionModel(buildExecutionModelConfiguration(new Integer(2)));
    }

    @Test(expected = XatkitException.class)
    public void getExecutionModelFromInvalidString() {
        xatkitCore = getValidXatkitCore();
        ExecutionModel executionModel = xatkitCore.getExecutionModel(buildExecutionModelConfiguration("/tmp/test.xmi"));
    }

    @Test(expected = XatkitException.class)
    public void getExecutionModelFromInvalidURI() {
        xatkitCore = getValidXatkitCore();
        ExecutionModel executionModel = xatkitCore.getExecutionModel(buildExecutionModelConfiguration(URI.createURI(
                "/tmp/test.xmi")));
    }

    @Test
    public void getExecutionModelFromValidInMemory() {
        xatkitCore = getValidXatkitCore();
        ExecutionModel executionModel =
                xatkitCore.getExecutionModel(buildExecutionModelConfiguration(VALID_EXECUTION_MODEL));
        assertThat(executionModel).as("Valid ExecutionModel").isEqualTo(VALID_EXECUTION_MODEL);
    }

    @Test
    public void getExecutionModelFromValidString() {
        xatkitCore = getValidXatkitCore();
        ExecutionModel executionModel =
                xatkitCore.getExecutionModel(buildExecutionModelConfiguration(VALID_EXECUTION_MODEL.eResource().getURI()
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
        xatkitCore = getValidXatkitCore();
        ExecutionModel executionModel =
                xatkitCore.getExecutionModel(buildExecutionModelConfiguration(VALID_EXECUTION_MODEL.eResource
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
        configuration.addProperty(XatkitCore.EXECUTION_MODEL_KEY, value);
        return configuration;
    }

    @Test(expected = XatkitException.class)
    public void shutdownAlreadyShutdown() {
        xatkitCore = getValidXatkitCore();
        xatkitCore.shutdown();
        xatkitCore.shutdown();
    }

    @Test(expected = NullPointerException.class)
    public void getOrCreateXatkitSessionNullSessionId() {
        xatkitCore = getValidXatkitCore();
        xatkitCore.getOrCreateXatkitSession(null);
    }

    @Test
    public void getOrCreateXatkitSessionValidSessionId() {
        xatkitCore = getValidXatkitCore();
        XatkitSession session = xatkitCore.getOrCreateXatkitSession("sessionID");
        assertThat(session).as("Not null XatkitSession").isNotNull();
        /*
         * Use contains because the underlying DialogFlow API add additional identification information in the
         * returned XatkitSession.
         */
        assertThat(session.getSessionId()).as("Valid session ID").contains("sessionID");
        assertThat(session.getRuntimeContexts()).as("Not null session context").isNotNull();
        assertThat(session.getRuntimeContexts().getContextMap()).as("Empty session context").isEmpty();
    }

    @Test
    public void shutdown() {
        xatkitCore = getValidXatkitCore();
        xatkitCore.shutdown();
        softly.assertThat(xatkitCore.getExecutionService().isShutdown()).as("ExecutorService is shutdown");
        softly.assertThat(xatkitCore.getIntentRecognitionProvider().isShutdown()).as("DialogFlow API is shutdown");
        softly.assertThat(xatkitCore.getRuntimePlatformRegistry().getRuntimePlatforms()).as("Empty runtimePlatform " +
                "registry").isEmpty();
    }

    /**
     * Computes a set of basic assertions on the provided {@code xatkitCore} using the
     * {@link #VALID_EXECUTION_MODEL}.
     *
     * @param xatkitCore the {@link XatkitCore} instance to check
     */
    private void checkXatkitCore(XatkitCore xatkitCore) {
        checkXatkitCore(xatkitCore, VALID_EXECUTION_MODEL);
    }

    /**
     * Computes a set of basic assertions on the provided {@code xatkitCore} using the provided {@code
     * executionModel}.
     *
     * @param xatkitCore     the {@link XatkitCore} instance to check
     * @param executionModel the {@link ExecutionModel} to check
     */
    private void checkXatkitCore(XatkitCore xatkitCore, ExecutionModel executionModel) {
        /*
         * isNotNull() assertions are not soft, otherwise the runner does not print the assertion error and fails on
         * a NullPointerException in the following assertions.
         */
        assertThat(xatkitCore.getIntentRecognitionProvider()).as("Not null IntentRecognitionProvider").isNotNull();
        assertThat(xatkitCore.getIntentRecognitionProvider()).as("IntentRecognitionProvider is a " +
                "DialogFlowApi " +
                "instance").isInstanceOf(DialogFlowApi.class);
        DialogFlowApi dialogFlowApi = (DialogFlowApi) xatkitCore.getIntentRecognitionProvider();
        softly.assertThat(dialogFlowApi.getProjectId()).as("Valid DialogFlowAPI project ID").isEqualTo
                (DialogFlowApiTest.VALID_PROJECT_ID);
        softly.assertThat(dialogFlowApi.getLanguageCode()).as("Valid DialogFlowAPI language code").isEqualTo
                (DialogFlowApiTest.VALID_LANGUAGE_CODE);
        assertThat(xatkitCore.getExecutionService().getExecutionModel()).as("Not null ExecutionModel")
                .isNotNull();
        softly.assertThat(xatkitCore.getExecutionService().getExecutionModel()).as("Valid " +
                "ExecutionModel").isEqualTo(executionModel);
        softly.assertThat(xatkitCore.isShutdown()).as("Not shutdown").isFalse();
        assertThat(xatkitCore.getXatkitServer()).as("Not null XatkitServer").isNotNull();
        URI corePlatformPathmapURI = URI.createURI(PlatformLoaderUtils.CORE_PLATFORM_PATHMAP + "CorePlatform.xmi");
        assertThat(xatkitCore.executionResourceSet.getResource(corePlatformPathmapURI, false)).as("CorePlatform " +
                "pathmap resolved").isNotNull();
        URI discordPlatformPathmapURI = URI.createURI(PlatformLoaderUtils.CORE_PLATFORM_PATHMAP + "DiscordPlatform" +
                ".xmi");
        assertThat(xatkitCore.executionResourceSet.getResource(discordPlatformPathmapURI, false)).as("DiscordPlatform" +
                " pathmap resolved").isNotNull();
        URI githubPlatformPathmapURI = URI.createURI(PlatformLoaderUtils.CORE_PLATFORM_PATHMAP + "GithubPlatform.xmi");
        assertThat(xatkitCore.executionResourceSet.getResource(githubPlatformPathmapURI, false)).as("GithubPlatform " +
                "pathmap resolved").isNotNull();
        URI logPlatformPathmapURI = URI.createURI(PlatformLoaderUtils.CORE_PLATFORM_PATHMAP + "LogPlatform.xmi");
        assertThat(xatkitCore.executionResourceSet.getResource(logPlatformPathmapURI, false)).as("LogPlatform " +
                "pathmap resolved").isNotNull();
        URI slackPlatformPathmapURI = URI.createURI(PlatformLoaderUtils.CORE_PLATFORM_PATHMAP + "SlackPlatform.xmi");
        assertThat(xatkitCore.executionResourceSet.getResource(slackPlatformPathmapURI, false)).as("SlackPlatform " +
                "pathmap resolved").isNotNull();
        URI coreLibraryPathmapURI = URI.createURI(LibraryLoaderUtils.CORE_LIBRARY_PATHMAP + "CoreLibrary.xmi");
        assertThat(xatkitCore.executionResourceSet.getResource(coreLibraryPathmapURI, false)).as("CoreLibrary pathmap" +
                " resolved").isNotNull();

    }

}
