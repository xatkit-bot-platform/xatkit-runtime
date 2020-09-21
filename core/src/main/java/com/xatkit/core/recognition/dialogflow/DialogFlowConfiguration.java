package com.xatkit.core.recognition.dialogflow;

import com.google.cloud.dialogflow.v2.EntityType;
import com.google.cloud.dialogflow.v2.Intent;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.configuration2.Configuration;

import javax.annotation.Nullable;
import java.util.Map;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;

/**
 * Contains DialogFlow-related configuration.
 * <p>
 * This class can be initialized with a {@link Configuration} instance, and takes care of extracting the
 * DialogFlow-related properties.
 * <p>
 * The base {@link Configuration} used to initialize this class can be accessed through {@link #getBaseConfiguration()}.
 */
@Value
public class DialogFlowConfiguration {

    /**
     * The {@link Configuration} key to store the unique identifier of the DialogFlow project.
     */
    public static String PROJECT_ID_KEY = "xatkit.dialogflow.projectId";

    /**
     * The {@link Configuration} key to store the code of the language processed by DialogFlow.
     */
    public static String LANGUAGE_CODE_KEY = "xatkit.dialogflow.language";

    /**
     * The {@link Configuration} key to store the path of the {@code JSON} credential file for DialogFlow.
     * <p>
     * If this key is not set the {@link DialogFlowIntentRecognitionProvider} will use the {@code
     * GOOGLE_APPLICATION_CREDENTIALS}
     * environment variable to retrieve the credentials file.
     */
    public static String GOOGLE_CREDENTIALS_PATH_KEY = "xatkit.dialogflow.credentials.path";

    /**
     * The default language processed by DialogFlow.
     */
    public static String DEFAULT_LANGUAGE_CODE = "en-US";

    /**
     * The {@link Configuration} key to store whether to clean the registered intents and registered entity types
     * when initializing the {@link DialogFlowIntentRecognitionProvider}.
     * <p>
     * This property is disabled by default. Enabling it allows to easily re-deploy chatbots under development, but
     * complete agent cleaning should not be done on production-ready bots (re-training such bots may take a long time).
     */
    public static String CLEAN_AGENT_ON_STARTUP_KEY = "xatkit.dialogflow.clean_on_startup";

    /**
     * The {@link Configuration} key to store whether to initialize the registered intents {@link Map} with the
     * {@link Intent}s already stored in the DialogFlow project.
     * <p>
     * Intent loading is enabled by default, and should not be an issue when using the
     * {@link DialogFlowIntentRecognitionProvider} in
     * development context. However, creating multiple instances of the {@link DialogFlowIntentRecognitionProvider}
     * (e.g. when running the
     * Xatkit test suite) may throw <i>RESOURCE_EXHAUSTED</i> exceptions. This option can be set to {@code false} to
     * avoid Intent loading, limiting the number of queries to the DialogFlow API.
     * <p>
     * Note that disabling {@link Intent} loading may create consistency issues between the DialogFlow agent and the
     * local {@link DialogFlowIntentRecognitionProvider}, and is not recommended in development environment.
     */
    public static String ENABLE_INTENT_LOADING_KEY = "xatkit.dialogflow.intent.loading";

    /**
     * The {@link Configuration} key to store whether to initialize the registered entity types {@link Map} with the
     * {@link EntityType}s already stored in the DialogFlow project.
     * <p>
     * Entity loading is enabled by default, and should not be an issue when using the
     * {@link DialogFlowIntentRecognitionProvider} in
     * development context. However, creating multiple instances of the {@link DialogFlowIntentRecognitionProvider}
     * (e.g. when running the
     * Xatkit test suite) may throw <i>RESOURCE_EXHAUSTED</i> exceptions. This option can be set to {@code false} to
     * avoid Entity loading, limiting the number of queries to the DialogFlow API.
     * <p>
     * Note that disabling {@link EntityType} loading may create consistency issues between the DialogFlow agent and
     * the local {@link DialogFlowIntentRecognitionProvider}, and is not recommended in development environment.
     */
    public static String ENABLE_ENTITY_LOADING_KEY = "xatkit.dialogflow.entity.loading";

    // TODO check if this is still needed?
    /**
     * The {@link Configuration} key to store whether to merge the local context in the DialogFlow one.
     * <p>
     * This option is enabled by default to ensure consistency between the local context and the DialogFlow's one.
     * However, bot implementations that strictly rely on the DialogFlow API and do not use local contexts can disable
     * this option to improve the bot's performances and
     * reduce the number of calls to the remote DialogFlow API.
     * <p>
     * Note that disabling this option for a bot implementation that manipulates local contexts may generate
     * consistency issues and unexpected behaviors (such as unmatched intents and context value overwriting).
     */
    public static String ENABLE_LOCAL_CONTEXT_MERGE_KEY = "xatkit.dialogflow.context.merge";

    /**
     * The {@link Configuration} key to store the lifespan value to use when creating followup intents.
     * <p>
     * This option is set to {@code 2} by default (which corresponds to the DialogFlow default value). Changing it to
     * a higher value allows to match followup intents after multiple inputs. Note that changing it to a smaller
     * value may result in unmatched intents.
     */
    public static String CUSTOM_FOLLOWUP_LIFESPAN = "xatkit.dialogflow.followup.lifespan";

