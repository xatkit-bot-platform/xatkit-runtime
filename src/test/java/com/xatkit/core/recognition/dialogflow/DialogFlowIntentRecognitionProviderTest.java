package com.xatkit.core.recognition.dialogflow;

import com.xatkit.core.EventDefinitionRegistry;
import com.xatkit.core.recognition.IntentRecognitionProviderException;
import com.xatkit.core.recognition.IntentRecognitionProviderTest;
import com.xatkit.intent.CompositeEntityDefinition;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.MappingEntityDefinition;
import com.xatkit.test.util.VariableLoaderHelper;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.After;
import org.junit.Test;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

public class DialogFlowIntentRecognitionProviderTest extends IntentRecognitionProviderTest<DialogFlowIntentRecognitionProvider> {


    public static Configuration buildConfiguration() {
        return buildConfiguration(VariableLoaderHelper.getVariable(DialogFlowConfiguration.PROJECT_ID_KEY), VariableLoaderHelper
                .getVariable(DialogFlowConfiguration.LANGUAGE_CODE_KEY));
    }

    private static Configuration buildConfiguration(String projectId, String languageCode) {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(DialogFlowConfiguration.PROJECT_ID_KEY, projectId);
        configuration.addProperty(DialogFlowConfiguration.LANGUAGE_CODE_KEY, languageCode);
        configuration.addProperty(DialogFlowConfiguration.GOOGLE_CREDENTIALS_PATH_KEY, VariableLoaderHelper
                .getVariable(DialogFlowConfiguration.GOOGLE_CREDENTIALS_PATH_KEY));
        /*
         * Disable Intent loading to avoid RESOURCE_EXHAUSTED exceptions from the DialogFlow API.
         */
        configuration.addProperty(DialogFlowConfiguration.ENABLE_INTENT_LOADING_KEY, false);
        /*
         * Disable Entity loading to avoid RESOURCE_EXHAUSTED exceptions from the DialogFlow API.
         */
        configuration.addProperty(DialogFlowConfiguration.ENABLE_ENTITY_LOADING_KEY, false);
        return configuration;
    }

    @After
    public void tearDown() throws IntentRecognitionProviderException {
        if (nonNull(registeredIntentDefinition) || !registeredEntityDefinitions.isEmpty()) {
            if (isNull(intentRecognitionProvider)) {
                /*
                 * Recreate a valid instance, we need to clean the registered intents/entities.
                 */
                intentRecognitionProvider = getIntentRecognitionProvider();
            }
        }
        if (nonNull(registeredIntentDefinition)) {
            intentRecognitionProvider.deleteIntentDefinition(registeredIntentDefinition);
        }
        registeredIntentDefinition = null;
        /*
         * Delete the EntityDefinition after the IntentDefinition in case the IntentDefinition defines a parameter to
         * the entity.
         */
        if (!registeredEntityDefinitions.isEmpty()) {
            /*
             * First retrieve the CompositeEntityDefinitions and remove them, otherwise the framework will throw an
             * error when attempting to remove a MappingEntityDefinition that is referenced from a Composite one.
             */
            for (EntityDefinition registeredEntityDefinition : registeredEntityDefinitions) {
                if (registeredEntityDefinition instanceof CompositeEntityDefinition) {
                    intentRecognitionProvider.deleteEntityDefinition(registeredEntityDefinition);
                }
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Log.error(e);
            }
            for (EntityDefinition e : registeredEntityDefinitions) {
                if (e instanceof MappingEntityDefinition) {
                    intentRecognitionProvider.deleteEntityDefinition(e);
                }
            }
            registeredEntityDefinitions.clear();
        }
        if (nonNull(intentRecognitionProvider)) {
            try {
                intentRecognitionProvider.shutdown();
            } catch (IntentRecognitionProviderException e) {
                /*
                 * Already shutdown, ignore.
                 */
            }
        }
    }

    @Test(expected = NullPointerException.class)
    public void constructNullEventRegistry() {
        intentRecognitionProvider = new DialogFlowIntentRecognitionProvider(null, buildConfiguration(), null);
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        intentRecognitionProvider = new DialogFlowIntentRecognitionProvider(new EventDefinitionRegistry(), null, null);
    }

    @Test
    public void registerIntentDefinitionAlreadyRegistered() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredIntentDefinition = intentProviderTestBot.getSimpleIntent();
        intentRecognitionProvider.registerIntentDefinition(registeredIntentDefinition);
        assertThatThrownBy(() -> intentRecognitionProvider.registerIntentDefinition(registeredIntentDefinition)).isInstanceOf(IntentRecognitionProviderException.class);
    }

    @Test
    public void deleteEntityReferencedInIntent() throws IntentRecognitionProviderException {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredEntityDefinitions.add(intentProviderTestBot.getMappingEntity());
        intentRecognitionProvider.registerEntityDefinition(intentProviderTestBot.getMappingEntity());
        registeredIntentDefinition = intentProviderTestBot.getMappingEntityIntent();
        intentRecognitionProvider.registerIntentDefinition(registeredIntentDefinition);
        assertThatThrownBy(() -> intentRecognitionProvider.deleteEntityDefinition(intentProviderTestBot.getMappingEntity())).isInstanceOf(IntentRecognitionProviderException.class);
    }

    @Override
    protected DialogFlowIntentRecognitionProvider getIntentRecognitionProvider() {
        return new DialogFlowIntentRecognitionProvider(eventRegistry, buildConfiguration(), null);
    }
}
