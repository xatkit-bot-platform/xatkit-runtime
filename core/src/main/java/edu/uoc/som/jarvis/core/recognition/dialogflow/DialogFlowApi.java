package edu.uoc.som.jarvis.core.recognition.dialogflow;

import com.google.api.core.ApiFuture;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.FailedPreconditionException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.dialogflow.v2.*;
import com.google.cloud.dialogflow.v2.Context;
import com.google.cloud.dialogflow.v2.EntityType;
import com.google.longrunning.Operation;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import edu.uoc.som.jarvis.core.EventDefinitionRegistry;
import edu.uoc.som.jarvis.core.JarvisCore;
import edu.uoc.som.jarvis.core.JarvisException;
import edu.uoc.som.jarvis.core.recognition.EntityMapper;
import edu.uoc.som.jarvis.core.recognition.IntentRecognitionProvider;
import edu.uoc.som.jarvis.core.session.JarvisSession;
import edu.uoc.som.jarvis.core.session.RuntimeContexts;
import edu.uoc.som.jarvis.intent.*;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;

import java.io.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A concrete wrapper for the DialogFlow API client.
 * <p>
 * This class is used to easily setup a connection to a given DialogFlow agent. Note that in addition to the
 * constructor parameters, the DialogFlow {@code projectId} (see {@link #PROJECT_ID_KEY}), language code (see
 * {@link #LANGUAGE_CODE_KEY}), and the location of the DialogFlow project's key file ((see
 * {@link #GOOGLE_CREDENTIALS_PATH_KEY}) should be provided in the constructor's {@link Configuration}. The
 * DialogFlow project's key file location can also be provided in the {@code GOOGLE_APPLICATION_CREDENTIALS}
 * environment variable. See
 * <a href="https://cloud.google.com/dialogflow-enterprise/docs/reference/libraries">DialogFlow documentation</a> for
 * further information.
 */
public class DialogFlowApi implements IntentRecognitionProvider {

    /**
     * The {@link Configuration} key to store the unique identifier of the DialogFlow project.
     *
     * @see #DialogFlowApi(JarvisCore, Configuration)
     */
    public static String PROJECT_ID_KEY = "jarvis.dialogflow.projectId";

    /**
     * The {@link Configuration} key to store the code of the language processed by DialogFlow.
     *
     * @see #DialogFlowApi(JarvisCore, Configuration)
     */
    public static String LANGUAGE_CODE_KEY = "jarvis.dialogflow.language";

    /**
     * The {@link Configuration} key to store the path of the {@code JSON} credential file for DialogFlow.
     * <p>
     * If this key is not set the {@link DialogFlowApi} will use the {@code GOOGLE_APPLICATION_CREDENTIALS}
     * environment variable to retrieve the credentials file.
     *
     * @see #DialogFlowApi(JarvisCore, Configuration)
     */
    public static String GOOGLE_CREDENTIALS_PATH_KEY = "jarvis.dialogflow.credentials.path";

    /**
     * The default language processed by DialogFlow.
     */
    public static String DEFAULT_LANGUAGE_CODE = "en-US";

    /**
     * The {@link Configuration} key to store whether to initialize the {@link #registeredIntents} {@link Map} with
     * the {@link Intent}s already stored in the DialogFlow project.
     * <p>
     * Intent loading is enabled by default, and should not be an issue when using the {@link DialogFlowApi} in
     * development context. However, creating multiple instances of the {@link DialogFlowApi} (e.g. when running the
     * Jarvis test suite) may throw <i>RESOURCE_EXHAUSTED</i> exceptions. This option can be set to {@code false} to
     * avoid Intent loading, limiting the number of queries to the DialogFlow API.
     * <p>
     * Note that disabling {@link Intent} loading may create consistency issues between the DialogFlow agent and the
     * local {@link DialogFlowApi}, and is not recommended in development environment.
     */
    public static String ENABLE_INTENT_LOADING_KEY = "jarvis.dialogflow.intent.loading";

    /**
     * The {@link Configuration} key to store whether to merge the local {@link JarvisSession} in the DialogFlow one.
     * <p>
     * This option is enabled by default to ensure consistency between the local {@link JarvisSession} and the
     * DialogFlow's one. However, bot implementations that strictly rely on the DialogFlow API and do not use local
     * {@link JarvisSession}s can disable this option to improve the bot's performances and reduce the number of
     * calls to the remote DialogFlow API.
     * <p>
     * Note that disabling this option for a bot implementation that manipulates local {@link JarvisSession}s may
     * generate consistency issues and unexpected behaviors (such as unmatched intents and context value overwriting).
     */
    public static String ENABLE_LOCAL_CONTEXT_MERGE_KEY = "jarvis.dialogflow.context.merge";

    /**
     * The DialogFlow Default Fallback Intent that is returned when the user input does not match any registered Intent.
     *
     * @see #convertDialogFlowIntentToIntentDefinition(Intent)
     */
    private static IntentDefinition DEFAULT_FALLBACK_INTENT = IntentFactory.eINSTANCE.createIntentDefinition();

    /*
     * Initializes the {@link #DEFAULT_FALLBACK_INTENT}'s name.
     */
    static {
        DEFAULT_FALLBACK_INTENT.setName("Default_Fallback_Intent");
    }

    /**
     * The containing {@link JarvisCore} instance.
     */
    private JarvisCore jarvisCore;

    /**
     * The {@link Configuration} used to initialize this class.
     * <p>
     * This {@link Configuration} is used to retrieve the underlying DialogFlow project identifier, language, and
     * customize {@link JarvisSession} and {@link RuntimeContexts}s.
     */
    private Configuration configuration;

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
     * A flag allowing the {@link DialogFlowApi} to load previously registered {@link Intent} from the DialogFlow agent.
     * <p>
     * This option is set to {@code true} by default. Setting it to {@code false} will reduce the number of queries
     * sent to the DialogFlow API, but may generate consistency issues between the DialogFlow agent and the local
     * {@link DialogFlowApi}.
     *
     * @see #ENABLE_INTENT_LOADING_KEY
     */
    private boolean enableIntentLoader;

    /**
     * A flag allowing the {@link DialogFlowApi} to merge local {@link JarvisSession}s in the DialogFlow one.
     * <p>
     * This option is set to {@code true} by default. Setting it to {@code false} will reduce the number of queries
     * sent to the DialogFlow API, but may generate consistency issues and unexpected behaviors for bot
     * implementations that manipulate local {@link JarvisSession}s.
     *
     * @see #ENABLE_LOCAL_CONTEXT_MERGE_KEY
     */
    private boolean enableContextMerge;

    /**
     * Represents the DialogFlow project name.
     * <p>
     * This attribute is used to compute project-level operations, such as the training of the underlying
     * DialogFlow's agent.
     *
     * @see #trainMLEngine()
     */
    private ProjectName projectName;

    /**
     * Represents the DialogFlow agent name.
     * <p>
     * This attribute is used to compute intent-level operations, such as retrieving the list of registered
     * {@link Intent}s, or deleting specific {@link Intent}s.
     *
     * @see #registerIntentDefinition(IntentDefinition)
     * @see #deleteIntentDefinition(IntentDefinition)
     */
    private ProjectAgentName projectAgentName;

    /**
     * The client instance managing DialogFlow agent-related queries.
     * <p>
     * This client is used to compute project-level operations, such as the training of the underlying DialogFlow's
     * agent.
     *
     * @see #trainMLEngine()
     */
    private AgentsClient agentsClient;

    /**
     * The client instance managing DialogFlow intent-related queries.
     * <p>
     * This client is used to compute intent-level operations, such as retrieving the list of registered
     * {@link Intent}s, or deleting specific {@link Intent}s.
     *
     * @see #registerIntentDefinition(IntentDefinition)
     * @see #deleteIntentDefinition(IntentDefinition)
     */
    private IntentsClient intentsClient;

    /**
     * The client instance managing DialogFlow entity-related queries.
     * <p>
     * This client is used to compute entity-level operations, such as retrieving the list of registered
     * {@link com.google.cloud.dialogflow.v2.EntityType.Entity} instances, or deleting specific
     * {@link com.google.cloud.dialogflow.v2.EntityType.Entity}.
     *
     * @see #registerEntityDefinition(EntityDefinition)
     * @see #deleteEntityDefinition(EntityDefinition)
     */
    private EntityTypesClient entityTypesClient;

    /**
     * The client instance managing DialogFlow sessions.
     * <p>
     * This instance is used to initiate new sessions (see {@link #createSession(String)}) and send {@link Intent}
     * detection queries to the DialogFlow engine.
     */
    private SessionsClient sessionsClient;

    /**
     * The client instance managing DialogFlow contexts.
     * <p>
     * This {@link ContextsClient} is used to merge local context information in the DialogFlow session. This enables
     * to match {@link IntentDefinition} with input contexts set from local computation (such as received events,
     * custom variables, etc).
     * <p>
     * <b>Note:</b> the local {@link RuntimeContexts} is merged in DialogFlow before each
     * intent recognition query (see {@link #getIntent(String, JarvisSession)}. This behavior can be disabled by
     * setting the {@link #ENABLE_LOCAL_CONTEXT_MERGE_KEY} to {@code false} in the provided {@link Configuration}.
     *
     * @see #getIntent(String, JarvisSession)
     * @see #ENABLE_LOCAL_CONTEXT_MERGE_KEY
     */
    private ContextsClient contextsClient;

    /**
     * The {@link IntentFactory} used to create {@link RecognizedIntent} instances from DialogFlow computed
     * {@link Intent}s.
     */
    private IntentFactory intentFactory;

    /**
     * The {@link EntityMapper} used to convert abstract entities from the intent model to DialogFlow entities.
     * <p>
     * This {@link EntityMapper} is initialized and configured by this class' constructor, that tailors its concrete
     * entities to DialogFlow-compatible entities.
     */
    private EntityMapper entityMapper;

    /**
     * A local cache used to retrieve registered {@link Intent}s from their display name.
     * <p>
     * This cache is used to limit the number of calls to the DialogFlow API.
     */
    private Map<String, Intent> registeredIntents;

    /**
     * Constructs a {@link DialogFlowApi} with the provided {@code configuration}.
     * <p>
     * The provided {@code configuration} must provide values for the following keys:
     * <ul>
     * <li><b>jarvis.dialogflow.projectId</b>: the unique identifier of the DialogFlow project</li>
     * </ul>
     * The value <b>jarvis.dialogflow.language</b> is not mandatory: if no language code is provided in the
     * {@link Configuration} the default one ({@link #DEFAULT_LANGUAGE_CODE} will be used.
     * <p>
     * The value <b>jarvis.dialogflow.credentials.path</b> is not mandatory either: if no credential path is provided
     * in the {@link Configuration} the {@link DialogFlowApi} will use the {@code GOOGLE_APPLICATION_CREDENTIALS}
     * environment variable to retrieve the credentials file.
     * <p>
     * The value <b>jarvis.dialogflow.intent.loading</b> is not mandatory and allows to tune whether the
     * {@link DialogFlowApi} should load registered DialogFlow {@link Intent}. This option is set to {@code true} by
     * default. Disabling it will reduce the number of queries sent to the DialogFlow API, but may generate
     * consistency issues between the DialogFlow agent and the local {@link DialogFlowApi}.
     * <p>
     * The vaule <b>jarvis.dialogflow.context.merge</b> is not mandatory and allows to tune whether the
     * {@link DialogFlowApi} should merge the local {@link JarvisSession} in the DialogFlow one. This option is set
     * to {@code true} by default, and may be set to {@code false} to improve the performances of bot implementations
     * that strictly rely on the DialogFlow API, and do not manipulate local {@link JarvisSession}s. Disabling this
     * option for a bot implementation that manipulates local {@link JarvisSession}s may generate consistency issues
     * and unexpected behaviors (such as unmatched intents and context value overwriting).
     *
     * @param jarvisCore    the {@link JarvisCore} instance managing the {@link DialogFlowApi}
     * @param configuration the {@link Configuration} holding the DialogFlow project ID and language code
     * @throws NullPointerException if the provided {@code jarvisCore}, {@code configuration} or one of the mandatory
     *                              {@code configuration} value is {@code null}.
     * @throws DialogFlowException  if the client failed to start a new session
     * @see #PROJECT_ID_KEY
     * @see #LANGUAGE_CODE_KEY
     * @see #GOOGLE_CREDENTIALS_PATH_KEY
     * @see #ENABLE_INTENT_LOADING_KEY
     * @see #ENABLE_LOCAL_CONTEXT_MERGE_KEY
     */
    public DialogFlowApi(JarvisCore jarvisCore, Configuration configuration) {
        checkNotNull(jarvisCore, "Cannot construct a DialogFlow API instance with a null JarvisCore instance");
        checkNotNull(configuration, "Cannot construct a DialogFlow API instance from a configuration");
        Log.info("Starting DialogFlow Client");
        this.jarvisCore = jarvisCore;
        this.configuration = configuration;
        this.loadConfiguration(configuration);
        this.projectAgentName = ProjectAgentName.of(projectId);
        this.buildDialogFlowClients(configuration);
        this.projectName = ProjectName.of(projectId);
        this.intentFactory = IntentFactory.eINSTANCE;
        this.entityMapper = new DialogFlowEntityMapper();
        this.importRegisteredIntents();

    }

    /**
     * Initializes the class' attributes from the provided {@code configuration}.
     *
     * @param configuration the {@link Configuration} to retrieve the attribute values from
     */
    private void loadConfiguration(Configuration configuration) {
        this.projectId = configuration.getString(PROJECT_ID_KEY);
        checkNotNull(projectId, "Cannot construct a jarvis instance from a null projectId");
        this.languageCode = configuration.getString(LANGUAGE_CODE_KEY);
        if (isNull(languageCode)) {
            Log.warn("No language code provided, using the default one ({0})", DEFAULT_LANGUAGE_CODE);
            languageCode = DEFAULT_LANGUAGE_CODE;
        }
        this.enableIntentLoader = configuration.getBoolean(ENABLE_INTENT_LOADING_KEY, true);
        this.enableContextMerge = configuration.getBoolean(ENABLE_LOCAL_CONTEXT_MERGE_KEY, true);
    }

    /**
     * Initializes the DialogFlow clients using the provided {@code configuration}.
     * <p>
     * This method builds the DialogFlow's {@link AgentsClient}, {@link SessionsClient},
     * {@link EntityTypesClient}, {@link IntentsClient}, and {@link ContextsClient}, and initializes them with the
     * credentials file path provided in the {@code configuration}.
     * <p>
     * If the provided {@code configuration} does not define a credentials file path the created clients are
     * initialized from the credentials file path stored in the {@code GOOGLE_APPLICATION_CREDENTIALS} environment
     * variable.
     *
     * @param configuration the {@link Configuration} containing the credentials file path
     * @throws DialogFlowException if the provided {@code configuration} or {@code GOOGLE_APPLICATION_CREDENTIALS}
     *                             environment variable does not contain a valid credentials file path
     */
    private void buildDialogFlowClients(Configuration configuration) {
        CredentialsProvider credentialsProvider = getCredentialsProvider(configuration);
        AgentsSettings agentsSettings;
        IntentsSettings intentsSettings;
        EntityTypesSettings entityTypesSettings;
        SessionsSettings sessionsSettings;
        ContextsSettings contextsSettings;
        try {
            if (isNull(credentialsProvider)) {
                /*
                 * No credentials provided, using the GOOGLE_APPLICATION_CREDENTIALS environment variable.
                 */
                Log.info("No credentials file provided, using GOOGLE_APPLICATION_CREDENTIALS environment variable");
                agentsSettings = AgentsSettings.newBuilder().build();
                intentsSettings = IntentsSettings.newBuilder().build();
                entityTypesSettings = EntityTypesSettings.newBuilder().build();
                sessionsSettings = SessionsSettings.newBuilder().build();
                contextsSettings = ContextsSettings.newBuilder().build();
            } else {
                agentsSettings = AgentsSettings.newBuilder().setCredentialsProvider(credentialsProvider).build();
                intentsSettings = IntentsSettings.newBuilder().setCredentialsProvider(credentialsProvider).build();
                entityTypesSettings = EntityTypesSettings.newBuilder().setCredentialsProvider(credentialsProvider)
                        .build();
                sessionsSettings = SessionsSettings.newBuilder().setCredentialsProvider(credentialsProvider).build();
                contextsSettings = ContextsSettings.newBuilder().setCredentialsProvider(credentialsProvider).build();
            }
            this.agentsClient = AgentsClient.create(agentsSettings);
            this.sessionsClient = SessionsClient.create(sessionsSettings);
            this.intentsClient = IntentsClient.create(intentsSettings);
            this.entityTypesClient = EntityTypesClient.create(entityTypesSettings);
            this.contextsClient = ContextsClient.create(contextsSettings);
        } catch (IOException e) {
            throw new DialogFlowException("An error occurred when initializing the DialogFlow clients, see attached " +
                    "exception", e);
        }
    }

    /**
     * Creates the Google's {@link CredentialsProvider} from the provided {@code configuration}.
     * <p>
     * This method loads the credentials file provided in the {@link Configuration} with the key
     * {@link #GOOGLE_CREDENTIALS_PATH_KEY}. If the file does not exist it attempts to load it from the classpath.
     *
     * @param configuration the {@link Configuration} containing the credentials file path
     * @return the created {@link CredentialsProvider}, or {@code null} if the provided {@code configuration} does
     * not specify a credentials file path
     * @throws DialogFlowException if an error occurred when loading the credentials file
     */
    private CredentialsProvider getCredentialsProvider(Configuration configuration) {
        String credentialsPath = configuration.getString(GOOGLE_CREDENTIALS_PATH_KEY);
        if (nonNull(credentialsPath)) {
            Log.info("Loading Google Credentials file {0}", credentialsPath);
            InputStream credentialsInputStream;
            try {
                File credentialsFile = new File(credentialsPath);
                if (credentialsFile.exists()) {
                    credentialsInputStream = new FileInputStream(credentialsFile);
                } else {
                    Log.warn("Cannot load the credentials file at {0}, trying to load it from the classpath",
                            credentialsPath);
                    String classpathCredentialsPath = this.getClass().getClassLoader().getResource(credentialsPath)
                            .getFile();
                    credentialsInputStream = new FileInputStream(classpathCredentialsPath);
                }
                return FixedCredentialsProvider.create(GoogleCredentials.fromStream(credentialsInputStream));
            } catch (FileNotFoundException e) {
                throw new DialogFlowException(MessageFormat.format("Cannot find the credentials file at {0}",
                        credentialsPath), e);
            } catch (IOException e) {
                throw new DialogFlowException("Cannot retrieve the credentials provider, see attached exception", e);
            }
        }
        return null;
    }

    /**
     * Imports the intents registered in the DialogFlow project.
     * <p>
     * Intents import can be disabled to reduce the number of queries sent to the DialogFlow API by setting the
     * {@link #ENABLE_INTENT_LOADING_KEY} property to {@code false} in the provided {@link Configuration}.
     * Note that disabling intents import may generate consistency issues when creating, deleting, and matching intents.
     */
    private void importRegisteredIntents() {
        this.registeredIntents = new HashMap<>();
        if (enableIntentLoader) {
            Log.info("Loading Intents previously registered in the DialogFlow project {0}", projectName
                    .getProject());
            for (Intent intent : getRegisteredIntents()) {
                registeredIntents.put(intent.getDisplayName(), intent);
            }
        } else {
            Log.info("Intent loading is disabled, existing Intents in the DialogFlow project {0} will not be " +
                    "imported", projectName.getProject());
        }
    }

    /**
     * Returns the DialogFlow project unique identifier.
     *
     * @return the DialogFlow project unique identifier
     */
    public String getProjectId() {
        return projectId;
    }

    /**
     * Returns the code of the language processed by DialogFlow.
     *
     * @return the code of the language processed by DialogFlow
     */
    public String getLanguageCode() {
        return languageCode;
    }

    /**
     * Returns the description of the {@link EntityType}s that are registered in the DialogFlow project.
     * <p>
     * <b>Note:</b> this method is package private for testing purposes, and should not be called by client code.
     *
     * @return the descriptions of the {@link EntityType}s that are registered in the DialogFlow project
     * @throws DialogFlowException if the {@link DialogFlowApi} is shutdown
     */
    List<EntityType> getRegisteredEntityTypes() {
        if (isShutdown()) {
            throw new DialogFlowException("Cannot retrieve the registered Entities, the DialogFlow API is shutdown");
        }
        List<EntityType> registeredEntityTypes = new ArrayList<>();
        for (EntityType entityType : entityTypesClient.listEntityTypes(projectAgentName).iterateAll()) {
            registeredEntityTypes.add(entityType);
        }
        return registeredEntityTypes;
    }

    /**
     * Returns the full descriptions of the {@link Intent}s that are registered in the DialogFlow project.
     * <p>
     * The full descriptions of the {@link Intent}s include the {@code training phrases}, that are typically used in
     * testing methods to check that a created {@link Intent} contains all the information provided to the API. To
     * get a partial description of the registered {@link Intent}s see {@link #getRegisteredIntents()}.
     * <p>
     * <b>Note:</b> this method is protected for testing purposes, and should not be called by client code.
     *
     * @return the full descriptions of the {@link Intent}s that are registered in the DialogFlow project
     * @throws DialogFlowException if the {@link DialogFlowApi} is shutdown
     */
    List<Intent> getRegisteredIntentsFullView() {
        if (isShutdown()) {
            throw new DialogFlowException("Cannot retrieve the registered Intents (full view), the DialogFlow API is " +
                    "shutdown");
        }
        List<Intent> registeredIntents = new ArrayList<>();
        ListIntentsRequest request = ListIntentsRequest.newBuilder().setIntentView(IntentView.INTENT_VIEW_FULL)
                .setParent(projectAgentName.toString()).build();
        for (Intent intent : intentsClient.listIntents(request).iterateAll()) {
            registeredIntents.add(intent);
        }
        return registeredIntents;
    }

    /**
     * Returns the partial description of the {@link Intent}s that are registered in the DialogFlow project.
     * <p>
     * The partial descriptions of the {@link Intent}s does not include the {@code training phrases}. To get a full
     * description of the registered {@link Intent}s see {@link #getRegisteredIntentsFullView()}
     * <p>
     * <b>Note:</b> this method is protected for testing purposes, and should not be called by client code.
     *
     * @return the partial descriptions of the {@link Intent}s that are registered in the DialogFlow project
     * @throws DialogFlowException if the {@link DialogFlowApi} is shutdown
     */
    List<Intent> getRegisteredIntents() {
        if (isShutdown()) {
            throw new DialogFlowException("Cannot retrieve the registered Intents (partial view), the DialogFlow API " +
                    "is shutdown");
        }
        List<Intent> registeredIntents = new ArrayList<>();
        for (Intent intent : intentsClient.listIntents(projectAgentName).iterateAll()) {
            registeredIntents.add(intent);
        }
        return registeredIntents;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method reuses the information contained in the provided {@link EntityDefinition} to create a new
     * DialogFlow {@link EntityType} and add it to the current project.
     *
     * @param entityDefinition the {@link EntityDefinition} to register to the DialogFlow project
     * @throws DialogFlowException if the {@link DialogFlowApi} is shutdown, or if the {@link EntityType} already
     *                             exists in the DialogFlow project
     */
    public void registerEntityDefinition(EntityDefinition entityDefinition) {
        if (isShutdown()) {
            throw new DialogFlowException(MessageFormat.format("Cannot register the {0} {1}, the DialogFlow API is" +
                    " shutdown", EntityDefinition.class.getSimpleName(), entityDefinition));
        }
        if (entityDefinition instanceof BaseEntityDefinition) {
            BaseEntityDefinition baseEntityDefinition = (BaseEntityDefinition) entityDefinition;
            Log.trace("Skipping registration of {0} ({1}), {0} are natively supported by DialogFlow",
                    BaseEntityDefinition.class.getSimpleName(), baseEntityDefinition.getEntityType().getLiteral());
        } else if (entityDefinition instanceof CustomEntityDefinition) {
            EntityType entityType = createEntityTypeFromCustomEntityDefinition((CustomEntityDefinition)
                    entityDefinition);
            try {
                entityTypesClient.createEntityType(projectAgentName, entityType);
            } catch (FailedPreconditionException e) {
                String errorMessage = MessageFormat.format("Cannot register the entity {0}, the entity already " +
                        "exists", entityDefinition);
                Log.error(errorMessage);
                throw new DialogFlowException(errorMessage, e);
            }
        } else {
            throw new DialogFlowException(MessageFormat.format("Cannot register the provided {0}, unsupported {1}",
                    entityDefinition.getClass().getSimpleName(), EntityDefinition.class.getSimpleName()));
        }
    }

    /**
     * Creates a DialogFlow {@link EntityType} from the provided {@code entityDefinition}.
     * <p>
     * This method does not register the created {@link EntityType} in the DialogFlow project.
     *
     * @param entityDefinition the {@link CustomEntityDefinition} to create an {@link EntityType} from
     * @return the created {@link EntityType}
     * @throws NullPointerException if the provided {@code entityDefinition} is {@code null}
     */
    private EntityType createEntityTypeFromCustomEntityDefinition(CustomEntityDefinition entityDefinition) {
        checkNotNull(entityDefinition, "Cannot create the %s from the provided %s %s", EntityType.Entity.class
                .getSimpleName(), CustomEntityDefinition.class.getSimpleName(), entityDefinition);
        String entityName = entityDefinition.getName();
        EntityType.Builder builder = EntityType.newBuilder().setDisplayName(entityName);
        if (entityDefinition instanceof MappingEntityDefinition) {
            MappingEntityDefinition mappingEntityDefinition = (MappingEntityDefinition) entityDefinition;
            List<EntityType.Entity> entities = createEntities(mappingEntityDefinition);
            builder.setKind(EntityType.Kind.KIND_MAP).addAllEntities(entities);
        } else if (entityDefinition instanceof CompositeEntityDefinition) {
            CompositeEntityDefinition compositeEntityDefinition = (CompositeEntityDefinition) entityDefinition;
            List<EntityType.Entity> entities = createEntities(compositeEntityDefinition);
            builder.setKind(EntityType.Kind.KIND_LIST).addAllEntities(entities);
        } else {
            throw new DialogFlowException(MessageFormat.format("Cannot register the provided {0}, unsupported {1}",
                    entityDefinition.getClass().getSimpleName(), EntityDefinition.class.getSimpleName()));
        }
        return builder.build();
    }

    /**
     * Creates the DialogFlow {@link EntityType.Entity} instances from the provided {@code mappingEntityDefinition}.
     * <p>
     * {@link EntityType.Entity} instances are created from the provided {@link MappingEntityDefinition}'s entries,
     * and contain the specified <i>referredValue</i> as well as the list of <i>synonyms</i>. The created
     * {@link EntityType.Entity} instances correspond to DialogFlow's
     * <a href="https://dialogflow.com/docs/entities/developer-entities#developer_mapping">Developer Mapping
     * Entities</a>.
     *
     * @param mappingEntityDefinition the {@link MappingEntityDefinition} to create the {@link EntityType.Entity}
     *                                instances from
     * @return the created {@link List} of DialogFlow {@link EntityType.Entity} instances
     * @throws NullPointerException if the provided {@code mappingEntityDefinition} is {@code null}
     */
    private List<EntityType.Entity> createEntities(MappingEntityDefinition mappingEntityDefinition) {
        checkNotNull(mappingEntityDefinition, "Cannot create the %s from the provided %s %s", EntityType.Entity.class
                .getSimpleName(), MappingEntityDefinition.class.getSimpleName(), mappingEntityDefinition);
        List<EntityType.Entity> entities = new ArrayList<>();
        for (MappingEntityDefinitionEntry entry : mappingEntityDefinition.getEntries()) {
            EntityType.Entity.Builder builder = EntityType.Entity.newBuilder().setValue(entry.getReferenceValue())
                    .addAllSynonyms(entry.getSynonyms()).addSynonyms(entry.getReferenceValue());
            entities.add(builder.build());
        }
        return entities;
    }

    /**
     * Creates the DialogFlow {@link EntityType.Entity} instances from the provided {@code compositeEntityDefinition}.
     * <p>
     * {@link EntityType.Entity} instances are created from the provided {@link CompositeEntityDefinition}'s entries,
     * and contain a valid String representation of their <i>value</i> (see
     * {@link #createEntityValue(CompositeEntityDefinitionEntry)}). The created {@link EntityType.Entity} instances
     * correspond to DialogFlow's
     * <a href="https://dialogflow.com/docs/entities/developer-entities#developer_enum">Developer Enums Entities</a>.
     *
     * @param compositeEntityDefinition the {@link CompositeEntityDefinition} to create the {@link EntityType.Entity}
     *                                  instances from
     * @return the create {@link List} of DialogFlow {@link EntityType.Entity} instances
     * @throws NullPointerException if the provided {@code compositeEntityDefinition} is {@code null}
     */
    private List<EntityType.Entity> createEntities(CompositeEntityDefinition compositeEntityDefinition) {
        checkNotNull(compositeEntityDefinition, "Cannot create the %s from the provided %s %s", EntityType.Entity
                .class.getSimpleName(), CompositeEntityDefinition.class.getSimpleName(), compositeEntityDefinition);
        List<EntityType.Entity> entities = new ArrayList<>();
        for (CompositeEntityDefinitionEntry entry : compositeEntityDefinition.getEntries()) {
            String valueString = createEntityValue(entry);
            /*
             * Add the created value as the only synonym for the created Entity: DialogFlow does not allow to create
             * Entities that does not contain their value in their synonym list.
             */
            EntityType.Entity.Builder builder = EntityType.Entity.newBuilder().setValue(valueString).addSynonyms
                    (valueString);
            entities.add(builder.build());
        }
        return entities;
    }

    /**
     * Creates a valid String entity value from the provided {@code entry}.
     * <p>
     * This method iterates the {@code entry}'s {@link TextFragment}s and merge them in a String that can be used as
     * the value of a DialogFlow {@link EntityType.Entity}. Note that {@link EntityTextFragment}s are translated
     * based on the mapping defined in the {@link DialogFlowEntityMapper}, and the name of their corresponding
     * variable is set based on their name.
     *
     * @param entry the {@link CompositeEntityDefinition} to create an entity value from
     * @return the create entity value
     * @throws NullPointerException if the provided {@code entry} is {@code null}
     */
    private String createEntityValue(CompositeEntityDefinitionEntry entry) {
        checkNotNull(entry, "Cannot create the {0} value from the provided {1} {2}", EntityType.Entity.class
                .getSimpleName(), CompositeEntityDefinition.class.getSimpleName(), entry);
        StringBuilder sb = new StringBuilder();
        for (TextFragment fragment : entry.getFragments()) {
            if (fragment instanceof LiteralTextFragment) {
                sb.append(((LiteralTextFragment) fragment).getValue());
            } else if (fragment instanceof EntityTextFragment) {
                /*
                 * Builds a String with the entity name and a default parameter value. The parameter value is set
                 * with the name of the entity itself (eg. @Class:Class). This is fine for composite entities
                 * referring once to their entities, but does not scale to more complex ones with multiple references
                 * to the same entity (see #199).
                 */
                EntityDefinition fragmentEntity = ((EntityTextFragment) fragment).getEntityReference()
                        .getReferredEntity();
                String mappedEntity = entityMapper.getMappingFor(fragmentEntity);
                String mappedEntityParameterName = fragmentEntity.getName();
                sb.append(mappedEntity);
                sb.append(":");
                sb.append(mappedEntityParameterName);
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method reuses the information contained in the provided {@link IntentDefinition} to create a new
     * DialogFlow {@link Intent} and add it to the current project.
     *
     * @param intentDefinition the {@link IntentDefinition} to register to the DialogFlow project
     * @throws DialogFlowException if the {@link DialogFlowApi} is shutdown, or if the {@link Intent} already exists in
     *                             the DialogFlow project
     * @see #createInContextNames(IntentDefinition)
     * @see #createOutContexts(IntentDefinition)
     * @see #createParameters(List)
     */
    @Override
    public void registerIntentDefinition(IntentDefinition intentDefinition) {
        if (isShutdown()) {
            throw new DialogFlowException(MessageFormat.format("Cannot register the Intent {0}, the DialogFlow API is" +
                    " shutdown", intentDefinition.getName()));
        }
        checkNotNull(intentDefinition, "Cannot register the IntentDefinition null");
        checkNotNull(intentDefinition.getName(), "Cannot register the IntentDefinition with null as its name");
        Log.info("Registering DialogFlow intent {0}", intentDefinition.getName());

        List<String> trainingSentences = intentDefinition.getTrainingSentences();
        List<Intent.TrainingPhrase> dialogFlowTrainingPhrases = new ArrayList<>();
        for (String trainingSentence : trainingSentences) {
            dialogFlowTrainingPhrases.add(createTrainingPhrase(trainingSentence, intentDefinition.getOutContexts()));
        }

        List<String> inContextNames = createInContextNames(intentDefinition);
        List<Context> outContexts = createOutContexts(intentDefinition);
        List<Intent.Parameter> parameters = createParameters(intentDefinition.getOutContexts());

        Intent.Builder builder = Intent.newBuilder().setDisplayName(adaptIntentDefinitionNameToDialogFlow
                (intentDefinition.getName())).addAllTrainingPhrases(dialogFlowTrainingPhrases)
                .addAllInputContextNames(inContextNames)
                .addAllOutputContexts(outContexts).addAllParameters(parameters);

        if (nonNull(intentDefinition.getFollows())) {
            Log.info("Registering intent {0} as a follow-up of {1}", intentDefinition.getName(), intentDefinition
                    .getFollows().getName());
            Intent parentIntent = registeredIntents.get(adaptIntentDefinitionNameToDialogFlow(intentDefinition
                    .getFollows().getName()));
            if (isNull(parentIntent)) {
                Log.info(MessageFormat.format("Cannot find intent {0} in the DialogFlow project, trying to register " +
                        "it", intentDefinition.getFollows().getName()));
                registerIntentDefinition(intentDefinition.getFollows());
                parentIntent = registeredIntents.get(adaptIntentDefinitionNameToDialogFlow(intentDefinition
                        .getFollows().getName()));
            }
            if (nonNull(parentIntent)) {
                builder.setParentFollowupIntentName(parentIntent.getName());
            } else {
                /*
                 * The parentIntent registration has failed, there is no way to build a DialogFlow agent that is
                 * consistent with the provided model.
                 */
                throw new JarvisException(MessageFormat.format("Cannot retrieve the parent intent {0}, check the logs" +
                        " for additional information", intentDefinition.getFollows().getName()));
            }
        }

        Intent intent = builder.build();
        try {
            Intent response = intentsClient.createIntent(projectAgentName, intent);
            registeredIntents.put(response.getDisplayName(), response);
            Log.info("Intent {0} successfully registered", response.getDisplayName());
        } catch (FailedPreconditionException e) {
            if (e.getMessage().contains("already exists")) {
                String errorMessage = MessageFormat.format("Cannot register the intent {0}, the intent already " +
                        "exists", intentDefinition.getName());
                Log.error(errorMessage);
                throw new DialogFlowException(errorMessage, e);
            }
        }
    }

    /**
     * Creates the DialogFlow's {@link com.google.cloud.dialogflow.v2.Intent.TrainingPhrase} from the provided {@code
     * trainingSentence} and {@code outContexts}.
     * <p>
     * This method splits the provided {@code trainingSentence} into DialogFlow's
     * {@link com.google.cloud.dialogflow.v2.Intent.TrainingPhrase.Part}s.
     * {@link com.google.cloud.dialogflow.v2.Intent.TrainingPhrase.Part}s corresponding to output context parameters
     * are bound to the corresponding DialogFlow entities using the {@link #entityMapper}.
     *
     * @param trainingSentence the {@link IntentDefinition}'s training sentence to create a
     *                         {@link com.google.cloud.dialogflow.v2.Intent.TrainingPhrase} from
     * @param outContexts      the {@link IntentDefinition}'s output {@link edu.uoc.som.jarvis.intent.Context}s
     *                         associated to the provided training sentence
     * @return the created DialogFlow's {@link com.google.cloud.dialogflow.v2.Intent.TrainingPhrase}
     * @throws NullPointerException if the provided {@code trainingSentence} or {@code outContexts} {@link List} is
     *                              {@code null}, or if one of the {@link ContextParameter}'s name from the provided
     *                              {@code outContexts} is {@code null}
     */
    protected Intent.TrainingPhrase createTrainingPhrase(String trainingSentence, List<edu.uoc.som.jarvis.intent
            .Context> outContexts) {
        checkNotNull(trainingSentence, "Cannot create a %s from the provided training sentence %s", Intent
                .TrainingPhrase.class.getSimpleName(), trainingSentence);
        checkNotNull(outContexts, "Cannot create a %s from the provided output %s list %s", Intent.TrainingPhrase
                .class.getSimpleName(), edu.uoc.som.jarvis.intent.Context.class.getSimpleName(), outContexts);
        if (outContexts.isEmpty()) {
            return Intent.TrainingPhrase.newBuilder().addParts(Intent.TrainingPhrase.Part.newBuilder().setText
                    (trainingSentence).build()).build();
        } else {
            /*
             * First mark all the context parameter literals with #<literal>#. This pre-processing allows to easily
             * split the training sentence into TrainingPhrase parts, that are bound to their concrete entity when
             * needed, and sent to the DialogFlow API.
             * We use this two-step process for simplicity. If the performance of TrainingPhrase creation become an
             * issue we can reshape this method to avoid this pre-processing phase.
             */
            String preparedTrainingSentence = trainingSentence;
            for (edu.uoc.som.jarvis.intent.Context context : outContexts) {
                for (ContextParameter parameter : context.getParameters()) {
                    if (preparedTrainingSentence.contains(parameter.getTextFragment())) {
                        preparedTrainingSentence = preparedTrainingSentence.replace(parameter.getTextFragment(), "#"
                                + parameter.getTextFragment() + "#");
                    }
                }
            }

            /*
             * Process the pre-processed String and bind its entities.
             */
            String[] splitTrainingSentence = preparedTrainingSentence.split("#");
            Intent.TrainingPhrase.Builder trainingPhraseBuilder = Intent.TrainingPhrase.newBuilder();
            for (int i = 0; i < splitTrainingSentence.length; i++) {
                String sentencePart = splitTrainingSentence[i];
                Intent.TrainingPhrase.Part.Builder partBuilder = Intent.TrainingPhrase.Part.newBuilder().setText
                        (sentencePart);
                for (edu.uoc.som.jarvis.intent.Context context : outContexts) {
                    for (ContextParameter parameter : context.getParameters()) {
                        if (sentencePart.equals(parameter.getTextFragment())) {
                            checkNotNull(parameter.getName(), "Cannot build the training sentence \"%s\", the " +
                                            "parameter for the fragment \"%s\" does not define a name",
                                    trainingSentence, parameter.getTextFragment());
                            checkNotNull(parameter.getEntity(), "Cannot build the training sentence \"%s\", the " +
                                            "parameter for the fragment \"%s\" does not define an entity",
                                    trainingSentence, parameter.getTextFragment());
                            String dialogFlowEntity = entityMapper.getMappingFor(parameter.getEntity()
                                    .getReferredEntity());
                            partBuilder.setEntityType(dialogFlowEntity).setAlias(parameter.getName());
                        }
                    }
                }
                trainingPhraseBuilder.addParts(partBuilder.build());
            }
            return trainingPhraseBuilder.build();
        }
    }

    /**
     * Creates the DialogFlow input {@link Context} names from the provided {@code intentDefinition}.
     * <p>
     * This method iterates the provided {@code intentDefinition}'s in {@link edu.uoc.som.jarvis.intent.Context}s, and
     * maps them to their concrete DialogFlow {@link String} identifier. The returned {@link String} can be used to
     * refer to existing DialogFlow's {@link Context}s.
     *
     * @param intentDefinition the {@link IntentDefinition} to create the DialogFlow input {@link Context}s from
     * @return the created {@link List} of DialogFlow {@link Context} identifiers
     * @throws NullPointerException if the provided {@code intentDefinition} is {@code null}
     * @see IntentDefinition#getInContexts()
     */
    protected List<String> createInContextNames(IntentDefinition intentDefinition) {
        checkNotNull(intentDefinition, "Cannot create the in contexts from the provided %s %s", IntentDefinition
                .class.getSimpleName(), intentDefinition);
        List<edu.uoc.som.jarvis.intent.Context> contexts = intentDefinition.getInContexts();
        List<String> results = new ArrayList<>();
        for (edu.uoc.som.jarvis.intent.Context context : contexts) {
            /*
             * Use a dummy session to create the context.
             */
            ContextName contextName = ContextName.of(projectId, SessionName.of(projectId, "setup").getSession(),
                    context.getName());
            results.add(contextName.toString());
            /*
             * Ignore the context parameters, they are not taken into account by DialogFlow for input contexts.
             */
        }
        if (nonNull(intentDefinition.getFollows())) {
            /*
             * Use getName instead of toString, getFollowUpContext returns a fully built context, with a name that
             * uniquely identifies it.
             */
            results.add(getFollowUpContext(intentDefinition.getFollows()).getName());
        }
        return results;
    }

    /**
     * Creates the DialogFlow output {@link Context}s from the provided {@code intentDefinition}.
     * <p>
     * This method iterates the provided {@code intentDefinition}'s out {@link edu.uoc.som.jarvis.intent.Context}s, and
     * maps them to their concrete DialogFlow implementations.
     *
     * @param intentDefinition the {@link IntentDefinition} to create the DialogFlow output {@link Context}s from
     * @return the created {@link List} of DialogFlow {@link Context}s
     * @throws NullPointerException if the provided {@code intentDefinition} is {@code null}
     * @see IntentDefinition#getOutContexts()
     */
    protected List<Context> createOutContexts(IntentDefinition intentDefinition) {
        checkNotNull(intentDefinition, "Cannot create the out contexts from the provided %s %s", IntentDefinition
                .class.getSimpleName(), intentDefinition);
        DialogFlowCheckingUtils.checkOutContexts(intentDefinition);
        List<edu.uoc.som.jarvis.intent.Context> intentDefinitionContexts = intentDefinition.getOutContexts();
        List<Context> results = new ArrayList<>();
        for (edu.uoc.som.jarvis.intent.Context context : intentDefinitionContexts) {
            /*
             * Use a dummy session to create the context.
             */
            ContextName contextName = ContextName.of(projectId, SessionName.of(projectId, "setup").getSession(),
                    context.getName());
            Context dialogFlowContext = Context.newBuilder().setName(contextName.toString()).setLifespanCount(context
                    .getLifeSpan()).build();
            results.add(dialogFlowContext);
        }
        if (!intentDefinition.getFollowedBy().isEmpty()) {
            results.add(getFollowUpContext(intentDefinition));
        }
        return results;
    }

    /**
     * Returns the DialogFlow context used to setup follow-up intent.
     * <p>
     * This method is used to create a context from the parent of a follow-up relationship between intents. The
     * created context is unique for the provided {@code parentIntentDefinition}, and can be reused in all its follow-up
     * intents.
     * <p>
     * The created context is set with a lifespan value of {@code 2}, following DialogFlow usage. Customization of
     * follow-up intent lifespan is planned for a future release (see
     * <a href="https://github.com/gdaniel/jarvis/issues/147">#147</a>)
     *
     * @param parentIntentDefinition the {@link IntentDefinition} to build the context from
     * @return the built DialogFlow context
     * @throws NullPointerException     if the provided {@code parentIntentDefinition} is {@code null}
     * @throws IllegalArgumentException if the provided {@code parentIntentDefinition}'s name is {@code null}
     */
    private Context getFollowUpContext(IntentDefinition parentIntentDefinition) {
        checkNotNull(parentIntentDefinition, "Cannot get the follow-up context name of the provided %s %s",
                IntentDefinition.class.getSimpleName(), parentIntentDefinition);
        checkArgument(nonNull(parentIntentDefinition.getName()) && !parentIntentDefinition.getName().isEmpty(),
                "Cannot get the follow(up context name for the provided %s %s, the name %s is invalid",
                IntentDefinition.class.getSimpleName(), parentIntentDefinition, parentIntentDefinition.getName());
        ContextName contextName = ContextName.of(projectId, SessionName.of(projectId, "setup").getSession(),
                parentIntentDefinition.getName() + "_followUp");
        return Context.newBuilder().setName(contextName.toString()).setLifespanCount(2).build();
    }

    /**
     * Creates the DialogFlow context parameters from the provided Jarvis {@code contexts}.
     * <p>
     * This method iterates the provided {@link edu.uoc.som.jarvis.intent.Context}s, and maps their contained
     * parameter's entities to their concrete DialogFlow implementation.
     *
     * @param contexts the {@link List} of Jarvis {@link edu.uoc.som.jarvis.intent.Context}s to create the parameters
     *                 from
     * @return the {@link List} of DialogFlow context parameters
     * @throws NullPointerException if the provided {@code contexts} {@link List} is {@code null}, or if one of the
     *                              provided {@link ContextParameter}'s name is {@code null}
     */
    protected List<Intent.Parameter> createParameters(List<edu.uoc.som.jarvis.intent.Context> contexts) {
        checkNotNull(contexts, "Cannot create the DialogFlow parameters from the provided %s List %s", edu.uoc.som
                .jarvis.intent.Context.class.getSimpleName(), contexts);
        List<Intent.Parameter> results = new ArrayList<>();
        for (edu.uoc.som.jarvis.intent.Context context : contexts) {
            for (ContextParameter contextParameter : context.getParameters()) {
                checkNotNull(contextParameter.getName(), "Cannot create the %s from the provided %s %s, the" +
                        " name %s is invalid", Intent.Parameter.class.getSimpleName(), ContextParameter.class
                        .getSimpleName(), contextParameter, contextParameter.getName());
                String dialogFlowEntity = entityMapper.getMappingFor(contextParameter.getEntity().getReferredEntity());
                /*
                 * DialogFlow parameters are prefixed with a '$'.
                 */
                Intent.Parameter parameter = Intent.Parameter.newBuilder().setDisplayName(contextParameter.getName())
                        .setEntityTypeDisplayName(dialogFlowEntity).setValue("$" + contextParameter
                                .getName()).build();
                results.add(parameter);
            }
        }
        return results;
    }

    /**
     * Adapts the provided {@code intentDefinitionName} by replacing its {@code _} by spaces.
     * <p>
     * This method is used to deserialize names that have been serialized by
     * {@link EventDefinitionRegistry#adaptEventName(String)}.
     *
     * @param intentDefinitionName the {@link IntentDefinition} name to adapt
     * @return the adapted {@code intentDefinitionName}
     */
    private String adaptIntentDefinitionNameToDialogFlow(String intentDefinitionName) {
        return intentDefinitionName.replaceAll("_", " ");
    }

    /**
     * {@inheritDoc}
     *
     * @throws DialogFlowException if the {@link DialogFlowApi} is shutdown
     */
    @Override
    public void deleteEntityDefinition(EntityDefinition entityDefinition) {
        if (isShutdown()) {
            throw new DialogFlowException(MessageFormat.format("Cannot delete the Intent {0}, the DialogFlow API is " +
                    "shutdown", entityDefinition));
        }
        checkNotNull(entityDefinition, "Cannot delete the {0} {1}", EntityDefinition.class.getSimpleName(),
                entityDefinition);
        if (entityDefinition instanceof BaseEntityDefinition) {
            BaseEntityDefinition baseEntityDefinition = (BaseEntityDefinition) entityDefinition;
            Log.trace("Skipping deletion of {0} ({1}), {0} are natively supported by DialogFlow and cannot be " +
                    "deleted", BaseEntityDefinition.class.getSimpleName(), baseEntityDefinition.getEntityType()
                    .getLiteral());
        } else if (entityDefinition instanceof CustomEntityDefinition) {
            CustomEntityDefinition customEntityDefinition = (CustomEntityDefinition) entityDefinition;
            List<EntityType> entityTypes = getRegisteredEntityTypes();
            for (EntityType entityType : entityTypes) {
                if (entityType.getDisplayName().equals(customEntityDefinition.getName())) {
                    Log.info("Deleting entity {0}", customEntityDefinition.getName());
                    entityTypesClient.deleteEntityType(entityType.getName());
                    Log.info("Entity {0} successfully deleted", customEntityDefinition.getName());
                    return;
                }
            }
            Log.warn("Cannot delete the entity {0}, the entity does not exist", customEntityDefinition.getName());
        } else {
            throw new DialogFlowException(MessageFormat.format("Cannot delete the provided {0}, unsupported {1}",
                    entityDefinition.getClass().getSimpleName(), EntityDefinition.class.getSimpleName()));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws DialogFlowException if the {@link DialogFlowApi} is shutdown
     */
    @Override
    public void deleteIntentDefinition(IntentDefinition intentDefinition) {
        if (isShutdown()) {
            throw new DialogFlowException(MessageFormat.format("Cannot delete the Intent {0}, the DialogFlow API is " +
                    "shutdown", intentDefinition.getName()));
        }
        checkNotNull(intentDefinition, "Cannot delete the IntentDefinition null");
        checkNotNull(intentDefinition.getName(), "Cannot delete the IntentDefinition with null as its name");
        List<Intent> registeredIntents = getRegisteredIntents();
        for (Intent intent : registeredIntents) {
            if (intent.getDisplayName().equals(intentDefinition.getName())) {
                Log.info("Deleting intent {0}", intentDefinition.getName());
                intentsClient.deleteIntent(intent.getName());
                Log.info("Intent {0} successfully deleted", intentDefinition.getName());
                return;
            }
        }
        Log.warn("Cannot delete the Intent {0}, the intent does not exist", intentDefinition.getName());
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method checks every second whether the underlying ML Engine has finished its training. Note that this
     * method is blocking as long as the ML Engine training is not terminated, and may not terminate if an issue
     * occurred on the DialogFlow side.
     *
     * @throws DialogFlowException if the {@link DialogFlowApi} is shutdown
     */
    @Override
    public void trainMLEngine() {
        if (isShutdown()) {
            throw new DialogFlowException("Cannot train the ML Engine, the DialogFlow API is shutdown");
        }
        Log.info("Starting ML Engine Training (this may take a few minutes)");
        TrainAgentRequest request = TrainAgentRequest.newBuilder()
                .setParent(projectName.toString())
                .build();
        ApiFuture<Operation> future = agentsClient.trainAgentCallable().futureCall(request);
        try {
            Operation operation = future.get();
            while (!operation.getDone()) {
                Thread.sleep(1000);
                /*
                 * Retrieve the new version of the Operation from the API.
                 */
                operation = agentsClient.getOperationsClient().getOperation(operation.getName());
            }
            Log.info("ML Engine Training completed");
        } catch (InterruptedException | ExecutionException e) {
            String errorMessage = "An error occurred during the ML Engine Training";
            Log.error(errorMessage);
            throw new DialogFlowException(errorMessage, e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The created session wraps the internal DialogFlow session that is used on the DialogFlow project to retrieve
     * conversation parts from a given user.
     * <p>
     * The returned {@link JarvisSession} is configured by the global {@link Configuration} provided in
     * {@link #DialogFlowApi(JarvisCore, Configuration)}.
     *
     * @throws DialogFlowException if the {@link DialogFlowApi} is shutdown
     */
    @Override
    public JarvisSession createSession(String sessionId) {
        if (isShutdown()) {
            throw new DialogFlowException("Cannot create a new Session, the DialogFlow API is shutdown");
        }
        SessionName sessionName = SessionName.of(projectId, sessionId);
        Log.info("New session created with path {0}", sessionName.toString());
        return new DialogFlowSession(sessionName, configuration);
    }

    /**
     * Merges the local {@link DialogFlowSession} in the remote DialogFlow API one.
     * <p>
     * This method ensures that the remote DialogFlow API stays consistent with the local {@link JarvisSession} by
     * setting all the local context variables in the remote session. This allows to match intents with input
     * contexts that have been defined locally, such as received events, custom variables, etc.
     * <p>
     * Local context values that are already defined in the remote DialogFlow API will be overridden by this method.
     * <p>
     * This method sets all the variables from the local context in a single query in order to reduce the number of
     * calls to the remote DialogFlow API.
     *
     * @param dialogFlowSession the local {@link DialogFlowSession} to merge in the remote one
     * @throws JarvisException      if at least one of the local context values' type is not supported
     * @throws NullPointerException if the provided {@code dialogFlowSession} is {@code null}
     * @see #getIntent(String, JarvisSession)
     */
    public void mergeLocalSessionInDialogFlow(DialogFlowSession dialogFlowSession) {
        Log.info("Merging local context in the DialogFlow session {0}", dialogFlowSession.getSessionId());
        checkNotNull(dialogFlowSession, "Cannot merge the provided %s %s", DialogFlowSession.class.getSimpleName(),
                dialogFlowSession);
        dialogFlowSession.getRuntimeContexts().getContextMap().entrySet().stream().forEach(contextEntry ->
                {
                    String contextName = contextEntry.getKey();
                    int contextLifespanCount = dialogFlowSession.getRuntimeContexts().getContextLifespanCount
                            (contextName);
                    Context.Builder builder = Context.newBuilder().setName(ContextName.of(projectId,
                            dialogFlowSession.getSessionName().getSession(), contextName).toString());
                    Map<String, Object> contextVariables = contextEntry.getValue();
                    Map<String, Value> dialogFlowContextVariables = new HashMap<>();
                    contextVariables.entrySet().stream().forEach(contextVariableEntry -> {
                                if (!(contextVariableEntry.getValue() instanceof String)) {
                                    throw new JarvisException(MessageFormat.format("Cannot merge the current {0} in " +
                                                    "DialogFlow, the context parameter value {1}.{2}={3} is not a " +
                                                    "{4}", this.getClass().getSimpleName(), contextName,
                                            contextVariableEntry.getKey(), contextVariableEntry.getValue(),
                                            String.class.getSimpleName()));
                                }
                                Value value = Value.newBuilder().setStringValue((String) contextVariableEntry
                                        .getValue()).build();
                                dialogFlowContextVariables.put(contextVariableEntry.getKey(), value);
                            }
                    );
                    /*
                     * Need to put the lifespanCount otherwise the context is ignored.
                     */
                    builder.setParameters(Struct.newBuilder().putAllFields(dialogFlowContextVariables))
                            .setLifespanCount(contextLifespanCount);
                    contextsClient.createContext(dialogFlowSession.getSessionName(), builder.build());
                }
        );
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned {@link RecognizedIntent} is constructed from the raw {@link Intent} returned by the DialogFlow
     * API, using the mapping defined in {@link #convertDialogFlowIntentToRecognizedIntent(QueryResult)}.
     * {@link RecognizedIntent}s are used to wrap the Intents returned by the Intent Recognition APIs and
     * decouple the application from the concrete API used.
     * <p>
     * If the {@link #ENABLE_LOCAL_CONTEXT_MERGE_KEY} property is set to {@code true} this method will first merge the
     * local {@link JarvisSession} in the remote DialogFlow one, in order to ensure that all the local contexts are
     * propagated to the recognition engine.
     *
     * @throws NullPointerException     if the provided {@code input} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code input} is empty
     * @throws DialogFlowException      if the {@link DialogFlowApi} is shutdown or if an exception is thrown by the
     *                                  underlying DialogFlow engine
     * @see #ENABLE_LOCAL_CONTEXT_MERGE_KEY
     */
    @Override
    public RecognizedIntent getIntent(String input, JarvisSession session) {
        if (isShutdown()) {
            throw new DialogFlowException("Cannot extract an Intent from the provided input, the DialogFlow API is " +
                    "shutdown");
        }
        checkNotNull(input, "Cannot retrieve the intent from null");
        checkNotNull(session, "Cannot retrieve the intent using null as a session");
        checkArgument(!input.isEmpty(), "Cannot retrieve the intent from empty string");
        checkArgument(session instanceof DialogFlowSession, "Cannot handle the message, expected session type to be " +
                "%s, found %s", DialogFlowSession.class.getSimpleName(), session.getClass().getSimpleName());
        TextInput.Builder textInput = TextInput.newBuilder().setText(input).setLanguageCode(languageCode);
        QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();
        DetectIntentResponse response;

        DialogFlowSession dialogFlowSession = (DialogFlowSession) session;
        if (enableContextMerge) {
            mergeLocalSessionInDialogFlow(dialogFlowSession);
        } else {
            Log.debug("Local context not merged in DialogFlow, context merging has been disabled");
        }

        try {
            response = sessionsClient.detectIntent(((DialogFlowSession) session).getSessionName(), queryInput);
        } catch (Exception e) {
            throw new DialogFlowException(e);
        }
        QueryResult queryResult = response.getQueryResult();
        Log.info("====================\n" +
                "Query Text: {0} \n" +
                "Detected Intent: {1} (confidence: {2})\n" +
                "Fulfillment Text: {3}", queryResult.getQueryText(), queryResult.getIntent()
                .getDisplayName(), queryResult.getIntentDetectionConfidence(), queryResult.getFulfillmentText());
        return convertDialogFlowIntentToRecognizedIntent(queryResult);
    }

    /**
     * Reifies the provided DialogFlow {@link QueryResult} into a {@link RecognizedIntent}.
     * <p>
     * This method relies on the {@link #convertDialogFlowIntentToIntentDefinition(Intent)} method to retrieve the
     * {@link IntentDefinition} associated to the {@link QueryResult}'s {@link Intent}, and the
     * {@link EventDefinitionRegistry#getEventDefinitionOutContext(String)} method to retrieve the registered
     * {@link ContextParameter}s from the DialogFlow contexts.
     *
     * @param result the DialogFlow {@link QueryResult} containing the {@link Intent} to reify
     * @return the reified {@link RecognizedIntent}
     * @throws NullPointerException     if the provided {@link QueryResult} is {@code null}
     * @throws IllegalArgumentException if the provided {@link QueryResult}'s {@link Intent} is {@code null}
     * @see #convertDialogFlowIntentToIntentDefinition(Intent)
     * @see EventDefinitionRegistry#getEventDefinitionOutContext(String)
     */
    private RecognizedIntent convertDialogFlowIntentToRecognizedIntent(QueryResult result) {
        checkNotNull(result, "Cannot create a %s from the provided %s %s", RecognizedIntent.class.getSimpleName(),
                QueryResult.class.getSimpleName(), result);
        checkArgument(nonNull(result.getIntent()), "Cannot create a %s from the provided %s'%s %s", RecognizedIntent
                .class.getSimpleName(), QueryResult.class.getSimpleName(), Intent.class.getSimpleName(), result
                .getIntent());
        Intent intent = result.getIntent();
        RecognizedIntent recognizedIntent = intentFactory.createRecognizedIntent();
        /*
         * Retrieve the IntentDefinition corresponding to this Intent.
         */
        IntentDefinition intentDefinition = convertDialogFlowIntentToIntentDefinition(intent);
        recognizedIntent.setDefinition(intentDefinition);
        /*
         * Set the output context values.
         */
        for (Context context : result.getOutputContextsList()) {
            String contextName = ContextName.parse(context.getName()).getContext();
            /*
             * Search if the Context exists in the retrieved IntentDefinition. It may not be the case because
             * DialogFlow merges all the context values in the active contexts. In that case the only solution is to
             * find the Context from the global registry, that may return inconsistent result if there are multiple
             * contexts defined with the same name.
             */
            edu.uoc.som.jarvis.intent.Context contextDefinition = intentDefinition.getOutContext(contextName);
            if (isNull(contextDefinition)) {
                contextDefinition = this.jarvisCore.getEventDefinitionRegistry().getEventDefinitionOutContext
                        (contextName);
            }
            if (nonNull(contextDefinition)) {
                int lifespanCount = context.getLifespanCount();
                ContextInstance contextInstance = intentFactory.createContextInstance();
                contextInstance.setDefinition(contextDefinition);
                contextInstance.setLifespanCount(lifespanCount);
                Log.info("Processing context {0}", context.getName());
                Map<String, Value> parameterValues = context.getParameters().getFieldsMap();
                for (String key : parameterValues.keySet()) {
                    Log.info("Processing context value {0} ({1})", key, parameterValues.get(key).getStringValue());
                    /*
                     * Ignore original: this variable contains the raw parsed value, we don't need this.
                     */
                    if (!key.contains(".original")) {
                        String parameterValue = parameterValues.get(key).getStringValue();
                        ContextParameter contextParameter = contextDefinition.getContextParameter(key);
                        if (nonNull(contextParameter)) {
                            ContextParameterValue contextParameterValue = intentFactory.createContextParameterValue();
                            contextParameterValue.setValue(parameterValue);
                            contextParameterValue.setContextParameter(contextParameter);
                            contextInstance.getValues().add(contextParameterValue);
                        }
                    }
                }
                recognizedIntent.getOutContextInstances().add(contextInstance);
            } else {
                Log.warn("Cannot retrieve the context definition for the context value {0}", contextName);
            }
        }
        return recognizedIntent;
    }

    /**
     * Reifies the provided DialogFlow {@code intent} into an Jarvis {@link IntentDefinition}.
     * <p>
     * This method looks in the {@link EventDefinitionRegistry} for an {@link IntentDefinition} associated to the
     * provided {@code intent}'s name and returns it. If there is no such {@link IntentDefinition} the
     * {@link #DEFAULT_FALLBACK_INTENT} is returned.
     *
     * @param intent the DialogFlow {@link Intent} to retrieve the Jarvis {@link IntentDefinition} from
     * @return the {@link IntentDefinition} associated to the provided {@code intent}
     * @throws NullPointerException if the provided {@code intent} is {@code null}
     */
    private IntentDefinition convertDialogFlowIntentToIntentDefinition(Intent intent) {
        checkNotNull(intent, "Cannot retrieve the %s from the provided %s %s", IntentDefinition.class.getSimpleName()
                , Intent.class.getSimpleName(), intent);
        IntentDefinition result = jarvisCore.getEventDefinitionRegistry().getIntentDefinition(intent
                .getDisplayName());
        if (isNull(result)) {
            Log.warn("Cannot retrieve the {0} with the provided name {1}, returning the Default Fallback Intent",
                    IntentDefinition.class.getSimpleName(), intent.getDisplayName());
            result = DEFAULT_FALLBACK_INTENT;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        if (isShutdown()) {
            throw new DialogFlowException("Cannot perform shutdown, DialogFlow API is already shutdown");
        }
        this.sessionsClient.shutdownNow();
        this.intentsClient.shutdownNow();
        this.contextsClient.shutdownNow();
        this.agentsClient.shutdownNow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isShutdown() {
        return this.sessionsClient.isShutdown() && this.intentsClient.isShutdown() && this.agentsClient.isShutdown()
                && contextsClient.isShutdown();
    }

    /**
     * Closes the DialogFlow session if it is not shutdown yet.
     */
    @Override
    protected void finalize() {
        if (!sessionsClient.isShutdown()) {
            Log.warn("DialogFlow session was not closed properly, calling automatic shutdown");
            this.sessionsClient.shutdownNow();
        }
        if (!intentsClient.isShutdown()) {
            Log.warn("DialogFlow Intent client was not closed properly, calling automatic shutdown");
            this.intentsClient.shutdownNow();
        }
        if (!contextsClient.isShutdown()) {
            this.contextsClient.shutdownNow();
        }
        if (!agentsClient.isShutdown()) {
            Log.warn("DialogFlow Agent client was not closed properly, calling automatic shutdown");
            this.agentsClient.shutdownNow();
        }
    }
}
