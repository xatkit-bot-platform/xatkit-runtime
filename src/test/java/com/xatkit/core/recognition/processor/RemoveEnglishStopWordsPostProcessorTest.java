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

public class RemoveEnglishStopWordsPostProcessorTest extends AbstractXatkitTest {

    private RemoveEnglishStopWordsPostProcessor processor;

    private StateContext context;

    @Before
    public void setUp() {
        this.processor = null;
        this.context = ExecutionFactory.eINSTANCE.createStateContext();
        this.context.setContextId("contextId");
    }

    @Test
    public void constructValid() {
        processor = new RemoveEnglishStopWordsPostProcessor();
        assertThat(processor.getStopWordsList()).as("Stop words list is not empty").isNotEmpty();
    }

    @Test
    public void processNoContext() {
        processor = new RemoveEnglishStopWordsPostProcessor();
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        intentDefinition.setName("IntentNoContext");
        RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
        recognizedIntent.setDefinition(intentDefinition);
        RecognizedIntent processedIntent = processor.process(recognizedIntent, context);
        assertThat(processedIntent).as("Returned intent is not null").isNotNull();
    }

    @Test
    public void processContextAnyEntityNoStopWord() {
        RecognizedIntent recognizedIntent = createRecognizedIntent("ENTITY", EntityType.ANY);
        processor = new RemoveEnglishStopWordsPostProcessor();
        RecognizedIntent processedIntent = processor.process(recognizedIntent, context);
        assertThatIntentContainsValue(processedIntent, "ENTITY");
    }

    @Test
    public void processContextAnyEntityWithStopWord() {
        RecognizedIntent recognizedIntent = createRecognizedIntent("an entity", EntityType.ANY);
        processor = new RemoveEnglishStopWordsPostProcessor();
        RecognizedIntent processedIntent = processor.process(recognizedIntent, context);
        assertThatIntentContainsValue(processedIntent, "entity");
    }

    /**
     * Test for #321
     */
    @Test
    public void processContextAnyEntityWithStopWordUpperCase() {
        RecognizedIntent recognizedIntent = createRecognizedIntent("An entity", EntityType.ANY);
        processor = new RemoveEnglishStopWordsPostProcessor();
        RecognizedIntent processedIntent = processor.process(recognizedIntent, context);
        assertThatIntentContainsValue(processedIntent, "entity");
    }

    @Test
    public void processContextAnyEntityWithOnlyStopWord() {
        /*
         * This should return the initial String, and not remove the only stop word.
         */
        RecognizedIntent recognizedIntent = createRecognizedIntent("an", EntityType.ANY);
        processor = new RemoveEnglishStopWordsPostProcessor();
        RecognizedIntent processedIntent = processor.process(recognizedIntent, context);
        assertThatIntentContainsValue(processedIntent, "an");
    }

    @Test
    public void processContextNumberEntityNoStopWord() {
        RecognizedIntent recognizedIntent = createRecognizedIntent("ENTITY", EntityType.NUMBER);
        processor = new RemoveEnglishStopWordsPostProcessor();
        RecognizedIntent processedIntent = processor.process(recognizedIntent, context);
        assertThatIntentContainsValue(processedIntent, "ENTITY");
    }

    @Test
    public void processContextNumberEntityStopWord() {
        RecognizedIntent recognizedIntent = createRecognizedIntent("an entity", EntityType.NUMBER);
        processor = new RemoveEnglishStopWordsPostProcessor();
        RecognizedIntent processedIntent = processor.process(recognizedIntent, context);
        assertThatIntentContainsValue(processedIntent, "an entity");
    }

    private void assertThatIntentContainsValue(RecognizedIntent intent, String contextParameterValue) {
        assertThat(intent).as("Returned intent is not null").isNotNull();
        assertThat(intent.getValues()).hasSize(1);
        assertThat(intent.getValues().get(0).getValue()).isEqualTo(contextParameterValue);
    }

    private RecognizedIntent createRecognizedIntent(String value, EntityType entityType) {
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        intentDefinition.setName("IntentContextNoStopWord");
        ContextParameter contextParameter = IntentFactory.eINSTANCE.createContextParameter();
        contextParameter.setName("parameter");
        contextParameter.getTextFragments().add("fragment");
        BaseEntityDefinition baseEntityDefinition = IntentFactory.eINSTANCE.createBaseEntityDefinition();
        baseEntityDefinition.setEntityType(entityType);
        BaseEntityDefinitionReference baseEntityDefinitionReference =
                IntentFactory.eINSTANCE.createBaseEntityDefinitionReference();
        baseEntityDefinitionReference.setBaseEntity(baseEntityDefinition);
        contextParameter.setEntity(baseEntityDefinitionReference);
        intentDefinition.getParameters().add(contextParameter);

        RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
        recognizedIntent.setDefinition(intentDefinition);
        ContextParameterValue contextParameterValue = IntentFactory.eINSTANCE.createContextParameterValue();
        contextParameterValue.setContextParameter(contextParameter);
        contextParameterValue.setValue(value);
        recognizedIntent.getValues().add(contextParameterValue);

        return recognizedIntent;
    }
}
