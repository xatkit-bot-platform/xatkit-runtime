package com.xatkit.core.recognition.processor;

import com.vdurmont.emoji.EmojiParser;
import com.xatkit.execution.StateContext;
import org.apache.commons.configuration2.Configuration;

import java.util.Set;
import java.util.TreeSet;

/**
 * Transcribes emojis to text
 * <p>
 * Emojis are replaced by one of its aliases, according to the implementation of
 * {@link EmojiParser#parseToAliases(String, EmojiParser.FitzpatrickAction)}.
 *
 * @see #process(String, StateContext)
 * @see EmojiParser#parseToAliases(String, EmojiParser.FitzpatrickAction)
 */
public class EmojiToTextPreProcessor implements InputPreProcessor {

    /**
     * The {@link Configuration} flag to specify whether the emojis should be removed or replaced.
     */
    public static final String REMOVE_EMOJIS = "xatkit.emojis.remove";

    /**
     * {@code true} if emojis should be removed, {code false} if emojis should be replaced by text.
     */
    private final boolean removeEmojis;


    /**
     * Initializes the {@link EmojiToTextPreProcessor}.
     *
     * @param configuration the Xatkit bot configuration.
     */
    public EmojiToTextPreProcessor(Configuration configuration) {
        removeEmojis = configuration.getBoolean(REMOVE_EMOJIS, true);
    }

    /**
     * Processes the provided {@code input}, replacing its emojis by their respective aliases.
     *
     * If the emojis are before/after some text that is missing a space in between, it is added.
     *
     * Some aliases contain numbers or {@code _}. The {@code _} chars are replaced by spaces, and numbers are deleted.
     * Skin tones are not taken into account (they are deleted)
     *
     * @param input   the input to process
     * @param context the {@link StateContext} associated to the {@code input}
     * @return the processed {@code input}
     */
    @Override
    public String process(String input, StateContext context) {
        String output;
        if (this.removeEmojis) {
            output = removeEmojis(input);
        } else {
            output = replaceEmojis(input);
        }
        return output;
    }

    private String removeEmojis(String input) {
        return EmojiParser.removeAllEmojis(input);
    }

    private String replaceEmojis(String input) {
        Set<String> emojisInInput = new TreeSet<>(EmojiParser.extractEmojis(input)).descendingSet();
        for (String emoji : emojisInInput) {
            while (input.contains(emoji)) {
                int index = input.indexOf(emoji);
                String alias = EmojiParser.parseToAliases(emoji, EmojiParser.FitzpatrickAction.REMOVE);
                alias = alias.substring(1, alias.length() - 1).replaceAll("_", " ").replaceAll("[0-9]", "");
                int aliasLength = alias.length();
                input = input.replaceFirst(emoji, alias);
                if ((index + aliasLength < input.length()) && (input.charAt(index + aliasLength) != ' ')) {
                    input = input.substring(0, index + aliasLength) + " " + input.substring(index + aliasLength);
                }
                if ((index > 0) && (input.charAt(index - 1) != ' ')) {
                    input = input.substring(0, index) + " " + input.substring(index);
                }
            }
        }
        return input;
    }
}
