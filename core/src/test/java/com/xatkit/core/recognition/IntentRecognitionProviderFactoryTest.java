package edu.uoc.som.jarvis.core.recognition;

import edu.uoc.som.jarvis.AbstractJarvisTest;
import edu.uoc.som.jarvis.core.JarvisCore;
import edu.uoc.som.jarvis.core.recognition.dialogflow.DialogFlowApi;
import edu.uoc.som.jarvis.core.recognition.dialogflow.DialogFlowApiTest;
import edu.uoc.som.jarvis.stubs.StubJarvisCore;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class IntentRecognitionProviderFactoryTest extends AbstractJarvisTest {

    private static JarvisCore stubJarvisCore = new StubJarvisCore();

    private IntentRecognitionProvider provider;

    @AfterClass
    public static void tearDownAfterClass() {
        if (!stubJarvisCore.isShutdown()) {
            stubJarvisCore.shutdown();
        }
    }

    @After
    public void tearDown() {
        if(nonNull(provider) && !provider.isShutdown()) {
            provider.shutdown();
        }
    }

    @Test(expected = NullPointerException.class)
    public void getIntentRecognitionProviderNullJarvisCore() {
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(null, new BaseConfiguration());
    }

    @Test(expected = NullPointerException.class)
    public void getIntentRecognitionProviderNullConfiguration() {
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(stubJarvisCore, null);
    }

    @Test
    public void getIntentRecognitionProviderDialogFlowProperties() {
        Configuration configuration = DialogFlowApiTest.buildConfiguration();
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider
                (stubJarvisCore, configuration);
        assertThat(provider).as("Not null IntentRecognitionProvider").isNotNull();
        assertThat(provider).as("IntentRecognitionProvider is a DialogFlowApi").isInstanceOf(DialogFlowApi.class);
        assertThat(provider.getRecognitionMonitor()).as("Recognition monitor is not null").isNotNull();
    }

    @Test
    public void getIntentRecognitionProviderDialogFlowPropertiesDisabledAnalytics() {
        Configuration configuration = DialogFlowApiTest.buildConfiguration();
        configuration.addProperty(IntentRecognitionProviderFactory.ENABLE_RECOGNITION_ANALYTICS, false);
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(stubJarvisCore, configuration);
        assertThat(provider).as("Not null IntentRecognitionProvider").isNotNull();
        assertThat(provider).as("IntentRecognitionProvider is a DialogFlowApi").isInstanceOf(DialogFlowApi.class);
        assertThat(provider.getRecognitionMonitor()).as("Recognition monitor is null").isNull();
    }

    @Test
    public void getIntentRecognitionProviderEmptyConfiguration() {
        /*
         * The factory should return a DefaultIntentRecognitionProvider if the provided configuration does not
         * contain any IntentRecognitionProvider property.
         */
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider
                (stubJarvisCore, new BaseConfiguration());
        assertThat(provider).as("Not null IntentRecognitionProvider").isNotNull();
        assertThat(provider).as("IntentRecognitionProvider is a DefaultIntentRecognitionProvider").isInstanceOf
                (DefaultIntentRecognitionProvider.class);
        assertThat(provider.getRecognitionMonitor()).as("Recognition monitor is not null").isNotNull();
    }

    @Test
    public void getIntentRecognitionProviderEmptyConfigurationDisableAnalytics() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(IntentRecognitionProviderFactory.ENABLE_RECOGNITION_ANALYTICS, false);
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(stubJarvisCore, configuration);
        assertThat(provider.getRecognitionMonitor()).as("Recognition monitor is null").isNull();
    }
}
