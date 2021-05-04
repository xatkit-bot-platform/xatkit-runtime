package com.xatkit.core.recognition.processor;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;
import com.vdurmont.emoji.EmojiParser;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.RecognizedIntent;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Detects the Emojis of the provided {@link RecognizedIntent}.
 * <p>
 * This processor stores in {@link RecognizedIntent#getNlpData()} a {@link Set} containing a {@link EmojiData} for
 * each different emoji found in the given {@link RecognizedIntent}, that can be accessed through the
 * {@link #EMOJI_DATA_SET_PARAMETER_KEY} key.
 *
 * Some emojis are available in the Emoji Sentiment Ranking, which is stored in {@link #EMOJI_SENTIMENT_RANKING_FILE}.
 * These emojis have extra attributes related to its sentiment in its respective {@link EmojiData}.
 *
 * Note that 2 identical emojis with different skin tones are considered as different emojis, thus 2 different
 * {@link EmojiData} objects will be stored in the {@link Set}
 *
 * @see #process(RecognizedIntent, StateContext)
 * @see EmojiData
 */
public class EmojiPostProcessor implements IntentPostProcessor {

    /**
     * The NLP-data key to access the set containing all EmojiData objects.
     *
     * @see EmojiData
     * @see RecognizedIntent#getNlpData()
     */
    public static final String EMOJI_DATA_SET_PARAMETER_KEY = "nlp.emoji.emojiDataSet";

    /**
     * The name of the file containing the entries of the Emoji Sentiment Ranking.
     */
    protected static final String EMOJI_SENTIMENT_RANKING_FILE = "Emoji_Sentiment_Data_v1.0.csv";

    /**
     * The in-memory {@link Map} of emoji information parsed from the corresponding file.
     * Keys are the unicodes of the emojis and values are the information available in the corresponding file
     *
     * @see #EMOJI_SENTIMENT_RANKING_FILE
     */
    private Map<String, String>  emojiSentimentRanking;

    /**
     * Initializes the {@link EmojiPostProcessor}.
     */
    public EmojiPostProcessor() {
        String emojisData = "";
        try (InputStream inputStream =
                this.getClass().getClassLoader().getResourceAsStream(EMOJI_SENTIMENT_RANKING_FILE)) {
            if (isNull(inputStream)) {
                Log.error("Cannot find the file {0}, this processor won't get any emoji sentiment data",
                        EMOJI_SENTIMENT_RANKING_FILE);
            } else {
                emojisData = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            Log.error("An error occurred when processing the emoji database {0}, this processor may produce "
                    + "unexpected behavior. Check the logs for more information.", EMOJI_SENTIMENT_RANKING_FILE);
        }
        emojiSentimentRanking = new HashMap<>();
        List<String> emojisLines = Arrays.asList(emojisData.split("\n"));
        for (String line : emojisLines.subList(1, emojisLines.size())) {
            String[] splitLine = line.split(",", 2);
            String unicode = splitLine[0];
            String data = splitLine[1];
            emojiSentimentRanking.put(unicode, data);
        }
        Log.debug("Loaded {0} emojis from {1}", emojiSentimentRanking.size(), EMOJI_SENTIMENT_RANKING_FILE);
    }

    /**
     * Detects the emojis from the provided {@code recognizedIntent}.
     * <p>
     * For each different emoji is created a {@link EmojiData} object. A {@link Set} containing all of them is
     * stored in {@link RecognizedIntent#getNlpData()}, and can be accessed through the
     * {@link #EMOJI_DATA_SET_PARAMETER_KEY} key.
     *
     * @param recognizedIntent the {@link RecognizedIntent} to process
     * @param context          the {@link StateContext} associated to the {@code recognizedIntent}
     * @return the updated {@code recognizedIntent}
     * @see EmojiData
     * @see #EMOJI_DATA_SET_PARAMETER_KEY
     */
    @Override
    public RecognizedIntent process(RecognizedIntent recognizedIntent, StateContext context) {
        String text = recognizedIntent.getMatchedInput();
        Set<EmojiData> emojis = new HashSet<>();
        if (EmojiManager.containsEmoji(text)) {
            Set<String> emojisInTextSet = new TreeSet<>(EmojiParser.extractEmojis(text)).descendingSet();
            for (String e : emojisInTextSet) {
                String alias = EmojiParser.parseToAliases(e, EmojiParser.FitzpatrickAction.IGNORE);
                EmojiData.EmojiDataBuilder emojiDataBuilder = EmojiData.builder();
                if (alias.lastIndexOf(":") != alias.length() - 1) {
                    String skinTone = alias.substring(alias.length() - 2);
                    emojiDataBuilder.skinTone(skinTone);
                }
                String aliasWithoutSkinTone = alias.substring(0, alias.indexOf(":", 1))
                        .substring(alias.indexOf(":", 0) + 1);
                Emoji emoji = EmojiManager.getForAlias(aliasWithoutSkinTone);
                String unicode = emoji.getUnicode();
                emojiDataBuilder.unicode(unicode);
                emojiDataBuilder.aliases(emoji.getAliases());
                emojiDataBuilder.tags(emoji.getTags());
                emojiDataBuilder.supportsSkinTone(emoji.supportsFitzpatrick());
                emojiDataBuilder.description(emoji.getDescription());
                List<Integer> positionsInText = getEmojiPositionsInText(text, e, emojisInTextSet);
                emojiDataBuilder.positions(positionsInText);

                String line = emojiSentimentRanking.get(unicode);
                if (nonNull(line)) {
                    /*
                     * Add sentiment analysis information if there is an entry corresponding to the current emoji in
                     * the EMOJI_SENTIMENT_RANKING_FILE.
                     */
                    String[] columns = line.split(",");
                    int frequencyInSentimentRanking = Integer.parseInt(columns[1]);
                    emojiDataBuilder.frequencyInSentimentRanking(frequencyInSentimentRanking);
                    int negativeCount = Integer.parseInt(columns[3]);
                    int neutralCount = Integer.parseInt(columns[4]);
                    int positiveCount = Integer.parseInt(columns[5]);
                    double negativeSentiment = (double) negativeCount / frequencyInSentimentRanking;
                    emojiDataBuilder.negativeSentiment(negativeSentiment);
                    double neutralSentiment = (double) neutralCount / frequencyInSentimentRanking;
                    emojiDataBuilder.neutralSentiment(neutralSentiment);
                    double positiveSentiment = (double) positiveCount / frequencyInSentimentRanking;
                    emojiDataBuilder.positiveSentiment(positiveSentiment);
                    String unicodeBlock = columns[7].toLowerCase();
                    unicodeBlock = unicodeBlock.substring(0, unicodeBlock.length() - 1);
                    emojiDataBuilder.unicodeBlock(unicodeBlock);
                }
                emojis.add(emojiDataBuilder.build());
            }
        }
        recognizedIntent.getNlpData().put(EMOJI_DATA_SET_PARAMETER_KEY, emojis);
        return recognizedIntent;
    }

    /**
     * Gets the positions of a target emoji in a given text
     * <p>
     * Note that most emojis occupy more than 1 character size (i.e. an emoji length can be > 1). So positions are the
     * ones that you can get doing {@code stringValue.indexOf(emoji)}
     *
     * @param text            the {@link String} from which to extract the emoji positions
     * @param targetEmoji     the unicode of the emoji from which to get the positions in the {@code text}
     * @param emojisInTextSet the {@link Set} containing all the emojis as unicodes in the {@code text}
     * @return the list containing the positions of all {@code targetEmoji} in {@code text}
     */
    private List<Integer> getEmojiPositionsInText(String text, String targetEmoji, Set<String> emojisInTextSet) {
        /*
         * This method is more complicated than it looks because some emojis with skin tones are composed of multiple
         * code points. This means that we cannot search for the targetEmoji directly, otherwise we may catch some
         * emojis that are variations of the provided one.
         */
        List<Integer> positions = new ArrayList<>();
        for (String emoji : emojisInTextSet) {
            while (text.contains(emoji)) {
                if (emoji.equals(targetEmoji)) {
                    positions.add(text.indexOf(emoji));
                }
                int emojiLength = emoji.length();
                String replacement = StringUtils.repeat(".", emojiLength);
                text = text.replaceFirst(emoji, replacement);
            }
            if (emoji.equals(targetEmoji)) {
                break;
            }
        }
        return positions;
    }

}
