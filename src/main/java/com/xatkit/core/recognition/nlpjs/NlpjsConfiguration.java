package com.xatkit.core.recognition.nlpjs;

import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.configuration2.Configuration;

import java.util.Locale;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;


/**
 * Contains NLP.js-related configuration.
 * <p>
 * This class can be initialized with a {@link Configuration} instance, and takes care of extracting the NLP
 * .js-related properties.
 * <p>
 * The base {@link Configuration} used to initialize this class can be accessed through {@link #getBaseConfiguration()}.
 */
@Value
public class NlpjsConfiguration {

    /**
     * The {@link Configuration} key to store the URL of the NLP.js server to connect to.
     */
    public static final String NLPJS_SERVER_KEY = "xatkit.nlpjs.server";

    /**
     * The {@link Configuration} key to store the Basic Authentication username to use to connect to the NLP.js server.
     */
    public static final String NLPJS_SERVER_BASICAUTH_USERNAME_KEY = "xatkit.nlpjs.basicauth.username";

    /**
     * The {@link Configuration} key to store the Basic Authentication password to use to connect to the NLP.js server.
     */
    public static final String NLPJS_SERVER_BASICAUTH_PASSWORD_KEY = "xatkit.nlpjs.basicauth.password";

    /**
     * The {@link Configuration} key to store the identifier of the NLP.js agent to use.
     */
    public static final String AGENT_ID_KEY = "xatkit.nlpjs.agentId";

    /**
     * The {@link Configuration} key to store the code of the language processed by the NLP.js agent.
     * <p>
     * This value is set to {@link #DEFAULT_LANGUAGE_CODE} if not provided in the {@link Configuration}.
     *
     * @see #DEFAULT_LANGUAGE_CODE
     */
    public static final String LANGUAGE_CODE_KEY = "xatkit.nlpjs.language";

    /**
     * The default language processed by NLP.js.
     * <p>
     * This value is used by default if {@link #LANGUAGE_CODE_KEY} is not specified in the provided
     * {@link Configuration}.
     *
     * @see #LANGUAGE_CODE_KEY
     */
    public static final String DEFAULT_LANGUAGE_CODE = "en";

    /**
     * The {@link Configuration} key to store whether to clean the NLP.js agent before training it.
     * <p>
     * This property is set to {@code true} if not provided in the {@link Configuration}.
     */
    public static final String CLEAN_AGENT_ON_STARTUP_KEY = "xatkit.nlpjs.clean_on_startup";

    /**
     * The base {@link Configuration} used to initialize the {@link NlpjsConfiguration}.
     */
    private Configuration baseConfiguration;

    /**
     * The URL of the NLP.js server to connect to.
     *
     * @see #NLPJS_SERVER_KEY
     */
    private String nlpjsServer;

    /**
     * The Basic Authentication username to use to connect to the NLP.js server.
     *
     * @see #NLPJS_SERVER_BASICAUTH_USERNAME_KEY
     */
    private String nlpjsServerBasicAuthUsername;

    /**
     * The Basic Authentication password to use to connect to the NLP.js server.
     *
     * @see #NLPJS_SERVER_BASICAUTH_PASSWORD_KEY
     */
    private String nlpjsServerBasicAuthPassword;

    /**
     * The identifier of the NLP.js agent to use.
     *
     * @see #AGENT_ID_KEY
     */
    private String agentId;

    /**
     * The language processed by the NLP.js agent.
     * <p>
     * This property is set to {@link #DEFAULT_LANGUAGE_CODE} if not provided in the {@link Configuration}.
     *
     * @see #LANGUAGE_CODE_KEY
     */
    private String languageCode;

    /**
     * A flag allowing the {@link NlpjsIntentRecognitionProvider} to perform a complete clean of the agent before its
     * initialization.
     * <p>
     * This option is set to {@code true} if not provided in the {@link Configuration}.
     *
     * @see #CLEAN_AGENT_ON_STARTUP_KEY
     */
    private boolean cleanAgentOnStartup;

    /**
     * Initializes the {@link NlpjsConfiguration} with the provided {@code baseConfiguration}.
     * For compound language codes (e.g. "es_ES") we just take the first component as otherwise nlp.js gets confused
     *
     * @param baseConfiguration the {@link Configuration} to load the values from
     * @throws NullPointerException     if the provided {@code baseConfiguration} is {@code null}
     * @throws IllegalArgumentException if the provided {@code baseConfiguration} does not contain a
     *                                  {@link #NLPJS_SERVER_KEY} value and a {@link #AGENT_ID_KEY} value
     */
    public NlpjsConfiguration(@NonNull Configuration baseConfiguration) {
        this.baseConfiguration = baseConfiguration;
        checkArgument(baseConfiguration.containsKey(NLPJS_SERVER_KEY), "The provided %s does not contain a value for "
                + "the mandatory property %s", Configuration.class.getSimpleName(), NLPJS_SERVER_KEY);
        checkArgument(baseConfiguration.containsKey(AGENT_ID_KEY), "The provided %s does not contain a value for the "
                + "mandatory property %s", Configuration.class.getSimpleName(), AGENT_ID_KEY);
        this.nlpjsServer = baseConfiguration.getString(NLPJS_SERVER_KEY);
        this.nlpjsServerBasicAuthUsername = baseConfiguration.getString(NLPJS_SERVER_BASICAUTH_USERNAME_KEY);
        this.nlpjsServerBasicAuthPassword = baseConfiguration.getString(NLPJS_SERVER_BASICAUTH_PASSWORD_KEY);
        this.agentId = baseConfiguration.getString(AGENT_ID_KEY);
        if (baseConfiguration.containsKey(LANGUAGE_CODE_KEY)) {
            String[] splitLocale = baseConfiguration.getString(LANGUAGE_CODE_KEY).split("_");
            languageCode = splitLocale[0];
        } else {
            Log.warn("No language code provided, using the default one ({0})", DEFAULT_LANGUAGE_CODE);
            languageCode = DEFAULT_LANGUAGE_CODE;
        }
        this.cleanAgentOnStartup = baseConfiguration.getBoolean(CLEAN_AGENT_ON_STARTUP_KEY, true);
    }
}
