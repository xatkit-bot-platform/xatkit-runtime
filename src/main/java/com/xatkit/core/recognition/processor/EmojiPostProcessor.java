package com.xatkit.core.recognition.processor;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;
import com.vdurmont.emoji.EmojiParser;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.RecognizedIntent;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.io.IOUtils;

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
     * The default value for an emoji without skin tone
     * <p>
     * This value is used as a placeholder when an emoji doesn't have skin tone (even if it can have it).
     */
    public static final String NULL_STRING = "";

    /**
     * The default value for negative, neutral and positive sentiments.
     * <p>
     * This value is used as a placeholder when an emoji is not present in the Emoji Sentiment Ranking.
     */
    public static final Double UNSET_SENTIMENT = -1d;

    /**
     * The default value for frequency in sentiment ranking.
     * <p>
     * This value is used as a placeholder when an emoji is not present in the Emoji Sentiment Ranking.
     */
    public static final int UNSET_FREQUENCY = -1;

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
    private Map<String,String>  emojiSentimentRanking;

    /**
     * Initializes the {@link EmojiPostProcessor}.
     */
    public EmojiPostProcessor() {
        InputStream inputStream =
                EmojiPostProcessor.class.getClassLoader().getResourceAsStream(EMOJI_SENTIMENT_RANKING_FILE);
        String emojisData = "";
        if (isNull(inputStream)) {
            Log.error("Cannot find the file {0}, this processor won't get any emoji sentiment data",
                    EMOJI_SENTIMENT_RANKING_FILE);
        } else {
            try {
                emojisData = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            } catch (IOException e) {
                Log.error(e, "An error occurred when loading the file {0}, this processors won't get any emoji "
                        + "sentiment data. See attached exception:", EMOJI_SENTIMENT_RANKING_FILE);
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.error(e, "An error occurred when closing the file {0}, see attached exception",
                            EMOJI_SENTIMENT_RANKING_FILE);
                }
            }
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
            Set<String> visitedEmojis = new HashSet<>();
            Set<String> emojisInTextSet = new TreeSet<>(EmojiParser.extractEmojis(text)).descendingSet();
            for (String e : emojisInTextSet) {
                String alias = EmojiParser.parseToAliases(e, EmojiParser.FitzpatrickAction.IGNORE);
                if (visitedEmojis.add(alias)) {
                    String skinTone = NULL_STRING;
                    if (alias.lastIndexOf(":") != alias.length() - 1) {
                        skinTone = alias.substring(alias.length()-2);
                    }
                    String aliasWithoutSkinTone = alias.substring(0,alias.indexOf(":", 1))
                            .substring(alias.indexOf(":", 0) + 1);
                    Emoji emoji = EmojiManager.getForAlias(aliasWithoutSkinTone);
                    String unicode = emoji.getUnicode();
                    Set<String> aliases = new HashSet<>(emoji.getAliases());
                    Set<String> tags = new HashSet<>(emoji.getTags());
                    boolean supportsSkinTone = emoji.supportsFitzpatrick();
                    String description = emoji.getDescription();
                    List<Integer> positionsInText = getEmojiPositionsInText(text, e, emojisInTextSet);
                    int occurrences = positionsInText.size();
                    String unicodeBlock = NULL_STRING;
                    int frequencyInSentimentRanking = UNSET_FREQUENCY;
                    double negativeSentiment = UNSET_SENTIMENT;
                    double neutralSentiment = UNSET_SENTIMENT;
                    double positiveSentiment = UNSET_SENTIMENT;
                    String line = emojiSentimentRanking.get(unicode);
                    if (nonNull(line)) {
                        String[] columns = line.split(",");
                        frequencyInSentimentRanking = Integer.parseInt(columns[1]);
                        int negativeCount = Integer.parseInt(columns[3]);
                        int neutralCount = Integer.parseInt(columns[4]);
                        int positiveCount = Integer.parseInt(columns[5]);
                        negativeSentiment = (double) negativeCount / frequencyInSentimentRanking;
                        neutralSentiment = (double) neutralCount / frequencyInSentimentRanking;
                        positiveSentiment = (double) positiveCount / frequencyInSentimentRanking;
                        unicodeBlock = columns[7].toLowerCase();
                        unicodeBlock = unicodeBlock.substring(0, unicodeBlock.length()-1);
                    }
                    EmojiData emojiData = new EmojiData(unicode, aliases, tags, supportsSkinTone, skinTone,
                            description, unicodeBlock, frequencyInSentimentRanking, negativeSentiment,
                            neutralSentiment, positiveSentiment, occurrences, positionsInText);
                    emojis.add(emojiData);
                }
            }
        }
        recognizedIntent.getNlpData().put(EMOJI_DATA_SET_PARAMETER_KEY, emojis);
        return recognizedIntent;
    }

    /**
     * Gets the positions of a target emoji in a given text
     *
     * @param text            the {@link String} from which to extract the emoji positions
     * @param targetEmoji     the unicode of the emoji from which to get the positions in the {@code text}
     * @param emojisInTextSet the {@link Set} containing all the emojis as unicodes in the {@code text}
     * @return the list containing the positions of all {@code targetEmoji} in {@code text}
     */
    private List<Integer> getEmojiPositionsInText(String text, String targetEmoji, Set<String> emojisInTextSet) {
        List<Integer> positions = new ArrayList<>();
        int targetOccurrences = 0;
        boolean targetRead = false;
        for (String emoji : emojisInTextSet) {
            if (emoji.equals(targetEmoji)) {
                while (text.contains(emoji)) {
                    positions.add(text.indexOf(emoji));
                    text = text.replaceFirst(emoji, ".");
                }
                targetRead = true;
                targetOccurrences = positions.size();
            }
            else {
                if (targetRead) {
                    int eLength = emoji.length();
                    while (text.contains(emoji)) {
                        int index = text.indexOf(emoji);
                        if (index < positions.get(targetOccurrences-1)) {
                            for (int i = 0; i < positions.size(); i++) {
                                int pos = positions.get(i);
                                if (pos > index) {
                                    positions.set(i, pos - eLength + 1);
                                }
                            }
                        }
                        text = text.replaceFirst(emoji, ".");
                    }
                }
                else {
                    text = text.replaceAll(emoji, ".");
                }
            }
        }
        return positions;
    }

}
