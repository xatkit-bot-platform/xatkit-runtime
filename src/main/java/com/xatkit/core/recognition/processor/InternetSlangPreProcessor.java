package com.xatkit.core.recognition.processor;

import com.google.gson.Gson;
import com.google.common.reflect.TypeToken;

import com.xatkit.execution.StateContext;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.isNull;

/**
 * Translates internet slang terms by standard-language forms.
 * <p>
 * It uses a dictionary of slang terms {@link InternetSlangPreProcessor#SLANG_DICTIONARY_FILE} to perform the
 * translations, which is obtained from {@link InternetSlangPreProcessor#NOSLANG_URL}
 *
 * @see #process(String, StateContext)
 * @see #getNoslangDictionary()
 */
public class InternetSlangPreProcessor implements InputPreProcessor {

    /**
     * The {@link Configuration} flag to specify whether the slang dictionary should be gotten from the Internet or
     * not (i.e. it is already saved in a file).
     */
    public static final String GET_SLANG_DICTIONARY = "xatkit.slang.getSlangDictionary";

    /**
     * The local path to the directory containing the slang dictionary, i.e.
     * {@link InternetSlangPreProcessor#SLANG_DICTIONARY_FILE}
     */
    protected static final String SLANG_DICTIONARY_PATH = "src/main/resources/";

    /**
     * The name of the file containing the entries of the slang dictionary.
     */
    protected static final String SLANG_DICTIONARY_FILE = "noslang_english_dictionary.json";


    /**
     * The in-memory {@link Map} containing the slang dictionary obtained from the corresponding file.
     * Keys are the slang terms and values are their corresponding translations to standard language.
     *
     * @see #SLANG_DICTIONARY_FILE
     */
    protected static Map<String,String>  slangDictionary;

    /**
     * A list of punctuation characters, used to identify them at the end of a slang term in the input text
     */
    protected static final String[] PUNCTUATION = {"?", "!", ".", ","};

    /**
     * The URL of the online source of the slang dictionary
     */
    protected static final String NOSLANG_URL = "https://www.noslang.com/dictionary/";

    /**
     * The list containing all the available letters of the different dictionary pages. Each letter refers to
     * the {@link InternetSlangPreProcessor#NOSLANG_URL} page that contains all slang terms starting by this
     * character. Note that the "1" page refers to the slang terms starting by non-alphabetic characters.
     */
    protected static final String[] letters = "1abcdefghijklmnopqrstuvwxyz".split("");

    /**
     * {@code true} if this processor needs to get the slang dictionary from
     * {@link InternetSlangPreProcessor#NOSLANG_URL}, {code false} otherwise (i.e.
     * {@link InternetSlangPreProcessor#SLANG_DICTIONARY_FILE} is already stored)
     */
    protected final boolean getSlangDictionary;

    /**
     * Initializes the {@link InternetSlangPreProcessor}.
     * <p>
     * If the {@link InternetSlangPreProcessor#GET_SLANG_DICTIONARY} configuration option is set to {@code true},
     * {@link InternetSlangPreProcessor#getNoslangDictionary()} is called. Then, the
     * {@link InternetSlangPreProcessor#SLANG_DICTIONARY_FILE} file is read and stored in
     * {@link InternetSlangPreProcessor#slangDictionary}
     */
    public InternetSlangPreProcessor(Configuration configuration) {
        getSlangDictionary = configuration.getBoolean(GET_SLANG_DICTIONARY, false);
        if (getSlangDictionary) {
            getNoslangDictionary();
        }
        InputStream inputStream =
                InternetSlangPreProcessor.class.getClassLoader().getResourceAsStream(SLANG_DICTIONARY_FILE);
        String dictionaryAsString = "";
        if (isNull(inputStream)) {
            Log.error("Cannot find the file {0}, this processor won't get the slang dictionary",
                    SLANG_DICTIONARY_FILE);
        } else {
            try {
                dictionaryAsString = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            } catch (IOException e) {
                Log.error(e, "An error occurred when loading the file {0}, this processor won't get the slang "
                        + "dictionary. See attached exception", SLANG_DICTIONARY_FILE);
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.error(e, "An error occurred when closing the file {0}, see attached exception",
                            SLANG_DICTIONARY_FILE);
                }
            }
        }
        slangDictionary = new Gson().fromJson(dictionaryAsString, new TypeToken<HashMap<String, String>>() {}.getType());
        Log.debug("Loaded {0} dictionary entries from {1}", slangDictionary.size(), SLANG_DICTIONARY_FILE);
    }

    /**
     * Processes the provided {@code input}, replacing the slang occurrences by their respective standard-English
     * translations.
     * <p>
     * The processor takes care of the punctuation characters after a slang term. If after a slang term there is some
     * punctuation character present in {@link InternetSlangPreProcessor#PUNCTUATION}, the slang term is properly
     * recognized and the punctuation character will remain there. If after a slang term there is some character
     * (different from a space) not present in {@link InternetSlangPreProcessor#PUNCTUATION}, the slang term will not
     * be recognized and thus not replaced by its standard form.
     *
     * @param input   the input to process
     * @param context the {@link StateContext} associated to the {@code input}
     * @return the processed {@code input}
     */
    public String process(String input, StateContext context) {
        String[] inputWords = input.split(" ");
        for (int i = 0; i < inputWords.length; i++) {
            String word = inputWords[i];
            String lastChar = word.substring(word.length()-1);
            if (Arrays.asList(PUNCTUATION).contains(lastChar)) {
                word = word.substring(0, word.length()-1);
            } else {
                lastChar = "";
            }
            String translation = slangDictionary.get(word);
            if (!isNull(translation)) {
                inputWords[i] = translation + lastChar;
            }
        }
        return String.join(" ", inputWords);
    }

    /**
     * Gets the Internet Slang Dictionary from {@link InternetSlangPreProcessor#NOSLANG_URL} and stores it in
     * {@link InternetSlangPreProcessor#SLANG_DICTIONARY_FILE}, located in
     * {@link InternetSlangPreProcessor#SLANG_DICTIONARY_PATH}.
     * <p>
     * This method is specific for {@link InternetSlangPreProcessor#NOSLANG_URL} since its implementation depends on
     * the structure of the html code of the web page (i.e. it will probably not work for another source)
     */
    protected static void getNoslangDictionary () {
        String fullUrl = null;
        JSONObject dictionary = new JSONObject();
        try {
            for (String letter : letters) {
                fullUrl = NOSLANG_URL + letter;
                Document doc = Jsoup.connect(fullUrl).get();
                Elements dictionaryWords = doc.select("div.dictionary-word");
                for (Element dictionaryWord : dictionaryWords) {
                    String slang = dictionaryWord.select("dt").html();
                    // Remove the " :" characters at the end
                    slang = slang.substring(0, slang.length()-2);
                    if (!dictionary.has(slang)) {
                        String meaning = dictionaryWord.select("dd").html();
                        dictionary.accumulate(slang, meaning);
                    }
                }
            }
        } catch (IOException e) {
            Log.error(e, "An error occurred while getting the slang dictionary from {0}, see attached exception",
                    fullUrl);
        }
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(SLANG_DICTIONARY_PATH + SLANG_DICTIONARY_FILE)))) {
            writer.write(dictionary.toString());
            Log.debug("Saved {0} successfully", SLANG_DICTIONARY_PATH + SLANG_DICTIONARY_FILE);
        } catch (IOException e) {
            Log.error(e, "An error occurred when writting the slang dictionary in {0}, see attached exception",
                    SLANG_DICTIONARY_PATH + SLANG_DICTIONARY_FILE);
        }
    }
}
