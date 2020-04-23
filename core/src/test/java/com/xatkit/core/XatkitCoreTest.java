package com.xatkit.core;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.recognition.IntentRecognitionProviderFactoryConfiguration;
import com.xatkit.core.recognition.regex.RegExIntentRecognitionProvider;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.test.util.TestBotExecutionModel;
import com.xatkit.test.util.TestModelLoader;
import com.xatkit.util.ModelLoader;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class XatkitCoreTest extends AbstractXatkitTest {

    private static TestBotExecutionModel testBotExecutionModel;

    protected static String VALID_EXECUTION_MODEL_PATH = "/tmp/xatkitTestExecutionResource.execution";

    public static Configuration buildConfiguration() {
        return buildConfiguration(testBotExecutionModel.getBaseModel());
    }

    public static Configuration buildConfiguration(Object executionModel) {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(ModelLoader.EXECUTION_MODEL_KEY, executionModel);
        /*
         * Disable analytics to avoid database-related issues.
         */
        configuration.addProperty(IntentRecognitionProviderFactoryConfiguration.ENABLE_RECOGNITION_ANALYTICS, false);
        return configuration;
    }

    @BeforeClass
    public static void setUpBeforeClass() throws ConfigurationException {
        testBotExecutionModel = TestModelLoader.loadTestBot();
    }

    private XatkitCore xatkitCore;

    @After
    public void tearDown() {
        if (nonNull(xatkitCore) && !xatkitCore.isShutdown()) {
            xatkitCore.shutdown();
        }
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

    @Test
    public void constructValidConfiguration() {
        Configuration configuration = buildConfiguration();
        xatkitCore = new XatkitCore(configuration);
        assertThatXatkitCoreIsCorrectlyInitilized(xatkitCore);
        assertThatEventRegistryContainsLoadedEvents(xatkitCore.getEventDefinitionRegistry(), testBotExecutionModel);
        /*
         * TODO test the platform registry
         */
    }

    @Test
    public void constructWithRegExIntentRecognitionProvider() {
        xatkitCore = new XatkitCore(buildConfiguration());
        assertThat(xatkitCore.getIntentRecognitionProvider()).as("XatkitCore uses RegExIntentRecognitionProvider")
                .isInstanceOf(RegExIntentRecognitionProvider.class);
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
        /*
         * Size = 4 because we have 4 outgoing transitions using intents in Init.
         */
        assertThat(session.getRuntimeContexts().getContextMap()).hasSize(4);
    }

    @Test
    public void shutdown() {
        xatkitCore = getValidXatkitCore();
        xatkitCore.shutdown();
        assertThat(xatkitCore.getExecutionService().isShutdown()).as("ExecutorService is shutdown");
        assertThat(xatkitCore.getIntentRecognitionProvider().isShutdown()).as("DialogFlow API is shutdown");
        assertThat(xatkitCore.getRuntimePlatformRegistry().getRuntimePlatforms()).as("Empty runtimePlatform registry").isEmpty();
    }

    private XatkitCore getValidXatkitCore() {
        Configuration configuration = buildConfiguration();
        xatkitCore = new XatkitCore(configuration);
        return xatkitCore;
    }

    /**
     * Computes a set of basic assertions on the provided {@code xatkitCore} using the
     * {@link #testBotExecutionModel}.
     *
     * @param xatkitCore the {@link XatkitCore} instance to check
     */
    private void assertThatXatkitCoreIsCorrectlyInitilized(XatkitCore xatkitCore) {
        assertThatXatkitCoreIsInitializedWithModel(xatkitCore, testBotExecutionModel.getBaseModel());
    }

    /**
     * Computes a set of basic assertions on the provided {@code xatkitCore} using the provided {@code
     * executionModel}.
     *
     * @param xatkitCore     the {@link XatkitCore} instance to check
     * @param executionModel the {@link ExecutionModel} to check
     */
    private void assertThatXatkitCoreIsInitializedWithModel(XatkitCore xatkitCore, ExecutionModel executionModel) {
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
        assertThat(xatkitCore.getRuntimePlatformRegistry()).isNotNull();
        assertThat(xatkitCore.getEventDefinitionRegistry()).isNotNull();
        assertThat(xatkitCore.getExecutionService().getExecutionModel()).as("Not null ExecutionModel")
                .isNotNull();
        assertThat(xatkitCore.getExecutionService().getExecutionModel()).as("Valid ExecutionModel").isEqualTo(executionModel);
        assertThat(xatkitCore.isShutdown()).as("Not shutdown").isFalse();
        assertThat(xatkitCore.getXatkitServer()).as("Not null XatkitServer").isNotNull();
    }

    private void assertThatEventRegistryContainsLoadedEvents(EventDefinitionRegistry registry,
                                                             TestBotExecutionModel testBotExecutionModel) {
        assertThat(registry.getEventDefinition(testBotExecutionModel.getSimpleIntent().getName())).isEqualTo(testBotExecutionModel.getSimpleIntent());
        assertThat(registry.getEventDefinition(testBotExecutionModel.getSystemEntityIntent().getName())).isEqualTo(testBotExecutionModel.getSystemEntityIntent());
        assertThat(registry.getEventDefinition(testBotExecutionModel.getMappingEntityIntent().getName())).isEqualTo(testBotExecutionModel.getMappingEntityIntent());
        assertThat(registry.getEventDefinition(testBotExecutionModel.getCompositeEntityIntent().getName())).isEqualTo(testBotExecutionModel.getCompositeEntityIntent());
    }

}
