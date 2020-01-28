package com.xatkit.core.recognition.processor;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.BaseEntityDefinition;
import com.xatkit.intent.BaseEntityDefinitionReference;
import com.xatkit.intent.Context;
import com.xatkit.intent.ContextInstance;
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

    @Before
    public void setUp() {
        this.processor = null;
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
        RecognizedIntent processedIntent = processor.process(recognizedIntent, new XatkitSession("sessionID"));
        assertThat(processedIntent).as("Returned intent is not null").isNotNull();
    }

    @Test
    public void processContextAnyEntityNoStopWord() {
        RecognizedIntent recognizedIntent = createRecognizedIntent("ENTITY", EntityType.ANY);
        processor = new RemoveEnglishStopWordsPostProcessor();
        RecognizedIntent processedIntent = processor.process(recognizedIntent, new XatkitSession("sessionID"));
        assertThatIntentContainsValue(processedIntent, "ENTITY");
    }

    @Test
    public void processContextAnyEntityWithStopWord() {
        RecognizedIntent recognizedIntent = createRecognizedIntent("an entity", EntityType.ANY);
        processor = new RemoveEnglishStopWordsPostProcessor();
        RecognizedIntent processedIntent = processor.process(recognizedIntent, new XatkitSession("sessionID"));
        assertThatIntentContainsValue(processedIntent, "entity");
    }

    @Test
    public void processContextAnyEntityWithOnlyStopWord() {
        /*
         * This should return the initial String, and not remove the only stop word.
         */
        RecognizedIntent recognizedIntent = createRecognizedIntent("an", EntityType.ANY);
        processor = new RemoveEnglishStopWordsPostProcessor();
        RecognizedIntent processedIntent = processor.process(recognizedIntent, new XatkitSession("sessionID"));
        assertThatIntentContainsValue(processedIntent, "an");
    }

    @Test
    public void processContextNumberEntityNoStopWord() {
        RecognizedIntent recognizedIntent = createRecognizedIntent("ENTITY", EntityType.NUMBER);
        processor = new RemoveEnglishStopWordsPostProcessor();
        RecognizedIntent processedIntent = processor.process(recognizedIntent, new XatkitSession("sessionID"));
        assertThatIntentContainsValue(processedIntent, "ENTITY");
    }

    @Test
    public void processContextNumberEntityStopWord() {
        RecognizedIntent recognizedIntent = createRecognizedIntent("an entity", EntityType.NUMBER);
        processor = new RemoveEnglishStopWordsPostProcessor();
        RecognizedIntent processedIntent = processor.process(recognizedIntent, new XatkitSession("sessionID"));
        assertThatIntentContainsValue(processedIntent, "an entity");
    }

    private void assertThatIntentContainsValue(RecognizedIntent intent, String contextParameterValue) {
        assertThat(intent).as("Returned intent is not null").isNotNull();
        assertThat(intent.getOutContextInstance("context")).as("Out context is not null").isNotNull();
        ContextInstance outContextInstance = intent.getOutContextInstance("context");
        assertThat(outContextInstance.getValues()).as("Out context contains a value").hasSize(1);
        assertThat(outContextInstance.getValues().get(0).getValue()).as("Out context value is unchanged").isEqualTo(
                contextParameterValue);

    }

    private RecognizedIntent createRecognizedIntent(String value, EntityType entityType) {
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        intentDefinition.setName("IntentContextNoStopWord");
        Context outContext = IntentFactory.eINSTANCE.createContext();
        outContext.setName("context");
        ContextParameter contextParameter = IntentFactory.eINSTANCE.createContextParameter();
        contextParameter.setName("parameter");
        contextParameter.setTextFragment("fragment");
        BaseEntityDefinition baseEntityDefinition = IntentFactory.eINSTANCE.createBaseEntityDefinition();
        baseEntityDefinition.setEntityType(entityType);
        BaseEntityDefinitionReference baseEntityDefinitionReference =
                IntentFactory.eINSTANCE.createBaseEntityDefinitionReference();
        baseEntityDefinitionReference.setBaseEntity(baseEntityDefinition);
        contextParameter.setEntity(baseEntityDefinitionReference);
        outContext.getParameters().add(contextParameter);

        RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
        recognizedIntent.setDefinition(intentDefinition);
        ContextInstance contextInstance = IntentFactory.eINSTANCE.createContextInstance();
        contextInstance.setDefinition(outContext);
        ContextParameterValue contextParameterValue = IntentFactory.eINSTANCE.createContextParameterValue();
        contextParameterValue.setContextParameter(contextParameter);
        contextParameterValue.setValue(value);
        contextInstance.getValues().add(contextParameterValue);
        recognizedIntent.getOutContextInstances().add(contextInstance);

        return recognizedIntent;
    }
}
