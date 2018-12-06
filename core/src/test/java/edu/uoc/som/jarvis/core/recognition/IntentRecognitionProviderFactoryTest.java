package edu.uoc.som.jarvis.core.recognition;

import edu.uoc.som.jarvis.AbstractJarvisTest;
import edu.uoc.som.jarvis.core.JarvisCore;
import edu.uoc.som.jarvis.core.recognition.dialogflow.DialogFlowApi;
import edu.uoc.som.jarvis.core.recognition.dialogflow.DialogFlowApiTest;
import edu.uoc.som.jarvis.stubs.StubJarvisCore;
import edu.uoc.som.jarvis.test.util.VariableLoaderHelper;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.AfterClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IntentRecognitionProviderFactoryTest extends AbstractJarvisTest {

    private static JarvisCore stubJarvisCore = new StubJarvisCore();

    @AfterClass
    public static void tearDownAfterClass() {
        if (!stubJarvisCore.isShutdown()) {
            stubJarvisCore.shutdown();
        }
    }

    @Test(expected = NullPointerException.class)
    public void getIntentRecognitionProviderNullJarvisCore() {
        IntentRecognitionProviderFactory.getIntentRecognitionProvider(null, new BaseConfiguration());
    }

    @Test(expected = NullPointerException.class)
    public void getIntentRecognitionProviderNullConfiguration() {
        IntentRecognitionProviderFactory.getIntentRecognitionProvider(stubJarvisCore, null);
    }

    @Test
    public void getIntentRecognitionProviderDialogFlowProperties() {
        Configuration configuration = DialogFlowApiTest.buildConfiguration();
        IntentRecognitionProvider provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider
                (stubJarvisCore, configuration);
        assertThat(provider).as("Not null IntentRecognitionProvider").isNotNull();
        assertThat(provider).as("IntentRecognitionProvider is a DialogFlowApi").isInstanceOf(DialogFlowApi.class);
    }

    @Test
    public void getIntentRecognitionProviderEmptyConfiguration() {
        /*
         * The factory should return a DefaultIntentRecognitionProvider if the provided configuration does not
         * contain any IntentRecognitionProvider property.
         */
        IntentRecognitionProvider provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider
                (stubJarvisCore, new BaseConfiguration());
        assertThat(provider).as("Not null IntentRecognitionProvider").isNotNull();
        assertThat(provider).as("IntentRecognitionProvider is a DefaultIntentRecognitionProvider").isInstanceOf
                (DefaultIntentRecognitionProvider.class);
    }
}
