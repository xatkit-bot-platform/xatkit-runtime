package com.xatkit.core.recognition;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.EventDefinitionRegistry;
import com.xatkit.core.XatkitBot;
import com.xatkit.core.XatkitException;
import com.xatkit.core.recognition.dialogflow.DialogFlowIntentRecognitionProvider;
import com.xatkit.core.recognition.dialogflow.DialogFlowIntentRecognitionProviderTest;
import com.xatkit.core.recognition.processor.IntentPostProcessor;
import com.xatkit.core.recognition.processor.PostProcessorWithConfiguration;
import com.xatkit.core.recognition.regex.RegExIntentRecognitionProvider;
import com.xatkit.core.server.XatkitServer;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IntentRecognitionProviderFactoryTest extends AbstractXatkitTest {

    private IntentRecognitionProvider provider;

    private XatkitBot xatkitBot;

    @Before
    public void setUp() {
        xatkitBot = mock(XatkitBot.class);
        when(xatkitBot.getEventDefinitionRegistry()).thenReturn(new EventDefinitionRegistry());
        when(xatkitBot.getXatkitServer()).thenReturn(mock(XatkitServer.class));
    }

    @After
    public void tearDown() {
        if (nonNull(provider) && !provider.isShutdown()) {
            try {
                provider.shutdown();
            } catch (IntentRecognitionProviderException e) {
                /*
                 * Nothing to do, the provider will be re-created anyways.
                 */
            }
        }
    }

    @Test(expected = NullPointerException.class)
    public void getIntentRecognitionProviderNullXatkitBot() {
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(null, new BaseConfiguration());
    }

    @Test(expected = NullPointerException.class)
    public void getIntentRecognitionProviderNullConfiguration() {
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(xatkitBot, null);
    }

    @Test
    public void getIntentRecognitionProviderDialogFlowProperties() {
        /*
         * Use DialogFlowIntentRecognitionProviderTest.buildConfiguration to get a valid configuration (with a valid
         * path to a credentials file)
         */
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(xatkitBot,
                DialogFlowIntentRecognitionProviderTest.buildConfiguration());
        assertThat(provider).as("Not null IntentRecognitionProvider").isNotNull();
        assertThat(provider).as("IntentRecognitionProvider is a DialogFlowIntentRecognitionProvider").isInstanceOf(DialogFlowIntentRecognitionProvider.class);
        assertThat(provider.getRecognitionMonitor()).as("Recognition monitor is not null").isNotNull();
        assertThat(provider.getPreProcessors()).as("PreProcessor list is empty").isEmpty();
        assertThat(provider.getPostProcessors()).as("PostProcessor list is empty").isEmpty();
    }

    @Test
    public void getIntentRecognitionProviderDialogFlowPropertiesDisabledAnalytics() {
        Configuration configuration = DialogFlowIntentRecognitionProviderTest.buildConfiguration();
        configuration.addProperty(IntentRecognitionProviderFactoryConfiguration.ENABLE_RECOGNITION_ANALYTICS, false);
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(xatkitBot, configuration);
        assertThat(provider).as("Not null IntentRecognitionProvider").isNotNull();
        assertThat(provider).as("IntentRecognitionProvider is a DialogFlowIntentRecognitionProvider").isInstanceOf(DialogFlowIntentRecognitionProvider.class);
        assertThat(provider.getRecognitionMonitor()).as("Recognition monitor is null").isNull();
        assertThat(provider.getPreProcessors()).as("PreProcessor list is empty").isEmpty();
        assertThat(provider.getPostProcessors()).as("PostProcessor list is empty").isEmpty();
    }

    @Test
    public void getIntentRecognitionProviderEmptyConfiguration() {
        /*
         * The factory should return a RegExIntentRecognitionProvider if the provided configuration does not
         * contain any IntentRecognitionProvider property.
         */
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider
                (xatkitBot, new BaseConfiguration());
        assertThat(provider).as("Not null IntentRecognitionProvider").isNotNull();
        assertThat(provider).as("IntentRecognitionProvider is a RegExIntentRecognitionProvider").isInstanceOf
                (RegExIntentRecognitionProvider.class);
        assertThat(provider.getRecognitionMonitor()).as("Recognition monitor is not null").isNotNull();
        assertThat(provider.getPreProcessors()).as("PreProcessor list is empty").isEmpty();
        assertThat(provider.getPostProcessors()).as("PostProcessor list is empty").isEmpty();
    }

    @Test
    public void getIntentRecognitionProviderEmptyConfigurationDisableAnalytics() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(IntentRecognitionProviderFactoryConfiguration.ENABLE_RECOGNITION_ANALYTICS, false);
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(xatkitBot, configuration);
        assertThat(provider.getRecognitionMonitor()).as("Recognition monitor is null").isNull();
        assertThat(provider.getPreProcessors()).as("PreProcessor list is empty").isEmpty();
        assertThat(provider.getPostProcessors()).as("PostProcessor list is empty").isEmpty();
    }

    @Ignore
    @Test
    public void getIntentRecognitionProviderWithPreProcessor() {
        // TODO when at least one pre-processor is implemented in xatkit-runtime
    }

    @Test
    public void getIntentRecognitionProviderWithPostProcessor() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(IntentRecognitionProviderFactoryConfiguration.RECOGNITION_POSTPROCESSORS_KEY,
                "RemoveEnglishStopWords");
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(xatkitBot, configuration);
        assertThat(provider.getPostProcessors()).as("PostProcessor list contains 1 element").hasSize(1);
        IntentPostProcessor postProcessor = provider.getPostProcessors().get(0);
        assertThat(postProcessor.getClass().getSimpleName()).as("Valid PostProcessor").isEqualTo(
                "RemoveEnglishStopWordsPostProcessor");
    }

    @Test
    public void getIntentRecognitionProviderWithPostProcessorConstructedWithConfiguration() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(IntentRecognitionProviderFactoryConfiguration.RECOGNITION_POSTPROCESSORS_KEY, "PostProcessorWithConfiguration");
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(xatkitBot, configuration);
        assertThat(provider.getPostProcessors()).hasSize(1);
        assertThat(provider.getPostProcessors().get(0)).isInstanceOf(PostProcessorWithConfiguration.class);
        PostProcessorWithConfiguration processor = (PostProcessorWithConfiguration) provider.getPostProcessors().get(0);
        assertThat(processor.getConfiguration()).isEqualTo(configuration);
    }

    @Test(expected = XatkitException.class)
    public void getIntentRecognitionProviderInvalidPreProcessorName() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(IntentRecognitionProviderFactoryConfiguration.RECOGNITION_PREPROCESSORS_KEY,
                "Invalid");
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(xatkitBot, configuration);
    }

    @Test(expected = XatkitException.class)
    public void getIntentRecognitionProviderInvalidPostProcessorName() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(IntentRecognitionProviderFactoryConfiguration.RECOGNITION_POSTPROCESSORS_KEY,
                "Invalid");
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(xatkitBot, configuration);
    }
}
