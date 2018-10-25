package fr.zelus.jarvis.core;

import fr.zelus.jarvis.AbstractJarvisTest;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.core_modules.utils.ModulesLoaderUtils;
import fr.zelus.jarvis.intent.IntentDefinition;
import fr.zelus.jarvis.intent.IntentFactory;
import fr.zelus.jarvis.module.*;
import fr.zelus.jarvis.orchestration.ActionInstance;
import fr.zelus.jarvis.orchestration.OrchestrationFactory;
import fr.zelus.jarvis.orchestration.OrchestrationLink;
import fr.zelus.jarvis.orchestration.OrchestrationModel;
import fr.zelus.jarvis.recognition.DefaultIntentRecognitionProvider;
import fr.zelus.jarvis.recognition.dialogflow.DialogFlowApi;
import fr.zelus.jarvis.stubs.io.StubJsonWebhookEventProvider;
import fr.zelus.jarvis.test.util.VariableLoaderHelper;
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

public class JarvisCoreTest extends AbstractJarvisTest {

    protected static String VALID_PROJECT_ID = VariableLoaderHelper.getJarvisDialogFlowProject();

    protected static String VALID_LANGUAGE_CODE = "en-US";

    protected static OrchestrationModel VALID_ORCHESTRATION_MODEL;

