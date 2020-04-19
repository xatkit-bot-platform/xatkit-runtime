package com.xatkit.core.recognition.dialogflow;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.EventDefinitionRegistry;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.CompositeEntityDefinition;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.MappingEntityDefinition;
import com.xatkit.test.util.TestBotExecutionModel;
import com.xatkit.test.util.TestModelLoader;
import com.xatkit.test.util.VariableLoaderHelper;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DialogFlowApiTest extends AbstractXatkitTest {

    private static TestBotExecutionModel testBotExecutionModel;

    public static Configuration buildConfiguration() {
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

    @BeforeClass
    public static void setUpBeforeClass() throws ConfigurationException {
        testBotExecutionModel = TestModelLoader.loadTestBot();
    }

    private DialogFlowApi dialogFlowApi;

    private EventDefinitionRegistry eventRegistry;

    private IntentDefinition registeredIntentDefinition;

    private List<EntityDefinition> registeredEntityDefinitions = new ArrayList<>();

    @Before
    public void setUp() {
        eventRegistry = new EventDefinitionRegistry();
        eventRegistry.registerEventDefinition(testBotExecutionModel.getSimpleIntent());
        eventRegistry.registerEventDefinition(testBotExecutionModel.getSystemEntityIntent());
        eventRegistry.registerEventDefinition(testBotExecutionModel.getMappingEntityIntent());
        eventRegistry.registerEventDefinition(testBotExecutionModel.getCompositeEntityIntent());
    }

    @After
    public void tearDown() {
        if (nonNull(registeredIntentDefinition) || !registeredEntityDefinitions.isEmpty()) {
            if (isNull(dialogFlowApi)) {
                /*
                 * Recreate a valid instance, we need to clean the registered intents/entities.
                 */
                dialogFlowApi = getValidDialogFlowApi();
            }
        }
        if (nonNull(registeredIntentDefinition)) {
            dialogFlowApi.deleteIntentDefinition(registeredIntentDefinition);
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
                    .forEach(e -> dialogFlowApi.deleteEntityDefinition(e));
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Log.error(e);
            }
            registeredEntityDefinitions.stream().filter(e -> e instanceof MappingEntityDefinition)
                    .forEach(e -> dialogFlowApi.deleteEntityDefinition(e));
            registeredEntityDefinitions.clear();
        }
        if (nonNull(dialogFlowApi)) {
            try {
                dialogFlowApi.shutdown();
            } catch (DialogFlowException e) {
                /*
                 * Already shutdown, ignore.
                 */
            }
        }
    }

    @Test(expected = NullPointerException.class)
    public void constructNullEventRegistry() {
        dialogFlowApi = new DialogFlowApi(null, buildConfiguration(), null);
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        dialogFlowApi = new DialogFlowApi(new EventDefinitionRegistry(), null, null);
    }

    @Test(expected = NullPointerException.class)
    public void registerNullIntent() {
        dialogFlowApi = getValidDialogFlowApi();
        dialogFlowApi.deleteIntentDefinition(null);
    }

    @Test
    public void registerSimpleIntent() {
        dialogFlowApi = getValidDialogFlowApi();
        registeredIntentDefinition = testBotExecutionModel.getSimpleIntent();
        dialogFlowApi.registerIntentDefinition(registeredIntentDefinition);
        /*
         * Nothing to check, the method does not return anything and does not change any visible state.
         */
    }

    @Test
    public void registerSystemEntityIntent() {
        dialogFlowApi = getValidDialogFlowApi();
        registeredIntentDefinition = testBotExecutionModel.getSystemEntityIntent();
        dialogFlowApi.registerIntentDefinition(registeredIntentDefinition);
        /*
         * Nothing to check, the method does not return anything and does not change any visible state.
         */
    }

    @Test
    public void registerMappingEntityIntent() {
        dialogFlowApi = getValidDialogFlowApi();
        registeredIntentDefinition = testBotExecutionModel.getMappingEntityIntent();
        dialogFlowApi.registerIntentDefinition(registeredIntentDefinition);
        /*
         * Nothing to check, the method does not return anything and does not change any visible state.
         */
    }

    @Test
    public void registerCompositeEntityIntent() {
        dialogFlowApi = getValidDialogFlowApi();
        registeredIntentDefinition = testBotExecutionModel.getCompositeEntityIntent();
        dialogFlowApi.registerIntentDefinition(registeredIntentDefinition);
    }

    @Test(expected = NullPointerException.class)
    public void deleteNullIntent() {
        dialogFlowApi = getValidDialogFlowApi();
        dialogFlowApi.deleteIntentDefinition(null);
    }

    @Test
    public void deleteExistingIntent() {
        dialogFlowApi = getValidDialogFlowApi();
        registeredIntentDefinition = testBotExecutionModel.getSimpleIntent();
        dialogFlowApi.registerIntentDefinition(registeredIntentDefinition);
        dialogFlowApi.deleteIntentDefinition(registeredIntentDefinition);
        /*
         * Reset to null, it has been deleted.
         */
        registeredIntentDefinition = null;
    }

    @Test
    public void registerIntentDefinitionAlreadyRegistered() {
        dialogFlowApi = getValidDialogFlowApi();
        registeredIntentDefinition = testBotExecutionModel.getSimpleIntent();
        dialogFlowApi.registerIntentDefinition(registeredIntentDefinition);
        assertThatThrownBy(() -> dialogFlowApi.registerIntentDefinition(registeredIntentDefinition)).isInstanceOf(DialogFlowException.class);
    }

    @Test(expected = NullPointerException.class)
    public void registerNullEntity() {
        dialogFlowApi = getValidDialogFlowApi();
        dialogFlowApi.registerEntityDefinition(null);
    }

    @Test
    public void registerMappingEntity() {
        dialogFlowApi = getValidDialogFlowApi();
        registeredEntityDefinitions.add(testBotExecutionModel.getMappingEntity());
        dialogFlowApi.registerEntityDefinition(testBotExecutionModel.getMappingEntity());
        /*
         * Nothing to check, the method does not return anything and does not change any visible state.
         */
    }

    @Test
    public void registerCompositeEntityReferencedEntitiesAlreadyRegistered() {
        dialogFlowApi = getValidDialogFlowApi();
        registeredEntityDefinitions.add(testBotExecutionModel.getMappingEntity());
        dialogFlowApi.registerEntityDefinition(testBotExecutionModel.getMappingEntity());
        registeredEntityDefinitions.add(testBotExecutionModel.getCompositeEntity());
        dialogFlowApi.registerEntityDefinition(testBotExecutionModel.getCompositeEntity());
        /*
         * Nothing to check, the method does not return anything and does not change any visible state.
         */
    }

    @Test
    public void registerCompositeEntityReferencedEntitiesNotRegistered() {
        dialogFlowApi = getValidDialogFlowApi();
        registeredEntityDefinitions.add(testBotExecutionModel.getCompositeEntity());
        /*
         * Add the mapping entity, it should be registered with the composite.
         */
        registeredEntityDefinitions.add(testBotExecutionModel.getMappingEntity());
        dialogFlowApi.registerEntityDefinition(testBotExecutionModel.getCompositeEntity());
        /*
         * Nothing to check, the method does not return anything and does not change any visible state.
         */
    }

    @Test(expected = NullPointerException.class)
    public void deleteNullEntity() {
        dialogFlowApi = getValidDialogFlowApi();
        dialogFlowApi.registerEntityDefinition(null);
    }

    @Test
    public void deleteEntityNotReferenced() {
        dialogFlowApi = getValidDialogFlowApi();
        registeredEntityDefinitions.add(testBotExecutionModel.getMappingEntity());
        dialogFlowApi.registerEntityDefinition(testBotExecutionModel.getMappingEntity());
        dialogFlowApi.deleteEntityDefinition(testBotExecutionModel.getMappingEntity());
        /*
         * Clean the registered entities list if the entities has been successfully deleted.
         */
        registeredEntityDefinitions.clear();
    }

    @Test
    public void deleteEntityReferencedInIntent() {
        dialogFlowApi = getValidDialogFlowApi();
        registeredEntityDefinitions.add(testBotExecutionModel.getMappingEntity());
        dialogFlowApi.registerEntityDefinition(testBotExecutionModel.getMappingEntity());
        registeredIntentDefinition = testBotExecutionModel.getMappingEntityIntent();
        dialogFlowApi.registerIntentDefinition(registeredIntentDefinition);
        assertThatThrownBy(() -> dialogFlowApi.deleteEntityDefinition(testBotExecutionModel.getMappingEntity())).isInstanceOf(DialogFlowException.class);
    }

    @Test
    public void deleteEntityReferencedInComposite() {
        dialogFlowApi = getValidDialogFlowApi();
        registeredEntityDefinitions.add(testBotExecutionModel.getCompositeEntity());
        registeredEntityDefinitions.add(testBotExecutionModel.getMappingEntity());
        dialogFlowApi.registerEntityDefinition(testBotExecutionModel.getCompositeEntity());
        assertThatThrownBy(() -> dialogFlowApi.deleteEntityDefinition(testBotExecutionModel.getMappingEntity())).isInstanceOf(DialogFlowException.class);
    }

    @Test(expected = NullPointerException.class)
    public void createSessionNullSessionId() {
        dialogFlowApi = getValidDialogFlowApi();
        dialogFlowApi.createSession(null);
    }

    @Test
    public void createSessionValidSessionId() {
        dialogFlowApi = getValidDialogFlowApi();
        XatkitSession session = dialogFlowApi.createSession("TEST");
        assertThat(session).isNotNull();
    }

    private DialogFlowApi getValidDialogFlowApi() {
        return new DialogFlowApi(eventRegistry, buildConfiguration(), null);
    }

}
