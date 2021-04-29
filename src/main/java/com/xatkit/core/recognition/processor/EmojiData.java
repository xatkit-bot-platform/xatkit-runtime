package com.xatkit.core.recognition.processor;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Stores the data of an emoji.
 * It is used by {@link EmojiPostProcessor} to store information about emojis found in intents.
 * <p>
 * The {@link EmojiData#positiveSentiment}, {@link EmojiData#neutralSentiment} and {@link EmojiData#negativeSentiment}
 * scores refer to the positive, neutral and negative sentiment of the emoji, respectively. These scores are in the
 * [0,1] interval, being 0 the lowest possible value and 1 the highest one. The sum of the 3 values is always 1.
 * This class contains positive, neutral and negative score (instead of a single generic) to allow the bot designers
 * to finely tune how sentiments from emojis are handled by their bot.
 *
 * @see EmojiPostProcessor
 */
public class EmojiData {

    /**
     * The unicode of the emoji
     */
    @Getter
    private String unicode;

    /**
     * The aliases of the emoji
     */
    @Getter
    private Set<String> aliases = new HashSet<>();

    /**
     * The tags of the emoji
     */
    @Getter
    private Set<String> tags = new HashSet<>();

    /**
     * {@code true} if the emoji supports skin tone. Otherwise {@code false}
     */
    @Getter
    private boolean supportsSkinTone;

    /**
     * The skin tone of the emoji. If the emoji doesn't have skin tone, its value is
     * {@link EmojiPostProcessor#UNSET_STRING}
     */
    @Getter
    private String skinTone;

    /**
     * The description of the emoji
     */
    @Getter
    private String description;

    /**
     * The unicode block or category of the emoji (e.g. "Emoticons", "Miscellaneous Symbols and Pictographs").
     * <p>
     * If the emoji is not present in the file {@link EmojiPostProcessor#EMOJI_SENTIMENT_RANKING_FILE}, its value is
     * {@link EmojiPostProcessor#UNSET_STRING}
     */
    @Getter
    private String unicodeBlock;

    /**
     * The frequency of the emoji in the Emoji Sentiment Ranking. It can be used to ignore sentiment values with a
     * low frequency (they might be considered untruthful)
     * <p>
     * If the emoji is not present in the file {@link EmojiPostProcessor#EMOJI_SENTIMENT_RANKING_FILE}, its value is
     * {@link EmojiPostProcessor#UNSET_FREQUENCY}
     */
    @Getter
    private int frequencyInSentimentRanking;

    /**
     * The negative sentiment of the emoji in the Emoji Sentiment Ranking, which is in the [0,1] interval, being 0 the
     * lowest negative sentiment value and 1 the highest one.
     * <p>
     * If the emoji is not present in the file {@link EmojiPostProcessor#EMOJI_SENTIMENT_RANKING_FILE}, its value is
     * {@link EmojiPostProcessor#UNSET_SENTIMENT}
     */
    @Getter
    private double negativeSentiment;

    /**
     * The neutral sentiment of the emoji in the Emoji Sentiment Ranking, which is in the [0,1] interval, being 0 the
     * lowest neutral sentiment value and 1 the highest one.
     * <p>
     * If the emoji is not present in the file {@link EmojiPostProcessor#EMOJI_SENTIMENT_RANKING_FILE}, its value is
     * {@link EmojiPostProcessor#UNSET_SENTIMENT}
     */
    @Getter
    private double neutralSentiment;

    /**
     * The positive sentiment of the emoji in the Emoji Sentiment Ranking, which is in the [0,1] interval, being 0 the
     * lowest positive sentiment value and 1 the highest one.
     * <p>
     * If the emoji is not present in the file {@link EmojiPostProcessor#EMOJI_SENTIMENT_RANKING_FILE}, its value is
     * {@link EmojiPostProcessor#UNSET_SENTIMENT}
     */
    @Getter
    private double positiveSentiment;

    /**
     * The number of occurrences of the emoji in the given text
     */
    @Getter
    private int occurrences;

    /**
     * The positions of the emoji in the given text
     */
    @Getter
    private List<Integer> positionsInText = new ArrayList<>();

    /**
     * Instantiates a new EmojiData.
     *
     * @param unicode                     the unicode of the emoji
     * @param aliases                     the aliases of the emoji
     * @param tags                        the tags of the emoji
     * @param supportsSkinTone            whether this emoji supports skin tone or not
     * @param skinTone                    the skin tone of the emoji
     * @param description                 the description of the emoji
     * @param unicodeBlock                the unicode block (category) of the emoji
     * @param frequencyInSentimentRanking the frequency in sentiment ranking of the emoji
     * @param negativeSentiment           the negative sentiment of the emoji
     * @param neutralSentiment            the neutral sentiment of the emoji
     * @param positiveSentiment           the positive sentiment of the emoji
     * @param occurrences                 the number of occurrences of the emoji in the text
     * @param positionsInText             the positions of the emoji in the text
     */
    public EmojiData(String unicode, Set<String> aliases, Set<String> tags, boolean supportsSkinTone, String skinTone,
                     String description, String unicodeBlock, int frequencyInSentimentRanking, double negativeSentiment,
                     double neutralSentiment, double positiveSentiment, int occurrences, List<Integer> positionsInText){
        this.unicode = unicode;
        this.aliases = aliases;
        this.tags = tags;
        this.supportsSkinTone = supportsSkinTone;
        this.skinTone = skinTone;
        this.description = description;
        this.unicodeBlock = unicodeBlock;
        this.frequencyInSentimentRanking = frequencyInSentimentRanking;
        this.negativeSentiment = negativeSentiment;
        this.neutralSentiment = neutralSentiment;
        this.positiveSentiment = positiveSentiment;
        this.occurrences = occurrences;
        this.positionsInText = positionsInText;
    }
}