    @BeforeClass
    public static void setUpBeforeClass() throws IOException {
        Module stubModule = ModuleFactory.eINSTANCE.createModule();
        stubModule.setName("StubJarvisModule");
        stubModule.setJarvisModulePath("fr.zelus.jarvis.stubs.StubJarvisModule");
        Action stubAction = ModuleFactory.eINSTANCE.createAction();
        stubAction.setName("StubJarvisAction");
        // No parameters, keep it simple
        stubModule.getActions().add(stubAction);
        InputProviderDefinition stubInputProvider = ModuleFactory.eINSTANCE.createInputProviderDefinition();
        stubInputProvider.setName("StubInputProvider");
        stubModule.getEventProviderDefinitions().add(stubInputProvider);
        IntentDefinition stubIntentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        stubIntentDefinition.setName("Default Welcome Intent");
        // No parameters, keep it simple
        stubModule.getIntentDefinitions().add(stubIntentDefinition);
        VALID_ORCHESTRATION_MODEL = OrchestrationFactory.eINSTANCE.createOrchestrationModel();
        OrchestrationLink link = OrchestrationFactory.eINSTANCE.createOrchestrationLink();
        link.setEvent(stubIntentDefinition);
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

    protected JarvisCore jarvisCore;

    public static Configuration buildConfiguration(String projectId, String languageCode, Object orchestrationModel) {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(DialogFlowApi.PROJECT_ID_KEY, projectId);
        configuration.addProperty(DialogFlowApi.LANGUAGE_CODE_KEY, languageCode);
        /*
         * Disable Intent loading to avoid RESOURCE_EXHAUSTED exceptions from the DialogFlow API.
         */
        configuration.addProperty(DialogFlowApi.ENABLE_INTENT_LOADING_KEY, false);
        configuration.addProperty(JarvisCore.ORCHESTRATION_MODEL_KEY, orchestrationModel);
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
     * The returned {@link JarvisCore} instance contains an empty {@link OrchestrationModel}, see
     * {@link OrchestrationServiceTest} for orchestration-related test cases.
     *
     * @return a valid {@link JarvisCore} instance
     */
    private JarvisCore getValidJarvisCore() {
        Configuration configuration = buildConfiguration(VALID_PROJECT_ID, VALID_LANGUAGE_CODE,
                VALID_ORCHESTRATION_MODEL);
        jarvisCore = new JarvisCore(configuration);
        return jarvisCore;
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        jarvisCore = new JarvisCore(null);
    }

    @Test(expected = NullPointerException.class)
    public void constructMissingOrchestrationPathInConfiguration() {
        Configuration configuration = new BaseConfiguration();
        jarvisCore = new JarvisCore(configuration);
    }

    @Test(expected = JarvisException.class)
    public void constructInvalidCustomModulePathInConfiguration() {
        Configuration configuration = buildConfiguration(VALID_PROJECT_ID, VALID_LANGUAGE_CODE,
                VALID_ORCHESTRATION_MODEL);
        configuration.addProperty(JarvisCore.CUSTOM_MODULES_KEY_PREFIX + "Example", "test");
        jarvisCore = new JarvisCore(configuration);
    }

    @Test
    public void constructValidCustomModulePathInConfiguration() {
        Configuration configuration = buildConfiguration(VALID_PROJECT_ID, VALID_LANGUAGE_CODE,
                VALID_ORCHESTRATION_MODEL);
        File validFile = new File(this.getClass().getClassLoader().getResource("Test_Modules/ExampleModule.xmi").getFile
                ());
        configuration.addProperty(JarvisCore.CUSTOM_MODULES_KEY_PREFIX + "Example", validFile.getAbsolutePath());
        jarvisCore = new JarvisCore(configuration);
        checkJarvisCore(jarvisCore);
        URI expectedURI = URI.createFileURI(validFile.getAbsolutePath());
        List<URI> registeredResourceURIs = jarvisCore.orchestrationResourceSet.getResources().stream().map(r -> r
                .getURI()).collect(Collectors.toList());
        assertThat(registeredResourceURIs).as("Custom module URI contained in the registered resource URIs").contains
                (expectedURI);
        URI expectedPathmapURI = URI.createURI(ModulesLoaderUtils.CUSTOM_MODULE_PATHMAP + "Example");
        assertThat(jarvisCore.orchestrationResourceSet.getURIConverter().getURIMap().keySet()).as("Custom module " +
                "pathmap contained in the ResourceSet's URI map").contains(expectedPathmapURI);
        assertThat(jarvisCore.orchestrationResourceSet.getURIConverter().getURIMap().get(expectedPathmapURI)).as
                ("Valid concrete URI associated to the registered pathmap URI").isEqualTo(expectedURI);
    }

    @Test
    public void constructValidConfiguration() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(DialogFlowApi.PROJECT_ID_KEY, VALID_PROJECT_ID);
        configuration.addProperty(DialogFlowApi.LANGUAGE_CODE_KEY, VALID_LANGUAGE_CODE);
        configuration.addProperty(JarvisCore.ORCHESTRATION_MODEL_KEY, VALID_ORCHESTRATION_MODEL);
        jarvisCore = new JarvisCore(configuration);
        checkJarvisCore(jarvisCore);
    }

    @Test
    public void constructValidDefaultModuleConstructor() {
        /*
         * Use another OrchestrationModel linking to the StubJarvisModuleJarvisCoreConstructor stub class, that only
         * defines a default constructor.
         */
        Module stubModule = ModuleFactory.eINSTANCE.createModule();
        stubModule.setName("StubJarvisModuleJarvisCoreConstructor");
        stubModule.setJarvisModulePath("fr.zelus.jarvis.stubs.StubJarvisModuleJarvisCoreConstructor");
        Action stubAction = ModuleFactory.eINSTANCE.createAction();
        stubAction.setName("StubJarvisAction");
        // No parameters, keep it simple
        stubModule.getActions().add(stubAction);
        InputProviderDefinition stubInputProvider = ModuleFactory.eINSTANCE.createInputProviderDefinition();
        stubInputProvider.setName("StubInputProvider");
        stubModule.getEventProviderDefinitions().add(stubInputProvider);
        IntentDefinition stubIntentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        stubIntentDefinition.setName("Default Welcome Intent");
        // No parameters, keep it simple
        stubModule.getIntentDefinitions().add(stubIntentDefinition);
        OrchestrationModel orchestrationModel = OrchestrationFactory.eINSTANCE.createOrchestrationModel();
        OrchestrationLink link = OrchestrationFactory.eINSTANCE.createOrchestrationLink();
        link.setEvent(stubIntentDefinition);
        ActionInstance actionInstance = OrchestrationFactory.eINSTANCE.createActionInstance();
        actionInstance.setAction(stubAction);
        link.getActions().add(actionInstance);
        orchestrationModel.getOrchestrationLinks().add(link);
        jarvisCore = new JarvisCore(buildConfiguration(VALID_PROJECT_ID, VALID_LANGUAGE_CODE, orchestrationModel));
        checkJarvisCore(jarvisCore, orchestrationModel);
    }

    @Test
    public void constructValidWebhookEventProvider() {
        Module stubModule = ModuleFactory.eINSTANCE.createModule();
        stubModule.setName("EmptyJarvisModule");
        stubModule.setJarvisModulePath("fr.zelus.jarvis.stubs.EmptyJarvisModule");
        EventProviderDefinition stubWebhookEventProviderDefinition = ModuleFactory.eINSTANCE
                .createEventProviderDefinition();
        stubWebhookEventProviderDefinition.setName("StubJsonWebhookEventProvider");
        stubModule.getEventProviderDefinitions().add(stubWebhookEventProviderDefinition);
        OrchestrationModel orchestrationModel = OrchestrationFactory.eINSTANCE.createOrchestrationModel();
        orchestrationModel.getEventProviderDefinitions().add(stubWebhookEventProviderDefinition);
        jarvisCore = new JarvisCore(buildConfiguration(VALID_PROJECT_ID, VALID_LANGUAGE_CODE, orchestrationModel));
        checkJarvisCore(jarvisCore, orchestrationModel);
        assertThat(jarvisCore.getJarvisServer().getRegisteredWebhookEventProviders()).as("Server WebhookEventProvider" +
                " collection is not empty").isNotEmpty();
        assertThat(jarvisCore.getJarvisServer().getRegisteredWebhookEventProviders().iterator().next()).as("Valid " +
                "registered WebhookEventProvider").isInstanceOf(StubJsonWebhookEventProvider.class);
    }

    @Test
    public void constructDefaultIntentRecognitionProviderEmptyOrchestrationModel() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JarvisCore.ORCHESTRATION_MODEL_KEY, OrchestrationFactory.eINSTANCE
                .createOrchestrationModel());
        jarvisCore = new JarvisCore(configuration);
        assertThat(jarvisCore.getIntentRecognitionProvider()).as("JarvisCore uses DefaultIntentRecognitionProvider")
                .isInstanceOf(DefaultIntentRecognitionProvider.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void constructDefaultIntentRecognitionProviderIntentDefinitionInOrchestrationModel() {
        /*
         * This test should fail: the DefaultIntentRecognitionProvider does not allow to register IntentDefinitions.
         */
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JarvisCore.ORCHESTRATION_MODEL_KEY, VALID_ORCHESTRATION_MODEL);
        jarvisCore = new JarvisCore(configuration);
    }

