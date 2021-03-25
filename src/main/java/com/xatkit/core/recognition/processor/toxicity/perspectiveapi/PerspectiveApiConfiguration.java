package com.xatkit.core.recognition.processor.toxicity.perspectiveapi;

import lombok.Value;
import org.apache.commons.configuration2.Configuration;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * The {@link PerspectiveApiClient} configuration.
 * <p>
 * This class is initialized with a {@link Configuration} instance, and takes care of extracting the Perspective
 * API-related properties.
 */
@Value
public class PerspectiveApiConfiguration {

    /**
     * The {@link Configuration} key to store the Perspective API key.
     */
    public static final String API_KEY = "xatkit.perspectiveapi.apiKey";

    /**
     * The {@link Configuration} key to specify the language of the input to analyze.
     */
    public static final String LANGUAGE = "xatkit.perspectiveapi.language";

    /**
     * The {@link Configuration} key to specify whether the data sent to Perspective API should be stored.
     */
    public static final String DO_NOT_STORE = "xatkit.perspectiveapi.doNotStore";

    /**
     * The {@link Configuration} key used to specify the Perspective API client token.
     */
    public static final String CLIENT_TOKEN = "xatkit.perspectiveapi.clientToken";

    /**
     * The {@link Configuration} key used to specify the Perspective API session identifier.
     */
    public static final String SESSION_ID = "xatkit.perspectiveapi.sessionId";

    /**
     * The Perspective API key.
     */
    private String apiKey;

    /**
     * The language of the input to analyze.
     */
    private String language;

    /**
     * {code false} if Perspective API should store the sent data, {@code true} otherwise.
     */
    private boolean doNotStore;

    /**
     * The Perspective API client token.
     */
    private String clientToken;

    /**
     * The Perspective API session identifier.
     */
    private String sessionId;

    /**
     * Initializes the configuration from the provided {@code baseConfiguration}.
     * <p>
     * The provided {@code baseConfiguration} must contain a non-null value for the following keys:
     * <ul>
     *     <li>{@link #API_KEY}</li>
     * </ul>
     *
     * @param baseConfiguration the base {@link Configuration} used to initialize this class
     * @throws NullPointerException if the provided {@code baseConfiguration} is {@code null} or if it does not
     *                              define a required key
     */
    public PerspectiveApiConfiguration(Configuration baseConfiguration) {
        this.apiKey = baseConfiguration.getString(API_KEY);
        checkNotNull(apiKey, "The provided {0} does not define a value for {1}", Configuration.class.getSimpleName(),
                API_KEY);
        this.language = baseConfiguration.getString(LANGUAGE, "en");
        this.doNotStore = baseConfiguration.getBoolean(DO_NOT_STORE, false);
        this.clientToken = baseConfiguration.getString(CLIENT_TOKEN);
        this.sessionId = baseConfiguration.getString(SESSION_ID);
    }
}
