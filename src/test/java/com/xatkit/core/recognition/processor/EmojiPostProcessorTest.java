package com.xatkit.core.recognition.processor;

import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.RecognizedIntent;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class EmojiPostProcessorTest {

    private EmojiPostProcessor processor;
    private StateContext context;
    private EmojiData emojiDataHug;
    private EmojiData emojiDataHands;
    private EmojiData emojiDataHandsDark;
    private EmojiData emojiDataHandsLight;

    private void setEmojisHardcoded() {
        // Positions and counts of the emojis are the ones for the intent in testRepeatedEmojisWithSkinTonesAndText

        // Attributes of 🤗
        String unicode = "\uD83E\uDD17";
        Set<String> aliases = new HashSet<String>();
        aliases.add("hugs");
        aliases.add("hug");
        aliases.add("hugging");
        Set<String> tags = new HashSet<String>();
        boolean supportsSkinTone = false;
        String skinTone = EmojiPostProcessor.NULL_STRING;
        String description = "hugging face";
        String unicodeBlock = EmojiPostProcessor.NULL_STRING;
        int frequencyInSentimentRanking = EmojiPostProcessor.UNSET_FREQUENCY;
        double negativeSentiment = EmojiPostProcessor.UNSET_SENTIMENT;
        double neutralSentiment = EmojiPostProcessor.UNSET_SENTIMENT;
        double positiveSentiment = EmojiPostProcessor.UNSET_SENTIMENT;
        int occurrences = 1;
        List<Integer> positionsInText = new ArrayList<Integer>();
        positionsInText.add(25);

        emojiDataHug = new EmojiData(unicode, aliases, tags, supportsSkinTone, skinTone, description,
                unicodeBlock, frequencyInSentimentRanking, negativeSentiment, neutralSentiment, positiveSentiment,
                occurrences, positionsInText);

        // Attributes of 🙌
        unicode = "\uD83D\uDE4C";
        aliases = new HashSet<>();;
        aliases.add("raised_hands");
        tags = new HashSet<>();
        tags.add("hooray");
        supportsSkinTone = true;
        skinTone = EmojiPostProcessor.NULL_STRING;
        description = "person raising both hands in celebration";
        unicodeBlock = "emoticons";
        frequencyInSentimentRanking = 1506;
        negativeSentiment = 0.10092961487383798;
        neutralSentiment = 0.23771580345285526;
        positiveSentiment = 0.6613545816733067;
        occurrences = 2;
        positionsInText = new ArrayList<>();
        positionsInText.add(6);
        positionsInText.add(12);

        emojiDataHands = new EmojiData(unicode, aliases, tags, supportsSkinTone, skinTone, description,
                unicodeBlock, frequencyInSentimentRanking, negativeSentiment, neutralSentiment, positiveSentiment,
                occurrences, positionsInText);

        // Attributes of 🙌🏿
        unicode = "\uD83D\uDE4C";
        aliases = new HashSet<>();;
        aliases.add("raised_hands");
        tags = new HashSet<>();
        tags.add("hooray");
        supportsSkinTone = true;
        skinTone = "\uD83C\uDFFF";
        description = "person raising both hands in celebration";
        unicodeBlock = "emoticons";
        frequencyInSentimentRanking = 1506;
        negativeSentiment = 0.10092961487383798;
        neutralSentiment = 0.23771580345285526;
        positiveSentiment = 0.6613545816733067;
        occurrences = 2;
        positionsInText = new ArrayList<>();
        positionsInText.add(10);
        positionsInText.add(16);

        emojiDataHandsDark = new EmojiData(unicode, aliases, tags, supportsSkinTone, skinTone, description,
                unicodeBlock, frequencyInSentimentRanking, negativeSentiment, neutralSentiment, positiveSentiment,
                occurrences, positionsInText);

        // Attributes of 🙌🏻
        unicode = "\uD83D\uDE4C";
        aliases = new HashSet<>();;
        aliases.add("raised_hands");
        tags = new HashSet<>();
        tags.add("hooray");
        supportsSkinTone = true;
        skinTone = "\uD83C\uDFFB";
        description = "person raising both hands in celebration";
        unicodeBlock = "emoticons";
        frequencyInSentimentRanking = 1506;
        negativeSentiment = 0.10092961487383798;
        neutralSentiment = 0.23771580345285526;
        positiveSentiment = 0.6613545816733067;
        occurrences = 2;
        positionsInText = new ArrayList<>();
        positionsInText.add(8);
        positionsInText.add(14);

        emojiDataHandsLight = new EmojiData(unicode, aliases, tags, supportsSkinTone, skinTone, description,
                unicodeBlock, frequencyInSentimentRanking, negativeSentiment, neutralSentiment, positiveSentiment,
                occurrences, positionsInText);
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
        assertThat(e.getAliases()).isEqualTo(emojiDataHands.getAliases());
        assertThat(e.getTags()).isEqualTo(emojiDataHands.getTags());
        assertThat(e.isSupportsSkinTone()).isEqualTo(emojiDataHands.isSupportsSkinTone());
        assertThat(e.getDescription()).isEqualTo(emojiDataHands.getDescription());
        assertThat(e.getUnicodeBlock()).isEqualTo(emojiDataHands.getUnicodeBlock());
        assertThat(e.getFrequencyInSentimentRanking()).isEqualTo(emojiDataHands.getFrequencyInSentimentRanking());
        assertThat(e.getNegativeSentiment()).isEqualTo(emojiDataHands.getNegativeSentiment());
        assertThat(e.getNeutralSentiment()).isEqualTo(emojiDataHands.getNeutralSentiment());
        assertThat(e.getPositiveSentiment()).isEqualTo(emojiDataHands.getPositiveSentiment());
        assertThat(e.getSkinTone()).isEqualTo(emojiDataHandsLight.getSkinTone());
        assertThat(e.getOccurrences()).isEqualTo(1);
        List<Integer> positions = new ArrayList<>();
        positions.add(0);
        assertThat(e.getPositionsInText()).isEqualTo(positions);

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
        boolean visitedHug = false;
        boolean visitedHands = false;
        boolean visitedHandsDark = false;
        boolean visitedHandsLight = false;
        for (EmojiData e : emojiDataSet) {
            switch (e.getUnicode()) {
                case "\uD83D\uDE4C": // 🙌
                    assertThat(e.getAliases()).isEqualTo(emojiDataHands.getAliases());
                    assertThat(e.getTags()).isEqualTo(emojiDataHands.getTags());
                    assertThat(e.isSupportsSkinTone()).isEqualTo(emojiDataHands.isSupportsSkinTone());
                    assertThat(e.getDescription()).isEqualTo(emojiDataHands.getDescription());
                    assertThat(e.getUnicodeBlock()).isEqualTo(emojiDataHands.getUnicodeBlock());
                    assertThat(e.getFrequencyInSentimentRanking())
                            .isEqualTo(emojiDataHands.getFrequencyInSentimentRanking());
                    assertThat(e.getNegativeSentiment()).isEqualTo(emojiDataHands.getNegativeSentiment());
                    assertThat(e.getNeutralSentiment()).isEqualTo(emojiDataHands.getNeutralSentiment());
                    assertThat(e.getPositiveSentiment()).isEqualTo(emojiDataHands.getPositiveSentiment());
                    if (e.getSkinTone().equals("\uD83C\uDFFF")) { // 🏿
                        assertThat(e.getSkinTone()).isEqualTo(emojiDataHandsDark.getSkinTone());
                        assertThat(e.getOccurrences()).isEqualTo(emojiDataHandsDark.getOccurrences());
                        assertThat(e.getPositionsInText()).isEqualTo(emojiDataHandsDark.getPositionsInText());
                        visitedHandsDark = true;
                    }
                    else if (e.getSkinTone().equals("\uD83C\uDFFB")) { // 🏻
                        assertThat(e.getSkinTone()).isEqualTo(emojiDataHandsLight.getSkinTone());
                        assertThat(e.getOccurrences()).isEqualTo(emojiDataHandsLight.getOccurrences());
                        assertThat(e.getPositionsInText()).isEqualTo(emojiDataHandsLight.getPositionsInText());
                        visitedHandsLight = true;
                    }
                    else { // without skin tone
                        assertThat(e.getSkinTone()).isEqualTo(emojiDataHands.getSkinTone());
                        assertThat(e.getOccurrences()).isEqualTo(emojiDataHands.getOccurrences());
                        assertThat(e.getPositionsInText()).isEqualTo(emojiDataHands.getPositionsInText());
                        visitedHands = true;
                    }
                    break;
                case "\uD83E\uDD17": // 🤗
                    assertThat(e.getAliases()).isEqualTo(emojiDataHug.getAliases());
                    assertThat(e.getTags()).isEqualTo(emojiDataHug.getTags());
                    assertThat(e.isSupportsSkinTone()).isEqualTo(emojiDataHug.isSupportsSkinTone());
                    assertThat(e.getDescription()).isEqualTo(emojiDataHug.getDescription());
                    assertThat(e.getUnicodeBlock()).isEqualTo(emojiDataHug.getUnicodeBlock());
                    assertThat(e.getFrequencyInSentimentRanking())
                            .isEqualTo(emojiDataHug.getFrequencyInSentimentRanking());
                    assertThat(e.getNegativeSentiment()).isEqualTo(emojiDataHug.getNegativeSentiment());
                    assertThat(e.getNeutralSentiment()).isEqualTo(emojiDataHug.getNeutralSentiment());
                    assertThat(e.getPositiveSentiment()).isEqualTo(emojiDataHug.getPositiveSentiment());
                    assertThat(e.getSkinTone()).isEqualTo(emojiDataHug.getSkinTone());
                    assertThat(e.getOccurrences()).isEqualTo(emojiDataHug.getOccurrences());
                    assertThat(e.getPositionsInText()).isEqualTo(emojiDataHug.getPositionsInText());
                    visitedHug = true;
                    break;
            }
        }
        assertThat(visitedHug).isTrue();
        assertThat(visitedHands).isTrue();
        assertThat(visitedHandsDark).isTrue();
        assertThat(visitedHandsLight).isTrue();

    }


}
