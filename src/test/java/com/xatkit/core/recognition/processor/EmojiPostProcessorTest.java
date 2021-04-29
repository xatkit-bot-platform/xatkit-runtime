package com.xatkit.core.recognition.processor;

import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.RecognizedIntent;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class EmojiPostProcessorTest {

    private EmojiPostProcessor processor;
    private StateContext context;

    private EmojiData.EmojiDataBuilder emojiDataHugBuilder;
    private EmojiData.EmojiDataBuilder emojiDataHandsBuilder;
    private EmojiData.EmojiDataBuilder emojiDataHandsLightBuilder;
    private EmojiData.EmojiDataBuilder emojiDataHandsDarkBuilder;

    private void setEmojisHardcoded() {
        // Positions and counts of the emojis are the ones for the intent in testRepeatedEmojisWithSkinTonesAndText

        // Attributes of 🤗
        emojiDataHugBuilder = EmojiData.builder()
                .unicode("\uD83E\uDD17")
                .alias("hugs")
                .alias("hug")
                .alias("hugging")
                .supportsSkinTone(false)
                .skinTone(EmojiPostProcessor.NULL_STRING)
                .description("hugging face")
                .unicodeBlock(EmojiPostProcessor.NULL_STRING)
                .frequencyInSentimentRanking(EmojiPostProcessor.UNSET_FREQUENCY)
                .negativeSentiment(EmojiPostProcessor.UNSET_SENTIMENT)
                .neutralSentiment(EmojiPostProcessor.UNSET_SENTIMENT)
                .positiveSentiment(EmojiPostProcessor.UNSET_SENTIMENT);


        // Attributes of 🙌
        emojiDataHandsBuilder = EmojiData.builder()
                .unicode("\uD83D\uDE4C")
                .alias("raised_hands")
                .tag("hooray")
                .supportsSkinTone(true)
                .skinTone(EmojiPostProcessor.NULL_STRING)
                .description("person raising both hands in celebration")
                .unicodeBlock("emoticons")
                .frequencyInSentimentRanking(1506)
                .negativeSentiment(0.10092961487383798)
                .neutralSentiment(0.23771580345285526)
                .positiveSentiment(0.6613545816733067);


        // Attributes of 🙌🏿
        emojiDataHandsDarkBuilder = EmojiData.builder()
                .unicode("\uD83D\uDE4C")
                .alias("raised_hands")
                .tag("hooray")
                .supportsSkinTone(true)
                .skinTone("\uD83C\uDFFF")
                .description("person raising both hands in celebration")
                .unicodeBlock("emoticons")
                .frequencyInSentimentRanking(1506)
                .negativeSentiment(0.10092961487383798)
                .neutralSentiment(0.23771580345285526)
                .positiveSentiment(0.6613545816733067);


        // Attributes of 🙌🏻
        emojiDataHandsLightBuilder = EmojiData.builder()
                .unicode("\uD83D\uDE4C")
                .alias("raised_hands")
                .tag("hooray")
                .supportsSkinTone(true)
                .skinTone("\uD83C\uDFFB")
                .description("person raising both hands in celebration")
                .unicodeBlock("emoticons")
                .frequencyInSentimentRanking(1506)
                .negativeSentiment(0.10092961487383798)
                .neutralSentiment(0.23771580345285526)
                .positiveSentiment(0.6613545816733067);
    }

    @Before
    public void setUp() {
        this.processor = null;
        this.context = ExecutionFactory.eINSTANCE.createStateContext();
        this.context.setContextId("contextId");
        setEmojisHardcoded();
    }

    @Test
    public void testSingleEmoji() {
        processor = new EmojiPostProcessor();
        RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
        recognizedIntent.setMatchedInput("\uD83D\uDE4C\uD83C\uDFFB"); // 🙌🏻
        RecognizedIntent processedIntent = processor.process(recognizedIntent, context);
        Set<EmojiData> emojiDataSet =
                (Set<EmojiData>) processedIntent.getNlpData().get(EmojiPostProcessor.EMOJI_DATA_SET_PARAMETER_KEY);
        assertThat(emojiDataSet.size()).isEqualTo(1);
        EmojiData e = emojiDataSet.iterator().next();
        EmojiData expectedEmojiData = emojiDataHandsLightBuilder
                .occurrences(1)
                .position(0)
                .build();
        assertThat(e).isEqualTo(expectedEmojiData);
    }

    @Test
    public void testTextWithoutEmojis() {
        processor = new EmojiPostProcessor();
        RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
        recognizedIntent.setMatchedInput("test");
        RecognizedIntent processedIntent = processor.process(recognizedIntent, context);
        HashSet<EmojiData> emojiDataSet =
                (HashSet<EmojiData>) processedIntent.getNlpData().get(EmojiPostProcessor.EMOJI_DATA_SET_PARAMETER_KEY);
        assertThat(emojiDataSet.size()).isEqualTo(0);
    }

    @Test
    public void testRepeatedEmojisWithSkinTonesAndText() {
        processor = new EmojiPostProcessor();
        RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
        // hello 🙌 🙌🏻 🙌🏿 🙌 🙌🏻 🙌🏿 friend 🤗
        recognizedIntent.setMatchedInput("hello \uD83D\uDE4C \uD83D\uDE4C\uD83C\uDFFB \uD83D\uDE4C\uD83C\uDFFF "
                + "\uD83D\uDE4C \uD83D\uDE4C\uD83C\uDFFB \uD83D\uDE4C\uD83C\uDFFF friend \uD83E\uDD17");
        RecognizedIntent processedIntent = processor.process(recognizedIntent, context);
        Set<EmojiData> emojiDataSet =
                (Set<EmojiData>) processedIntent.getNlpData().get(EmojiPostProcessor.EMOJI_DATA_SET_PARAMETER_KEY);

        assertThat(emojiDataSet).hasSize(4);

        EmojiData handsEmoji = emojiDataHandsBuilder
                .occurrences(2)
                .position(6)
                .position(12)
                .build();

        EmojiData handsLightEmoji = emojiDataHandsLightBuilder
                .occurrences(2)
                .position(8)
                .position(14)
                .build();

        EmojiData handsDarkEmoji = emojiDataHandsDarkBuilder
                .occurrences(2)
                .position(10)
                .position(16)
                .build();

        EmojiData hugEmoji = emojiDataHugBuilder
                .occurrences(1)
                .position(25)
                .build();

        assertThat(emojiDataSet).anyMatch(e -> e.equals(handsEmoji));
        assertThat(emojiDataSet).anyMatch(e -> e.equals(handsLightEmoji));
        assertThat(emojiDataSet).anyMatch(e -> e.equals(handsDarkEmoji));
        assertThat(emojiDataSet).anyMatch(e -> e.equals(hugEmoji));
    }
}