    /**
     * The {@link Configuration} key to store the DialogFlow confidence threshold.
     * <p>
     * This threshold is used to accept/reject a matched intent based on its confidence. The default value is {@code
     * 0} (accept all intents).
     * <p>
     * <b>Note</b>: recognized intents that contain an {@code any} entity are never rejected based on the threshold,
     * these entities typically have a low confidence value.
     */
    public static String CONFIDENCE_THRESHOLD_KEY = "xatkit.dialogflow.confidence.threshold";

    /**
     * The base {@link Configuration} used to initialized the {@link DialogFlowConfiguration}.
     */
    private Configuration baseConfiguration;

    /**
     * The unique identifier of the DialogFlow project.
     *
     * @see #PROJECT_ID_KEY
     */
    private String projectId;

    /**
     * The language code of the DialogFlow project.
     *
     * @see #LANGUAGE_CODE_KEY
     */
    private String languageCode;

    /**
     * The path of the Google credentials file.
     *
     * @see #GOOGLE_CREDENTIALS_PATH_KEY
     */
    @Nullable
    private String googleCredentialsPath;

    /**
     * A flag allowing the {@link DialogFlowIntentRecognitionProvider} to perform a complete clean of the agent
     * before its initialization.
     * <p>
     * This option is set to {@code false} by default. Setting it to {@code true} allows to easily re-deploy bots
     * under development, but should not be used in production-ready bots (re-training the agent may take a while to
     * complete).
     *
     * @see #CLEAN_AGENT_ON_STARTUP_KEY
     */
    private boolean cleanAgentOnStartup;

    /**
     * A flag allowing the {@link DialogFlowIntentRecognitionProvider} to load previously registered {@link Intent}
     * from the DialogFlow agent.
     * <p>
     * This option is set to {@code true} by default. Setting it to {@code false} will reduce the number of queries
     * sent to the DialogFlow API, but may generate consistency issues between the DialogFlow agent and the local
     * {@link DialogFlowIntentRecognitionProvider}.
     *
     * @see #ENABLE_INTENT_LOADING_KEY
     */
    private boolean enableIntentLoader;

    /**
     * A flag allowing the {@link DialogFlowIntentRecognitionProvider} to load previously registered
     * {@link EntityType}s from the
     * DialogFlow agent.
     * <p>
     * This option is set to {@code true} by default. Setting it to {@code false} will reduce the number of queries
     * sent to the DialogFlow API, but may generate consistency issues between the DialogFlow agent and the local
     * {@link DialogFlowIntentRecognitionProvider}.
     *
     * @see #ENABLE_ENTITY_LOADING_KEY
     */
    private boolean enableEntityLoader;

    /**
     * The custom lifespan value to use when creating followup intents.
     * <p>
     * This option is set to {@code 2} by default (which corresponds to the DialogFlow default value). Changing it to
     * a higher value allows to match followup intents after multiple inputs. Note that changing it to a smaller
     * value may result in unmatched intents.
     */
    private int customFollowupLifespan;

    /**
     * The DialogFlow confidence threshold used to accept/reject intents based on their confidence score.
     * <p>
     * This option is set to {@code 0} by default (accept all intents).
     * <b>Note</b>: recognized intents that contain an {@code any} entity are never rejected based on the threshold,
     * these entities typically have a low confidence value.
     */
    private float confidenceThreshold;


    /**
     * Initializes the {@link DialogFlowConfiguration} with the provided {@code baseConfiguration}.
     *
     * @param baseConfiguration the {@link Configuration} to load the values from
     * @throws NullPointerException     if the provided {@code baseConfiguration} is {@code null}
     * @throws IllegalArgumentException if the provided {@code baseConfiguration} does not contain a
     *                                  {@link #PROJECT_ID_KEY} value
     */
    public DialogFlowConfiguration(@NonNull Configuration baseConfiguration) {
        this.baseConfiguration = baseConfiguration;
        checkArgument(baseConfiguration.containsKey(PROJECT_ID_KEY), "The provided %s does not contain a value for " +
                "the mandatory property %s", Configuration.class.getSimpleName(), PROJECT_ID_KEY);
        this.projectId = baseConfiguration.getString(PROJECT_ID_KEY);
        if (baseConfiguration.containsKey(LANGUAGE_CODE_KEY)) {
            languageCode = baseConfiguration.getString(LANGUAGE_CODE_KEY);
        } else {
            Log.warn("No language code provided, using the default one ({0})", DEFAULT_LANGUAGE_CODE);
            languageCode = DEFAULT_LANGUAGE_CODE;
        }
        this.googleCredentialsPath = baseConfiguration.getString(GOOGLE_CREDENTIALS_PATH_KEY);
        this.cleanAgentOnStartup = baseConfiguration.getBoolean(CLEAN_AGENT_ON_STARTUP_KEY, false);
        this.enableIntentLoader = baseConfiguration.getBoolean(ENABLE_INTENT_LOADING_KEY, true);
        this.enableEntityLoader = baseConfiguration.getBoolean(ENABLE_ENTITY_LOADING_KEY, true);
        this.customFollowupLifespan = baseConfiguration.getInt(CUSTOM_FOLLOWUP_LIFESPAN, 2);
        this.confidenceThreshold = baseConfiguration.getFloat(CONFIDENCE_THRESHOLD_KEY, 0);
    }

}
