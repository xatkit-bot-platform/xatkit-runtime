package com.xatkit.core.recognition.processor;

import com.xatkit.execution.StateContext;
import com.xatkit.intent.BaseEntityDefinition;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.EntityType;
import com.xatkit.intent.RecognizedIntent;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

/**
 * Removes the English stop words from the intent's parameter values extracted from {@code any} entities.
 * <p>
 * If a processed parameter value result in an empty String the initial value is preserved, assuming that the entity is
 * supposed to match stop words.
 * <p>
 * This post-processor uses the list of English stop words collected by the community on
 * <a href="https://gist.github.com/sebleier/554280">Github</a>.
 */
public class RemoveEnglishStopWordsPostProcessor implements IntentPostProcessor {

    /**
     * The name of the file containing the list of stop words.
     */
    private static final String STOP_WORDS_FILE = "en-stopwords.txt";

    /**
     * The in-memory {@link List} of stop words parsed from the corresponding file.
     *
     * @see #STOP_WORDS_FILE
     */
    private List<String> stopWordsList;

    /**
     * Loads the stop words {@link List}.
     * <p>
     * If an error occurred while loading the stop words an error message is logged, but no exception is thrown. This
     * processor won't be able to remove stop words but it should not prevent Xatkit to start.
     */
    public RemoveEnglishStopWordsPostProcessor() {
        InputStream inputStream =
                RemoveEnglishStopWordsPostProcessor.class.getClassLoader().getResourceAsStream(STOP_WORDS_FILE);
        String stopWords = "";
        if (isNull(inputStream)) {
            Log.error("Cannot find the stop word file {0}, this processor won't remove any stop word", STOP_WORDS_FILE);
        } else {
            try {
                stopWords = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            } catch (IOException e) {
                Log.error(e, "An error occurred when loading the stop word file {0}, this processors won't remove any"
                        + " stop word. See attached exception:", STOP_WORDS_FILE);
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.error(e, "An error occurred when closing the stop word file {0}, see attached exception",
                            STOP_WORDS_FILE);
                }
            }
        }
        stopWordsList = Arrays.asList(stopWords.split(","));
        Log.debug("Loaded {0} stop words from {1}", stopWordsList.size(), STOP_WORDS_FILE);
    }

    /**
     * Removes the English stop words from the {@code recognizedIntent}'s parameter values extracted from {@code any}
     * entities.
     * <p>
     * If a processed parameter value result in an empty String the initial value is preserved, assuming that the
     * entity is supposed to match stop words.
     *
     * @param recognizedIntent the {@link RecognizedIntent} to process
     * @param context          the {@link StateContext} associated to the {@code recognizedIntent}
     * @return the updated {@code recognizedIntent}
     */
    @Override
    public RecognizedIntent process(RecognizedIntent recognizedIntent, StateContext context) {
        recognizedIntent.getValues().forEach(v -> {
            EntityDefinition referredEntity = v.getContextParameter().getEntity().getReferredEntity();
            if (referredEntity instanceof BaseEntityDefinition) {
                BaseEntityDefinition baseEntityDefinition = (BaseEntityDefinition) referredEntity;
                if (baseEntityDefinition.getEntityType().equals(EntityType.ANY)) {
                    if (v.getValue() instanceof String) {
                        String processedValue = removeStopWords((String) v.getValue());
                        v.setValue(processedValue);
                    } else {
                        Log.error("Found {1} parameter value for an any entity", v.getClass().getSimpleName());
                    }
                }
            }
        });
        return recognizedIntent;
    }

    /**
     * Returns the {@link List} of stop words used by this processor.
     * <p>
     * This method is package-private for testing purposes.
     *
     * @return the {@link List} of stop words used by this processor
     */
    List<String> getStopWordsList() {
        return this.stopWordsList;
    }

    /**
     * Removes the stop words from the provided {@link String}.
     *
     * @param from the {@link String} to remove the stop words from
     * @return the resulting {@link String}
     */
    private String removeStopWords(String from) {
        /*
         * Can't use Arrays.asList here, the returned ArrayList does not support remove().
         */
        List<String> splitFrom = Arrays.stream(from.split(" ")).collect(Collectors.toList());
        /*
         * Fix #321. The stop words list only contains lowercase entries, so we need to ignore case to compare the
         * values.
         */
        splitFrom.removeIf(value -> stopWordsList.contains(value.toLowerCase()));
        String result = String.join(" ", splitFrom);
        if (result.isEmpty()) {
            /*
             * If we removed everything from the result this probably means that the stop word was actually useful,
             * in this case we return the original String.
             */
            return from;
        }
        return result;
    }
}
