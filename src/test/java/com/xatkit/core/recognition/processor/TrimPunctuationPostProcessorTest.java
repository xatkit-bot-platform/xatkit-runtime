package com.xatkit.core.recognition.processor;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.BaseEntityDefinition;
import com.xatkit.intent.BaseEntityDefinitionReference;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.ContextParameterValue;
import com.xatkit.intent.EntityType;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.RecognizedIntent;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TrimPunctuationPostProcessorTest extends AbstractXatkitTest {

    private TrimPunctuationPostProcessor processor;

    private StateContext context;

    @Before
    public void setUp() {
        this.processor = null;
        this.context = ExecutionFactory.eINSTANCE.createStateContext();
        this.context.setContextId("contextId");
    }

    @Test
    public void processNoAnyParameter() {
        processor = new TrimPunctuationPostProcessor();
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        intentDefinition.setName("IntentNoAnyParameter");
        RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
        recognizedIntent.setDefinition(intentDefinition);
        RecognizedIntent processedIntent = processor.process(recognizedIntent, context);
        assertThat(processedIntent).isNotNull();
    }

    @Test
    public void processAnyParameterNoPunctuation() {
        processor = new TrimPunctuationPostProcessor();
        RecognizedIntent recognizedIntent = createRecognizedIntent("test", EntityType.ANY);
        RecognizedIntent processedIntent = processor.process(recognizedIntent, context);
        assertThatIntentContainsValue(processedIntent, "test");
    }

    @Test
    public void processAnyParameterWithQuestionMark() {
        processor = new TrimPunctuationPostProcessor();
        RecognizedIntent recognizedIntent = createRecognizedIntent("test?", EntityType.ANY);
        RecognizedIntent processedIntent = processor.process(recognizedIntent, context);
        assertThatIntentContainsValue(processedIntent, "test");
    }

    @Test
    public void processAnyParameterWithPeriod() {
        processor = new TrimPunctuationPostProcessor();
        RecognizedIntent recognizedIntent = createRecognizedIntent("test.", EntityType.ANY);
        RecognizedIntent processedIntent = processor.process(recognizedIntent, context);
        assertThatIntentContainsValue(processedIntent, "test");
    }

    @Test
    public void processAnyParameterWithExclamationMark() {
        processor = new TrimPunctuationPostProcessor();
        RecognizedIntent recognizedIntent = createRecognizedIntent("test!", EntityType.ANY);
        RecognizedIntent processedIntent = processor.process(recognizedIntent, context);
        assertThatIntentContainsValue(processedIntent, "test");
    }

    @Test
    public void processAnyParameterWithOnlyPunctuation() {
        processor = new TrimPunctuationPostProcessor();
        RecognizedIntent recognizedIntent = createRecognizedIntent("?!.", EntityType.ANY);
        RecognizedIntent processedIntent = processor.process(recognizedIntent, context);
        assertThatIntentContainsValue(processedIntent, "");
    }

    @Test
    public void processNotAnyParameterWithPunctuation() {
        processor = new TrimPunctuationPostProcessor();
        RecognizedIntent recognizedIntent = createRecognizedIntent("test?", EntityType.CITY);
        RecognizedIntent processedIntent = processor.process(recognizedIntent, context);
        assertThatIntentContainsValue(processedIntent, "test?");
    }

    private void assertThatIntentContainsValue(RecognizedIntent recognizedIntent, String value) {
        // TODO FIXME: this code is duplicated from RemoveEnglishStopWordsPostProcessorTest
        assertThat(recognizedIntent).isNotNull();
        assertThat(recognizedIntent.getValues()).hasSize(1);
        assertThat(recognizedIntent.getValues().get(0).getValue()).isEqualTo(value);
    }

    private RecognizedIntent createRecognizedIntent(String value, EntityType entityType) {
        // TODO FIXME: this code is duplicated from RemoveEnglishStopWordsPostProcessorTest
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        intentDefinition.setName("Intent");
        ContextParameter parameter = IntentFactory.eINSTANCE.createContextParameter();
        parameter.setName("parameter");
        parameter.getTextFragments().add("fragment");
        BaseEntityDefinition baseEntityDefinition = IntentFactory.eINSTANCE.createBaseEntityDefinition();
        baseEntityDefinition.setEntityType(entityType);
        BaseEntityDefinitionReference baseEntityDefinitionReference =
                IntentFactory.eINSTANCE.createBaseEntityDefinitionReference();
        baseEntityDefinitionReference.setBaseEntity(baseEntityDefinition);
        parameter.setEntity(baseEntityDefinitionReference);
        intentDefinition.getParameters().add(parameter);

        RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
        recognizedIntent.setDefinition(intentDefinition);
        ContextParameterValue contextParameterValue = IntentFactory.eINSTANCE.createContextParameterValue();
        contextParameterValue.setContextParameter(parameter);
        contextParameterValue.setValue(value);
        recognizedIntent.getValues().add(contextParameterValue);

        return recognizedIntent;
    }
}