    @Test(expected = JarvisException.class)
    public void getOrchestrationModelInvalidType() {
        jarvisCore = getValidJarvisCore();
        OrchestrationModel orchestrationModel = jarvisCore.getOrchestrationModel(new Integer(2));
    }

    @Test(expected = JarvisException.class)
    public void getOrchestrationModelFromInvalidString() {
        jarvisCore = getValidJarvisCore();
        OrchestrationModel orchestrationModel = jarvisCore.getOrchestrationModel("/tmp/test.xmi");
    }

    @Test(expected = JarvisException.class)
    public void getOrchestrationModelFromInvalidURI() {
        jarvisCore = getValidJarvisCore();
        OrchestrationModel orchestrationModel = jarvisCore.getOrchestrationModel(URI.createURI("/tmp/test.xmi"));
    }

    @Test
    public void getOrchestrationModelFromValidInMemory() {
        jarvisCore = getValidJarvisCore();
        OrchestrationModel orchestrationModel = jarvisCore.getOrchestrationModel(VALID_ORCHESTRATION_MODEL);
        assertThat(orchestrationModel).as("Valid OrchestrationModel").isEqualTo(VALID_ORCHESTRATION_MODEL);
    }

    @Test
    public void getOrchestrationModelFromValidString() {
        jarvisCore = getValidJarvisCore();
        OrchestrationModel orchestrationModel = jarvisCore.getOrchestrationModel(VALID_ORCHESTRATION_MODEL.eResource
                ().getURI().toString());
        assertThat(orchestrationModel).as("Not null OrchestrationModel").isNotNull();
        /*
         * Not enough, but comparing the entire content of the model is more complicated than it looks like.
         */
        assertThat(orchestrationModel.getOrchestrationLinks()).as("Valid OrchestrationLink size").hasSize
                (VALID_ORCHESTRATION_MODEL.getOrchestrationLinks().size());
    }

    @Test
    public void getOrchestrationModelFromValidURI() {
        jarvisCore = getValidJarvisCore();
        OrchestrationModel orchestrationModel = jarvisCore.getOrchestrationModel(VALID_ORCHESTRATION_MODEL.eResource
                ().getURI());
        assertThat(orchestrationModel).as("Not null OrchestrationModel").isNotNull();
        /*
         * Not enough, but comparing the entire content of the model is more complicated than it looks like.
         */
        assertThat(orchestrationModel.getOrchestrationLinks()).as("Valid OrchestrationLink size").hasSize
                (VALID_ORCHESTRATION_MODEL.getOrchestrationLinks().size());
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
        assertThat(session.getJarvisContext()).as("Not null session context").isNotNull();
        assertThat(session.getJarvisContext().getContextMap()).as("Empty session context").isEmpty();
    }

