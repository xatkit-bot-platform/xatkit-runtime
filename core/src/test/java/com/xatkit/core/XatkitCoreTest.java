package com.xatkit.core;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.xatkit.AbstractXatkitTest;
import com.xatkit.common.CommonPackage;
import com.xatkit.core.recognition.IntentRecognitionProviderFactory;
import com.xatkit.core.recognition.regex.RegExIntentRecognitionProvider;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.execution.ExecutionPackage;
import com.xatkit.intent.IntentPackage;
import com.xatkit.language.common.CommonStandaloneSetup;
import com.xatkit.language.execution.ExecutionRuntimeModule;
import com.xatkit.language.execution.ExecutionStandaloneSetup;
import com.xatkit.language.intent.IntentStandaloneSetup;
import com.xatkit.language.platform.PlatformStandaloneSetup;
import com.xatkit.metamodels.utils.LibraryLoaderUtils;
import com.xatkit.metamodels.utils.PlatformLoaderUtils;
import com.xatkit.platform.EventProviderDefinition;
import com.xatkit.platform.PlatformDefinition;
import com.xatkit.platform.PlatformFactory;
import com.xatkit.platform.PlatformPackage;
import com.xatkit.stubs.io.StubJsonWebhookEventProvider;
import com.xatkit.test.util.models.TestExecutionModel;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.assertj.core.api.JUnitSoftAssertions;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.xbase.XbasePackage;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class XatkitCoreTest extends AbstractXatkitTest {

    protected static String VALID_EXECUTION_MODEL_PATH = "/tmp/xatkitTestExecutionResource.execution";

    protected static ExecutionModel VALID_EXECUTION_MODEL;

    @BeforeClass
    public static void setUpBeforeClass() throws IOException {
        TestExecutionModel testExecutionModel = new TestExecutionModel();
        /*
         * Create the Resource used to store the valid execution model.
         */
        EPackage.Registry.INSTANCE.put(XbasePackage.eINSTANCE.getNsURI(), XbasePackage.eINSTANCE);
        EPackage.Registry.INSTANCE.put(CommonPackage.eINSTANCE.getNsURI(), CommonPackage.eINSTANCE);
        EPackage.Registry.INSTANCE.put(IntentPackage.eNS_URI, IntentPackage.eINSTANCE);
        EPackage.Registry.INSTANCE.put(PlatformPackage.eNS_URI, PlatformPackage.eINSTANCE);
        EPackage.Registry.INSTANCE.put(ExecutionPackage.eNS_URI, ExecutionPackage.eINSTANCE);
        CommonStandaloneSetup.doSetup();
        IntentStandaloneSetup.doSetup();
        PlatformStandaloneSetup.doSetup();
        ExecutionStandaloneSetup.doSetup();
        Injector injector = Guice.createInjector(new ExecutionRuntimeModule());
        ResourceSet testResourceSet = injector.getInstance(XtextResourceSet.class);
        File libraryFile = new File("/tmp/xatkitTestIntentResource.intent");
        if (libraryFile.exists()) {
            libraryFile.delete();
        }
        libraryFile.createNewFile();
        BufferedWriter libraryWriter = new BufferedWriter(new FileWriter(libraryFile));
        libraryWriter.write("Library StubLibrary");
        libraryWriter.newLine();
        libraryWriter.write("intent Default_Welcome_Intent {");
        libraryWriter.newLine();
        libraryWriter.write("inputs {");
        libraryWriter.newLine();
        libraryWriter.write("\"\"");
        libraryWriter.newLine();
        libraryWriter.write("}");
        libraryWriter.newLine();
        libraryWriter.write("}");
        libraryWriter.close();

        File platformFile = new File("/tmp/xatkitTestPlatformResource.platform");
        if (platformFile.exists()) {
            platformFile.delete();
        }
        platformFile.createNewFile();
        BufferedWriter platformWriter = new BufferedWriter(new FileWriter(platformFile));
        platformWriter.write("Platform StubRuntimePlatform");
        platformWriter.newLine();
        platformWriter.write("path \"com.xatkit.stubs.StubRuntimePlatform\"");
        platformWriter.newLine();
        platformWriter.write("providers {");
        platformWriter.newLine();
        platformWriter.write("input StubInputProvider");
        platformWriter.newLine();
        platformWriter.write("}");
        platformWriter.newLine();
        platformWriter.write("actions {");
        platformWriter.newLine();
        platformWriter.write("StubRuntimeAction");
        platformWriter.newLine();
        platformWriter.write("}");
        platformWriter.close();

        File executionFile = new File("/tmp/xatkitTestExecutionResource.execution");
        if (executionFile.exists()) {
            executionFile.delete();
        }
        executionFile.createNewFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(executionFile));
        writer.write("import library \"/tmp/xatkitTestIntentResource.intent\"");
        writer.newLine();
        writer.write("import platform \"/tmp/xatkitTestPlatformResource.platform\"\n");
        writer.newLine();
        writer.write("on intent Default_Welcome_Intent do\n");
        writer.newLine();
        writer.write("\tStubRuntimePlatform.StubRuntimeAction()\n");
        writer.close();

        Resource resource = testResourceSet.getResource(URI.createFileURI(VALID_EXECUTION_MODEL_PATH), true);
        VALID_EXECUTION_MODEL = (ExecutionModel) resource.getContents().get(0);

    }

    protected XatkitCore xatkitCore;

    public static Configuration buildConfiguration() {
        return buildConfiguration(VALID_EXECUTION_MODEL);
    }

    public static Configuration buildConfiguration(Object executionModel) {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(XatkitCore.EXECUTION_MODEL_KEY, executionModel);
        configuration.addProperty(XatkitCore.ENABLE_RECOGNITION_ANALYTICS, false);
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
        File validFile = new File(this.getClass().getClassLoader().getResource("Test_Platforms/ExamplePlatform" +
                ".platform").getFile());
        configuration.addProperty(XatkitCore.CUSTOM_PLATFORMS_KEY_PREFIX + "Example", validFile.getAbsolutePath());
        xatkitCore = new XatkitCore(configuration);
        assertXatkitCoreState(xatkitCore);
        URI expectedURI = URI.createFileURI(validFile.getAbsolutePath());
        List<URI> registeredResourceURIs = xatkitCore.executionResourceSet.getResources().stream().map(r -> r.getURI
                ()).collect(Collectors.toList());
        URI expectedPathmapURI = URI.createURI(PlatformLoaderUtils.CUSTOM_PLATFORM_PATHMAP + "Example.platform");
        assertThat(registeredResourceURIs).as("Custom runtimePlatform URI contained in the registered resource URIs")
                .contains(expectedPathmapURI);
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
    public void constructValidCustomLibraryFromPathInConfiguration() {
        Configuration configuration = buildConfiguration();
        File validFile = new File(this.getClass().getClassLoader().getResource("Test_Libraries/ExampleLibrary.intent")
                .getFile());
        configuration.addProperty(XatkitCore.CUSTOM_LIBRARIES_KEY_PREFIX + "Example", validFile.getAbsolutePath());
        xatkitCore = new XatkitCore(configuration);
        assertXatkitCoreState(xatkitCore);
        URI expectedURI = URI.createFileURI(validFile.getAbsolutePath());
        List<URI> registeredResourceURIs = xatkitCore.executionResourceSet.getResources().stream().map(r -> r.getURI
                ()).collect(Collectors.toList());
        URI expectedPathmapURI = URI.createURI(LibraryLoaderUtils.CUSTOM_LIBRARY_PATHMAP + "Example.intent");
        assertThat(registeredResourceURIs).as("Custom library URI contained in the registered resource URIs")
                .contains(expectedPathmapURI);
        assertThat(xatkitCore.executionResourceSet.getURIConverter().getURIMap().keySet()).as("Custom library pathmap" +
                " contained in the ResourceSet's URI map").contains(expectedPathmapURI);
        assertThat(xatkitCore.executionResourceSet.getURIConverter().getURIMap().get(expectedPathmapURI)).as("Valid " +
                "concrete URI associated to the registered pathmap URI").isEqualTo(expectedURI);
    }

    @Test
    public void constructValidConfiguration() {
        Configuration configuration = buildConfiguration();
        xatkitCore = new XatkitCore(configuration);
        assertXatkitCoreState(xatkitCore);
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
        assertXatkitCoreState(xatkitCore, executionModel);
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
        assertXatkitCoreState(xatkitCore, executionModel);
        assertThat(xatkitCore.getXatkitServer().getRegisteredRestHandlers()).as("Server RestHandler collection is not" +
                " empty").isNotEmpty();
        assertThat(xatkitCore.getXatkitServer().getRegisteredRestHandlers().iterator().next()).as("Valid " +
                "registered RestHandler").isEqualTo(StubJsonWebhookEventProvider.handler);
    }

    @Test
    public void constructRegExIntentRecognitionProviderEmptyExecutionModel() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(XatkitCore.EXECUTION_MODEL_KEY, ExecutionFactory.eINSTANCE.createExecutionModel());
        xatkitCore = new XatkitCore(configuration);
        assertThat(xatkitCore.getIntentRecognitionProvider()).as("XatkitCore uses " +
                "RegExIntentRecognitionProvider")
                .isInstanceOf(RegExIntentRecognitionProvider.class);
    }

    @Test
    public void constructRegExIntentRecognitionProviderIntentDefinitionInExecutionModel() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(XatkitCore.EXECUTION_MODEL_KEY, VALID_EXECUTION_MODEL);
        xatkitCore = new XatkitCore(configuration);
        assertThat(xatkitCore.getIntentRecognitionProvider()).as("XatkitCore uses " +
                "RegExIntentRecognitionProvider")
                .isInstanceOf(RegExIntentRecognitionProvider.class);
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
                xatkitCore.getExecutionModel(buildExecutionModelConfiguration(VALID_EXECUTION_MODEL_PATH));
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
    private void assertXatkitCoreState(XatkitCore xatkitCore) {
        assertXatkitCoreState(xatkitCore, VALID_EXECUTION_MODEL);
    }

    /**
     * Computes a set of basic assertions on the provided {@code xatkitCore} using the provided {@code
     * executionModel}.
     *
     * @param xatkitCore     the {@link XatkitCore} instance to check
     * @param executionModel the {@link ExecutionModel} to check
     */
    private void assertXatkitCoreState(XatkitCore xatkitCore, ExecutionModel executionModel) {
        /*
         * isNotNull() assertions are not soft, otherwise the runner does not print the assertion error and fails on
         * a NullPointerException in the following assertions.
         */
        assertThat(xatkitCore.getIntentRecognitionProvider()).as("Not null IntentRecognitionProvider").isNotNull();
        /*
         * The provider should be the default one, any other provider is tested in its own class.
         */
        assertThat(xatkitCore.getIntentRecognitionProvider()).as("IntentRecognitionProvider is a " +
                "RegExIntentRecognitionProvider instance").isInstanceOf(RegExIntentRecognitionProvider.class);
        assertThat(xatkitCore.getExecutionService().getExecutionModel()).as("Not null ExecutionModel")
                .isNotNull();
        softly.assertThat(xatkitCore.getExecutionService().getExecutionModel()).as("Valid " +
                "ExecutionModel").isEqualTo(executionModel);
        softly.assertThat(xatkitCore.isShutdown()).as("Not shutdown").isFalse();
        assertThat(xatkitCore.getXatkitServer()).as("Not null XatkitServer").isNotNull();
    }

}
