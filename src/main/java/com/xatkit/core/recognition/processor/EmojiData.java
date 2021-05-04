package com.xatkit.core.recognition.processor;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;

import javax.annotation.Nullable;
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
@Builder
@EqualsAndHashCode
public class EmojiData {

    /**
     * The unicode of the emoji.
     */
    @Getter
    private String unicode;

    /**
     * The aliases of the emoji.
     */
    @Singular
    @Getter
    private Set<String> aliases;

    /**
     * The tags of the emoji.
     */
    @Singular
    @Getter
    private Set<String> tags;

    /**
     * Whether the emoji supports skin tone.
     */
    @Getter
    private boolean supportsSkinTone;

    /**
     * The skin tone of the emoji.
     */
    @Getter
    @Nullable
    private String skinTone;

    /**
     * The description of the emoji.
     */
    @Getter
    private String description;

    /**
     * The unicode block or category of the emoji (e.g. "Emoticons", "Miscellaneous Symbols and Pictographs").
     */
    @Getter
    @Nullable
    private String unicodeBlock;

    /**
     * The frequency of the emoji in the Emoji Sentiment Ranking. It can be used to ignore sentiment values with a
     * low frequency (they might be considered untruthful)
     * <p>
     * If the emoji is not present in the file {@link EmojiPostProcessor#EMOJI_SENTIMENT_RANKING_FILE}, its value is
     * {@code 0}.
     */
    @Getter
    private int frequencyInSentimentRanking;

    /**
     * The negative sentiment of the emoji in the Emoji Sentiment Ranking, which is in the [0,1] interval.
     * <p>
     * A score of 0 means that the emoji is not likely negative. It doesn't say anything on the other scores.
     * Conversely, a score of 1 means that the emoji is likely to be negative.
     * <p>
     * This score is set by default to {@code 0}.
     */
    @Getter
    private double negativeSentiment;

    /**
     * The neutral sentiment of the emoji in the Emoji Sentiment Ranking, which is in the [0,1] interval.
     * <p>
     * A score of 0 means that the emoji is not likely neutral. It doesn't say anything on the other scores.
     * Conversely, a score of 1 means that the emoji is likely to be neutral.
     * <p>
     * This score is set by default to {@code 0}.
     */
    @Getter
    private double neutralSentiment;

    /**
     * The positive sentiment of the emoji in the Emoji Sentiment Ranking, which is in the [0,1] interval.
     * <p>
     * A score of 0 means that the emoji is not likely positive. It doesn't say anything on the other scores.
     * Conversely, a score of 1 means that the emoji is likely to be positive.
     * <p>
     * This score is set by default to {@code 0}.
     */
    @Getter
    private double positiveSentiment;

    /**
     * The positions of the emoji in the given text.
     */
    @Singular
    @Getter
    private List<Integer> positions;

    /**
     * Returns the number of occurrences of the emoji in the given text.
     *
     * @return the number of occurrences of the emoji in the given text
     */
    public int getOccurrences() {
        return this.positions.size();
    }
}
