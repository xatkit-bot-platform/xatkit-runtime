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
                .description("hugging face");


        // Attributes of 🙌
        emojiDataHandsBuilder = EmojiData.builder()
                .unicode("\uD83D\uDE4C")
                .alias("raised_hands")
                .tag("hooray")
                .supportsSkinTone(true)
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
                .position(6)
                .position(19)
                .build();

        EmojiData handsLightEmoji = emojiDataHandsLightBuilder
                .position(9)
                .position(22)
                .build();

        EmojiData handsDarkEmoji = emojiDataHandsDarkBuilder
                .position(14)
                .position(27)
                .build();

        EmojiData hugEmoji = emojiDataHugBuilder
                .position(39)
                .build();

        assertThat(emojiDataSet).anyMatch(e -> e.equals(handsEmoji));
        assertThat(emojiDataSet).anyMatch(e -> e.equals(handsLightEmoji));
        assertThat(emojiDataSet).anyMatch(e -> e.equals(handsDarkEmoji));
        assertThat(emojiDataSet).anyMatch(e -> e.equals(hugEmoji));
    }
}
