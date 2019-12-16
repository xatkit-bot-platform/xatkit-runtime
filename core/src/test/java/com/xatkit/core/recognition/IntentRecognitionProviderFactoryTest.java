package com.xatkit.core.recognition;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.XatkitCore;
import com.xatkit.core.recognition.dialogflow.DialogFlowApi;
import com.xatkit.core.recognition.dialogflow.DialogFlowApiTest;
import com.xatkit.core.recognition.regex.RegExIntentRecognitionProvider;
import com.xatkit.stubs.StubXatkitCore;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class IntentRecognitionProviderFactoryTest extends AbstractXatkitTest {

    private static XatkitCore stubXatkitCore = new StubXatkitCore();

    private IntentRecognitionProvider provider;

    @AfterClass
    public static void tearDownAfterClass() {
        if (!stubXatkitCore.isShutdown()) {
            stubXatkitCore.shutdown();
        }
    }

    @After
    public void tearDown() {
        if(nonNull(provider) && !provider.isShutdown()) {
            provider.shutdown();
        }
    }

    @Test(expected = NullPointerException.class)
    public void getIntentRecognitionProviderNullXatkitCore() {
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(null, new BaseConfiguration());
    }

    @Test(expected = NullPointerException.class)
    public void getIntentRecognitionProviderNullConfiguration() {
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(stubXatkitCore, null);
    }

    @Test
    public void getIntentRecognitionProviderDialogFlowProperties() {
        Configuration configuration = DialogFlowApiTest.buildConfiguration();
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider
                (stubXatkitCore, configuration);
        assertThat(provider).as("Not null IntentRecognitionProvider").isNotNull();
        assertThat(provider).as("IntentRecognitionProvider is a DialogFlowApi").isInstanceOf(DialogFlowApi.class);
        assertThat(provider.getRecognitionMonitor()).as("Recognition monitor is not null").isNotNull();
    }

    @Test
    public void getIntentRecognitionProviderDialogFlowPropertiesDisabledAnalytics() {
        Configuration configuration = DialogFlowApiTest.buildConfiguration();
        configuration.addProperty(XatkitCore.ENABLE_RECOGNITION_ANALYTICS, false);
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(stubXatkitCore, configuration);
        assertThat(provider).as("Not null IntentRecognitionProvider").isNotNull();
        assertThat(provider).as("IntentRecognitionProvider is a DialogFlowApi").isInstanceOf(DialogFlowApi.class);
        assertThat(provider.getRecognitionMonitor()).as("Recognition monitor is null").isNull();
    }

    @Test
    public void getIntentRecognitionProviderEmptyConfiguration() {
        /*
         * The factory should return a RegExIntentRecognitionProvider if the provided configuration does not
         * contain any IntentRecognitionProvider property.
         */
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider
                (stubXatkitCore, new BaseConfiguration());
        assertThat(provider).as("Not null IntentRecognitionProvider").isNotNull();
        assertThat(provider).as("IntentRecognitionProvider is a RegExIntentRecognitionProvider").isInstanceOf
                (RegExIntentRecognitionProvider.class);
        assertThat(provider.getRecognitionMonitor()).as("Recognition monitor is not null").isNotNull();
    }

    @Test
    public void getIntentRecognitionProviderEmptyConfigurationDisableAnalytics() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(XatkitCore.ENABLE_RECOGNITION_ANALYTICS, false);
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(stubXatkitCore, configuration);
        assertThat(provider.getRecognitionMonitor()).as("Recognition monitor is null").isNull();
    }
}
