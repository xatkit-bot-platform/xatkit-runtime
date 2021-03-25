package com.xatkit.core.recognition;

import lombok.NonNull;
import lombok.Value;
import org.apache.commons.configuration2.Configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

/**
 * Contains {@link IntentRecognitionProviderFactory} configuration.
 * <p>
 * This class is initialized with a {@link Configuration} instance, and takes care of extracting the
 * {@link IntentRecognitionProviderFactory}-related properties.
 * <p>
 * The base {@link Configuration} used to initialize this class can be accessed through {@link #getBaseConfiguration()}.
 */
@Value
public class IntentRecognitionProviderFactoryConfiguration {

    /**
     * The {@link Configuration} key used to specify whether to enable intent recognition monitoring.
     * <p>
     * Intent recognition monitoring is enabled by default, and stores the results in the {@code data/analytics}
     * folder. It can be disabled by explicitly setting this property to {@code false} in the {@link Configuration}
     * file.
     */
    public static final String ENABLE_RECOGNITION_ANALYTICS = "xatkit.recognition.enable_monitoring";

    /**
     * The {@link Configuration} key used to specify the
     * {@link com.xatkit.core.recognition.processor.InputPreProcessor}s to add to the created
     * {@link IntentRecognitionProvider}.
     * <p>
     * {@link com.xatkit.core.recognition.processor.InputPreProcessor}s are specified as a comma-separated list of
     * processor's names.
     */
    public static final String RECOGNITION_PREPROCESSORS_KEY = "xatkit.recognition.preprocessors";

    /**
     * The {@link Configuration} key used to specify the
     * {@link com.xatkit.core.recognition.processor.IntentPostProcessor}s to add to the created
     * {@link IntentRecognitionProvider}.
     * <p>
     * {@link com.xatkit.core.recognition.processor.IntentPostProcessor}s are specified as a comma-separated list of
     * processor's names.
     */
    public static final String RECOGNITION_POSTPROCESSORS_KEY = "xatkit.recognition.postprocessors";

    /**
     * The base {@link Configuration} used to initialize the {@link IntentRecognitionProviderFactoryConfiguration}.
     */
    private Configuration baseConfiguration;

    /**
     * A flag to enable/disable recognition analytics.
     */
    private boolean enableRecognitionAnalytics;

    /**
     * The list of preprocessor names to add to the created {@link IntentRecognitionProvider}.
     */
    private List<String> preProcessorNames;

    /**
     * The list of postprocessor names to add to the created {@link IntentRecognitionProvider}.
     */
    private List<String> postProcessorNames;

    /**
     * Initializes the {@link IntentRecognitionProviderFactoryConfiguration} with the provided {@code
     * baseConfiguration}.
     *
     * @param baseConfiguration the {@link Configuration} to load the values from
     * @throws NullPointerException if the provided {@code baseConfiguration} is {@code null}
     */
    public IntentRecognitionProviderFactoryConfiguration(@NonNull Configuration baseConfiguration) {
        this.baseConfiguration = baseConfiguration;
        this.enableRecognitionAnalytics = baseConfiguration.getBoolean(ENABLE_RECOGNITION_ANALYTICS, true);
        this.preProcessorNames = getList(baseConfiguration, RECOGNITION_PREPROCESSORS_KEY);
        this.postProcessorNames = getList(baseConfiguration, RECOGNITION_POSTPROCESSORS_KEY);

    }

    /**
     * Returns a {@link List} extracted from the value associated to the provided {@code key} in the given {@code
     * configuration}.
     * <p>
     * <b>Note</b>: this method is a workaround, the correct way to implement it is to use {@code configuration
     * .setListDelimiterHandler}, but this method does not work with the DefaultConversionHandler embedded in the
     * configuration.
     *
     * @param configuration the {@link Configuration} to get the {@link List} from
     * @param key           the key to retrieve the {@link List} from
     * @return the {@link List}, or {@code null} if the provided {@code key} is not contained in the {@code
     * configuration}
     */
    private @NonNull List<String> getList(@NonNull Configuration configuration, @NonNull String key) {
        String value = configuration.getString(key);
        if (isNull(value)) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(",")).map(String::trim).collect(Collectors.toList());
    }
}
