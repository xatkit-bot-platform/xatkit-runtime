package com.xatkit.core.recognition.processor;

import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.StateContext;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class EmojiToTextPreProcessorTest {

    private EmojiToTextPreProcessor processor;
    private StateContext context;

    @Before
    public void setUp() {
        this.processor = null;
        this.context = ExecutionFactory.eINSTANCE.createStateContext();
        this.context.setContextId("contextId");
    }

    @Test
    public void testReplaceEmojis() {
        Configuration botConfiguration = new BaseConfiguration();
        botConfiguration.setProperty(EmojiToTextPreProcessor.REMOVE_EMOJIS, false);
        processor = new EmojiToTextPreProcessor(botConfiguration);
        String input = ("\uD83E\uDDD1");
        String processedInput = processor.process(input, context);
        assertThat(processedInput).isEqualTo("person");
        input = "hello\uD83E\uDDD1hello\uD83E\uDDD1 \uD83E\uDDD1\uD83E\uDDD1 \uD83E\uDDD1";
        processedInput = processor.process(input, context);
        assertThat(processedInput).isEqualTo("hello person hello person person person person");
    }

    @Test
    public void testRemoveEmojis() {
        Configuration botConfiguration = new BaseConfiguration();
        botConfiguration.setProperty(EmojiToTextPreProcessor.REMOVE_EMOJIS, true);
        processor = new EmojiToTextPreProcessor(botConfiguration);
        String input = ("\uD83E\uDDD1");
        String processedInput = processor.process(input, context);
        assertThat(processedInput).isEqualTo("");
        input = "hello \uD83E\uDDD1hello\uD83E\uDDD1 \uD83E\uDDD1\uD83E\uDDD1 \uD83E\uDDD1";
        processedInput = processor.process(input, context);
        assertThat(processedInput).isEqualTo("hello hello  ");
    }
}
