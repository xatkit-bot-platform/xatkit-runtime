package com.xatkit.core.recognition.dialogflow.mapper;

import com.google.cloud.dialogflow.v2.Intent;
import com.xatkit.core.recognition.dialogflow.DialogFlowConfiguration;
import com.xatkit.intent.Context;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.test.util.TestBotExecutionModel;
import com.xatkit.test.util.TestModelLoader;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class DialogFlowIntentMapperTest {

    private static TestBotExecutionModel testBotExecutionModel;

    @BeforeClass
    public static void setUpBeforeClass() throws ConfigurationException {
        testBotExecutionModel = TestModelLoader.loadTestBot();
    }

    private DialogFlowIntentMapper mapper;

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        mapper = new DialogFlowIntentMapper(null, new DialogFlowEntityReferenceMapper());
    }

    @Test(expected = NullPointerException.class)
    public void constructNullEntityReferenceMapper() {
        mapper = new DialogFlowIntentMapper(getValidConfiguration(), null);
    }

    @Test
    public void constructValid() {
        mapper = new DialogFlowIntentMapper(getValidConfiguration(), new DialogFlowEntityReferenceMapper());
        assertThat(mapper).isNotNull();
    }

    @Test(expected = NullPointerException.class)
    public void mapNull() {
        mapper = new DialogFlowIntentMapper(getValidConfiguration(), new DialogFlowEntityReferenceMapper());
        mapper.mapIntentDefinition(null);
    }

    @Test
    public void mapSimpleIntent() {
        mapper = new DialogFlowIntentMapper(getValidConfiguration(), new DialogFlowEntityReferenceMapper());
        Intent intent = mapper.mapIntentDefinition(testBotExecutionModel.getSimpleIntent());
        assertCorrectMappingForIntentDefinition(testBotExecutionModel.getSimpleIntent(), intent);
    }

    @Test
    public void mapSystemEntityIntent() {
        mapper = new DialogFlowIntentMapper(getValidConfiguration(), new DialogFlowEntityReferenceMapper());
        Intent intent = mapper.mapIntentDefinition(testBotExecutionModel.getSystemEntityIntent());
        assertCorrectMappingForIntentDefinitionWithEntityReference(testBotExecutionModel.getSystemEntityIntent(),
                intent);
    }

    @Test
    public void mapMappingEntityIntent() {
        mapper = new DialogFlowIntentMapper(getValidConfiguration(), new DialogFlowEntityReferenceMapper());
        Intent intent = mapper.mapIntentDefinition(testBotExecutionModel.getMappingEntityIntent());
        assertCorrectMappingForIntentDefinitionWithEntityReference(testBotExecutionModel.getMappingEntityIntent(),
                intent);
    }

    @Test
    public void mapCompositeEntityIntent() {
        mapper = new DialogFlowIntentMapper(getValidConfiguration(), new DialogFlowEntityReferenceMapper());
        Intent intent = mapper.mapIntentDefinition(testBotExecutionModel.getCompositeEntityIntent());
        assertCorrectMappingForIntentDefinitionWithEntityReference(testBotExecutionModel.getCompositeEntityIntent(),
                intent);
    }

    private void assertCorrectMappingForIntentDefinition(IntentDefinition intentDefinition, Intent dialogFlowIntent) {
        assertThat(dialogFlowIntent).isNotNull();
        assertThat(dialogFlowIntent.getDisplayName()).isEqualTo(intentDefinition.getName());
        assertThat(dialogFlowIntent.getTrainingPhrasesList()).isNotEmpty();
        for (String trainingPhrase : intentDefinition.getTrainingSentences()) {
            assertIntentHasTrainingPhrase(dialogFlowIntent, trainingPhrase);
        }
    }

    private void assertCorrectMappingForIntentDefinitionWithEntityReference(IntentDefinition intentDefinition,
                                                                            Intent dialogFlowIntent) {
        assertCorrectMappingForIntentDefinition(intentDefinition, dialogFlowIntent);
        for (Context outContext : intentDefinition.getOutContexts()) {
            assertThat(hasOutContext(dialogFlowIntent, outContext.getName())).isTrue();
            for (ContextParameter parameter : outContext.getParameters()) {
                assertIntentContainsParameter(dialogFlowIntent, parameter);
            }
        }
    }

    private boolean hasOutContext(Intent intent, String outContext) {
        return getOutContext(intent, outContext).isPresent();
    }

    private Optional<com.google.cloud.dialogflow.v2.Context> getOutContext(Intent intent, String outContext) {
        /*
         * Use contains here, the name of the context is something like <full project name>/<context name>
         */
        return intent.getOutputContextsList().stream().filter(context -> context.getName().contains(outContext)).findAny();
    }

    private void assertIntentContainsParameter(Intent intent, ContextParameter contextParameter) {
        Optional<Intent.Parameter> parameterOptional = getParameter(intent, contextParameter.getName());
        assertThat(parameterOptional).isPresent();
        Intent.Parameter parameter = parameterOptional.get();
        String entityReferenceMapping =
                new DialogFlowEntityReferenceMapper().getMappingFor(contextParameter.getEntity().getReferredEntity());
        assertThat(parameter.getEntityTypeDisplayName()).isEqualTo(entityReferenceMapping);
    }

    private Optional<Intent.Parameter> getParameter(Intent intent, String parameter) {
        return intent.getParametersList().stream().filter(p -> p.getDisplayName().equals(parameter)).findAny();
    }

    private void assertIntentHasTrainingPhrase(Intent intent, String trainingPhrase) {
        boolean found = false;
        for (Intent.TrainingPhrase intentTrainingPhrase : intent.getTrainingPhrasesList()) {
            String mergedParts = mergeTrainingPhraseParts(intentTrainingPhrase);
            if (mergedParts.equals(trainingPhrase)) {
                found = true;
            }
        }
        assertThat(found).isTrue();
    }

    private String mergeTrainingPhraseParts(Intent.TrainingPhrase trainingPhrase) {
        StringBuilder sb = new StringBuilder();
        for (Intent.TrainingPhrase.Part part : trainingPhrase.getPartsList()) {
            sb.append(part.getText());
        }
        return sb.toString();
    }

    private DialogFlowConfiguration getValidConfiguration() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(DialogFlowConfiguration.PROJECT_ID_KEY, "PROJECT");
        return new DialogFlowConfiguration(configuration);
    }
}