    @Test
    public void shutdown() {
        jarvisCore = getValidJarvisCore();
        jarvisCore.shutdown();
        softly.assertThat(jarvisCore.getOrchestrationService().isShutdown()).as("ExecutorService is shutdown");
        softly.assertThat(jarvisCore.getIntentRecognitionProvider().isShutdown()).as("DialogFlow API is shutdown");
        softly.assertThat(jarvisCore.getJarvisModuleRegistry().getModules()).as("Empty module registry").isEmpty();
    }

    /**
     * Computes a set of basic assertions on the provided {@code jarvisCore} using the
     * {@link #VALID_ORCHESTRATION_MODEL}.
     *
     * @param jarvisCore the {@link JarvisCore} instance to check
     */
    private void checkJarvisCore(JarvisCore jarvisCore) {
        checkJarvisCore(jarvisCore, VALID_ORCHESTRATION_MODEL);
    }

    /**
     * Computes a set of basic assertions on the provided {@code jarvisCore} using the provided {@code
     * orchestrationModel}.
     *
     * @param jarvisCore         the {@link JarvisCore} instance to check
     * @param orchestrationModel the {@link OrchestrationModel} to check
     */
    private void checkJarvisCore(JarvisCore jarvisCore, OrchestrationModel orchestrationModel) {
        /*
         * isNotNull() assertions are not soft, otherwise the runner does not print the assertion error and fails on
         * a NullPointerException in the following assertions.
         */
        assertThat(jarvisCore.getIntentRecognitionProvider()).as("Not null IntentRecognitionProvider").isNotNull();
        assertThat(jarvisCore.getIntentRecognitionProvider()).as("IntentRecognitionProvider is a DialogFlowApi " +
                "instance").isInstanceOf(DialogFlowApi.class);
        DialogFlowApi dialogFlowApi = (DialogFlowApi) jarvisCore.getIntentRecognitionProvider();
        softly.assertThat(dialogFlowApi.getProjectId()).as("Valid DialogFlowAPI project ID").isEqualTo
                (VALID_PROJECT_ID);
        softly.assertThat(dialogFlowApi.getLanguageCode()).as("Valid DialogFlowAPI language code").isEqualTo
                (VALID_LANGUAGE_CODE);
        assertThat(jarvisCore.getOrchestrationService().getOrchestrationModel()).as("Not null OrchestrationModel")
                .isNotNull();
        softly.assertThat(jarvisCore.getOrchestrationService().getOrchestrationModel()).as("Valid " +
                "OrchestrationModel").isEqualTo(orchestrationModel);
        softly.assertThat(jarvisCore.isShutdown()).as("Not shutdown").isFalse();
        assertThat(jarvisCore.getJarvisServer()).as("Not null JarvisServer").isNotNull();
        URI coreModulePathmapURI = URI.createURI(ModulesLoaderUtils.CORE_MODULE_PATHMAP + "CoreModule.xmi");
        assertThat(jarvisCore.orchestrationResourceSet.getResource(coreModulePathmapURI, false)).as("CoreModule " +
                "pathmap resolved").isNotNull();
        URI discordModulePathmapURI = URI.createURI(ModulesLoaderUtils.CORE_MODULE_PATHMAP + "DiscordModule.xmi");
        assertThat(jarvisCore.orchestrationResourceSet.getResource(discordModulePathmapURI, false)).as("DiscordModule" +
                " pathmap resolved").isNotNull();
        URI genericChatModulePathmapURI = URI.createURI(ModulesLoaderUtils.CORE_MODULE_PATHMAP + "GenericChatModule" +
                ".xmi");
        assertThat(jarvisCore.orchestrationResourceSet.getResource(genericChatModulePathmapURI, false)).as
                ("GenericChatModule pathmap resolved").isNotNull();
        URI githubModulePathmapURI = URI.createURI(ModulesLoaderUtils.CORE_MODULE_PATHMAP + "GithubModule.xmi");
        assertThat(jarvisCore.orchestrationResourceSet.getResource(githubModulePathmapURI, false)).as("GithubModule " +
                "pathmap resolved").isNotNull();
        URI logModulePathmapURI = URI.createURI(ModulesLoaderUtils.CORE_MODULE_PATHMAP + "LogModule.xmi");
        assertThat(jarvisCore.orchestrationResourceSet.getResource(logModulePathmapURI, false)).as("LogModule " +
                "pathmap resolved").isNotNull();
        URI slackModulePathmapURI = URI.createURI(ModulesLoaderUtils.CORE_MODULE_PATHMAP + "SlackModule.xmi");
        assertThat(jarvisCore.orchestrationResourceSet.getResource(slackModulePathmapURI, false)).as("SlackModule " +
                "pathmap resolved").isNotNull();

    }

}
