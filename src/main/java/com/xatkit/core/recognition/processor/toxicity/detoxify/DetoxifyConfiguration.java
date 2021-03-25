package com.xatkit.core.recognition.processor.toxicity.detoxify;

import lombok.Value;
import org.apache.commons.configuration2.Configuration;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * The {@link DetoxifyClient} configuration.
 * <p>
 * This class is initialized with a {@link Configuration} instance, and takes care of extracting the Detoxify-related
 * properties.
 */
@Value
public class DetoxifyConfiguration {

    /**
     * The {@link Configuration} key to store the Detoxify server URL.
     */
    public static final String DETOXIFY_SERVER_URL = "xatkit.detoxify.server.url";

    /**
     * The Detoxify server URL.
     */
    private String detoxifyServerUrl;

    /**
     * Initializes the configuration from the provided {@code baseConfiguration}.
     * <p>
     * The provided {@code baseConfiguration} must contain a non-null value for the following keys:
     * <ul>
     *     <li>{@link #DETOXIFY_SERVER_URL}</li>
     * </ul>
     *
     * @param baseConfiguration the base {@link Configuration} used to initialize this class
     * @throws NullPointerException if the provided {@code baseConfiguration} is {@code null} or if it does not
     *                              define a required key
     */
    public DetoxifyConfiguration(Configuration baseConfiguration) {
        String serverUrl = baseConfiguration.getString(DETOXIFY_SERVER_URL);
        checkNotNull(serverUrl, "The provided {0} does not define a value for {1}",
                Configuration.class.getSimpleName(), DETOXIFY_SERVER_URL);
        if (serverUrl.endsWith("/")) {
            /*
             * Remove the trailing "/" to uniformize the URL.
             */
            this.detoxifyServerUrl = serverUrl.substring(0, serverUrl.length() - 1);
        } else {
            this.detoxifyServerUrl = serverUrl;
        }
    }
}
