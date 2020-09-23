package com.xatkit.core.recognition.dialogflow.mapper;

import com.google.cloud.dialogflow.v2.Intent;
import com.xatkit.core.recognition.IntentRecognitionProviderException;
import com.xatkit.core.recognition.dialogflow.DialogFlowConfiguration;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.test.bot.IntentProviderTestBot;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class DialogFlowIntentMapperTest {

    private static IntentProviderTestBot intentProviderTestBot;

    @BeforeClass
    public static void setUpBeforeClass() {
        intentProviderTestBot = new IntentProviderTestBot();
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
    public void mapNull() throws IntentRecognitionProviderException {
        mapper = new DialogFlowIntentMapper(getValidConfiguration(), new DialogFlowEntityReferenceMapper());
        mapper.mapIntentDefinition(null);
    }

    @Test
    public void mapSimpleIntent() throws IntentRecognitionProviderException {
        mapper = new DialogFlowIntentMapper(getValidConfiguration(), new DialogFlowEntityReferenceMapper());
        Intent intent = mapper.mapIntentDefinition(intentProviderTestBot.getSimpleIntent());
        assertCorrectMappingForIntentDefinition(intentProviderTestBot.getSimpleIntent(), intent);
    }

    @Test
    public void mapSystemEntityIntent() throws IntentRecognitionProviderException {
        mapper = new DialogFlowIntentMapper(getValidConfiguration(), new DialogFlowEntityReferenceMapper());
        Intent intent = mapper.mapIntentDefinition(intentProviderTestBot.getSystemEntityIntent());
        assertCorrectMappingForIntentDefinitionWithEntityReference(intentProviderTestBot.getSystemEntityIntent(),
                intent);
    }

    @Test
    public void mapMappingEntityIntent() throws IntentRecognitionProviderException {
        mapper = new DialogFlowIntentMapper(getValidConfiguration(), new DialogFlowEntityReferenceMapper());
        Intent intent = mapper.mapIntentDefinition(intentProviderTestBot.getMappingEntityIntent());
        assertCorrectMappingForIntentDefinitionWithEntityReference(intentProviderTestBot.getMappingEntityIntent(),
                intent);
    }

    @Test
    public void mapCompositeEntityIntent() throws IntentRecognitionProviderException {
        mapper = new DialogFlowIntentMapper(getValidConfiguration(), new DialogFlowEntityReferenceMapper());
        Intent intent = mapper.mapIntentDefinition(intentProviderTestBot.getCompositeEntityIntent());
        assertCorrectMappingForIntentDefinitionWithEntityReference(intentProviderTestBot.getCompositeEntityIntent(),
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
        assertThat(hasOutContext(dialogFlowIntent, "Xatkit")).isTrue();
        for (ContextParameter parameter : intentDefinition.getParameters()) {
            assertIntentContainsParameter(dialogFlowIntent, parameter);
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
