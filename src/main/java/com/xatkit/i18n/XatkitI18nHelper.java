package com.xatkit.i18n;

import lombok.NonNull;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * An utility class that wraps a {@link ResourceBundle} and allows to load {@link String} arrays from property files.
 * <p>
 * This class provides an implementation of {@link #getStringArray(String)} that splits the result of
 * {@link #getString(String)} to return an {@link String} array. This is not possible with the default implementation
 * of {@link ResourceBundle} that doesn't contain a String-to-array transformation and throws a
 * {@link ClassCastException}.
 * <p>
 * The {@link #getStringArray(String)} result is computed on the fly from the underlying {@link ResourceBundle}
 * contents. This means that you can provide an arbitrary number of {@link String} for each locale.
 * This is useful when translating intents where several (or none) translations can be provided for a given training
 * sentence.
 * <p>
 * As an example, this class can be used to load localized intent training sentences:
 * <pre>
 * {@code
 * XatkitI18nHelper helper = new Xatkit18nHelper("MyIntentLibrary", Locale.FRENCH);
 * val myIntent = intent("MyIntent")
 *     .trainingSentences(helper.getStringArray("MyIntent"))
 * }
 * </pre>
 * <p>
 * <b>Note</b>: this class does not extend {@link ResourceBundle} because it is not designed to be loaded with different
 * locales. The concrete bundle is used internally to retrieve the localized values.
 */
public class XatkitI18nHelper {

    /**
     * The {@link ResourceBundle} used to access localized resources.
     */
    private ResourceBundle bundle;

    /**
     * Constructs an instance of this class for the given resource {@code baseName} and the given {@code locale}.
     *
     * @param baseName the name of the {@link ResourceBundle} to use to retrieve localized resources
     * @param locale   the {@link Locale} to use
     * @throws java.util.MissingResourceException if no resource bundle for the given {@code baseName} can be found
     */
    public XatkitI18nHelper(String baseName, Locale locale) {
        this.bundle = ResourceBundle.getBundle(baseName, locale);
    }

    /**
     * Gets a {@link String} for the given {@code key} from the underlying bundle.
     *
     * @param key the key to retrieve the {@link String} for
     * @return the corresponding {@link String} if it exists, or {@code null} otherwise
     * @throws NullPointerException               if the provided {@code key} is {@code null}
     * @throws java.util.MissingResourceException if no value for the given {@code key} can be found
     * @see ResourceBundle#getString(String)
     */
    public @NonNull
    String getString(@NonNull String key) {
        return this.bundle.getString(key);
    }


    /**
     * Gets a {@link String} array for the given {@code key} from the underlying bundle.
     * <p>
     * This method splits the {@link String} associated to the provided {@code key} to return an array. The delimiter
     * used to split the {@link String} is {@code \n}.
     *
     * @param key the key to retrieve the {@link String} for
     * @return the corresponding {@link String} array if it exists, {@code null} otherwise
     * @throws NullPointerException               if the provided {@code key} is {@code null}
     * @throws java.util.MissingResourceException if no value for the given {@code key} can be found
     * @see ResourceBundle#getStringArray(String)
     */
    public @NonNull
    String[] getStringArray(@NonNull String key) {
        return this.bundle.getString(key).split("\\n");
    }
}
