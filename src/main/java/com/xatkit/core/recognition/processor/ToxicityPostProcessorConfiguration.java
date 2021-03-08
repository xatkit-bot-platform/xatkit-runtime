package com.xatkit.core.recognition.processor;

import lombok.Value;
import lombok.experimental.Accessors;
import org.apache.commons.configuration2.Configuration;

/**
 * Configuration options for the {@link ToxicityPostProcessor}.
 * <p>
 * This class is initialized with a {@link Configuration} instance, and takes care of extracting the
 * {@link ToxicityPostProcessor}-related properties.
 */
@Value
public class ToxicityPostProcessorConfiguration {

    /**
     * The {@link Configuration} flag to enable Perspective API labelling.
     */
    public static final String USE_PERSPECTIVE_API = "xatkit.perspectiveapi";

    /**
     * The {@link Configuration} flag to enable Detoxify labelling.
     */
    public static final String USE_DETOXIFY = "xatkit.detoxify";

    /**
     * {@code true} if Perspective API labelling is enabled, {@code false} otherwise.
     * <p>
     * This flag is {@code false} by default.
     */
    @Accessors(fluent = true)
    private boolean usePerspectiveApi;

    /**
     * {@code true} if Detoxify labelling is enabled, {@code false} otherwise.
     * <p>
     * This flag is {@code false} by default.
     */
    @Accessors(fluent = true)
    private boolean useDetoxifyApi;

    /**
     * Initializes the configuration from the provided {@code baseConfiguration}.
     *
     * @param baseConfiguration the {@link Configuration} to load the values from
     */
    public ToxicityPostProcessorConfiguration(Configuration baseConfiguration) {
        this.usePerspectiveApi = baseConfiguration.getBoolean(USE_PERSPECTIVE_API, false);
        this.useDetoxifyApi = baseConfiguration.getBoolean(USE_DETOXIFY, false);
    }
}
