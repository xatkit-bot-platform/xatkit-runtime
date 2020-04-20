package com.xatkit.core.recognition.dialogflow;

import com.xatkit.core.EventDefinitionRegistry;
import com.xatkit.core.recognition.IntentRecognitionProviderTest;
import com.xatkit.intent.CompositeEntityDefinition;
import com.xatkit.intent.MappingEntityDefinition;
import com.xatkit.test.util.VariableLoaderHelper;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.After;
import org.junit.Test;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DialogFlowApiTest extends IntentRecognitionProviderTest<DialogFlowApi> {


    private static Configuration buildConfiguration() {
        return buildConfiguration(VariableLoaderHelper.getXatkitDialogFlowProject(), VariableLoaderHelper
                .getXatkitDialogFlowLanguage());
    }

    private static Configuration buildConfiguration(String projectId, String languageCode) {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(DialogFlowConfiguration.PROJECT_ID_KEY, projectId);
        configuration.addProperty(DialogFlowConfiguration.LANGUAGE_CODE_KEY, languageCode);
        configuration.addProperty(DialogFlowConfiguration.GOOGLE_CREDENTIALS_PATH_KEY, VariableLoaderHelper
                .getXatkitDialogflowCredentialsPath());
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
    public void tearDown() {
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
            registeredEntityDefinitions.stream().filter(e -> e instanceof CompositeEntityDefinition)
                    .forEach(e -> intentRecognitionProvider.deleteEntityDefinition(e));
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Log.error(e);
            }
            registeredEntityDefinitions.stream().filter(e -> e instanceof MappingEntityDefinition)
                    .forEach(e -> intentRecognitionProvider.deleteEntityDefinition(e));
            registeredEntityDefinitions.clear();
        }
        if (nonNull(intentRecognitionProvider)) {
            try {
                intentRecognitionProvider.shutdown();
            } catch (DialogFlowException e) {
                /*
                 * Already shutdown, ignore.
                 */
            }
        }
    }

    @Test(expected = NullPointerException.class)
    public void constructNullEventRegistry() {
        intentRecognitionProvider = new DialogFlowApi(null, buildConfiguration(), null);
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        intentRecognitionProvider = new DialogFlowApi(new EventDefinitionRegistry(), null, null);
    }

    @Test
    public void registerIntentDefinitionAlreadyRegistered() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredIntentDefinition = testBotExecutionModel.getSimpleIntent();
        intentRecognitionProvider.registerIntentDefinition(registeredIntentDefinition);
        assertThatThrownBy(() -> intentRecognitionProvider.registerIntentDefinition(registeredIntentDefinition)).isInstanceOf(DialogFlowException.class);
    }

    @Test
    public void deleteEntityReferencedInIntent() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredEntityDefinitions.add(testBotExecutionModel.getMappingEntity());
        intentRecognitionProvider.registerEntityDefinition(testBotExecutionModel.getMappingEntity());
        registeredIntentDefinition = testBotExecutionModel.getMappingEntityIntent();
        intentRecognitionProvider.registerIntentDefinition(registeredIntentDefinition);
        assertThatThrownBy(() -> intentRecognitionProvider.deleteEntityDefinition(testBotExecutionModel.getMappingEntity())).isInstanceOf(DialogFlowException.class);
    }

    @Test
    public void deleteEntityReferencedInComposite() {
        intentRecognitionProvider = getIntentRecognitionProvider();
        registeredEntityDefinitions.add(testBotExecutionModel.getCompositeEntity());
        registeredEntityDefinitions.add(testBotExecutionModel.getMappingEntity());
        intentRecognitionProvider.registerEntityDefinition(testBotExecutionModel.getCompositeEntity());
        assertThatThrownBy(() -> intentRecognitionProvider.deleteEntityDefinition(testBotExecutionModel.getMappingEntity())).isInstanceOf(DialogFlowException.class);
    }

    @Override
    protected DialogFlowApi getIntentRecognitionProvider() {
        return new DialogFlowApi(eventRegistry, buildConfiguration(), null);
    }
}
