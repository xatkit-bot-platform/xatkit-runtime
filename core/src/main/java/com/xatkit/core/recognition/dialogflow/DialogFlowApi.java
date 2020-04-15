package com.xatkit.core.recognition.dialogflow;

import com.google.api.core.ApiFuture;
import com.google.api.gax.rpc.FailedPreconditionException;
import com.google.api.gax.rpc.InvalidArgumentException;
import com.google.cloud.dialogflow.v2.Context;
import com.google.cloud.dialogflow.v2.ContextName;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.EntityType;
import com.google.cloud.dialogflow.v2.Intent;
import com.google.cloud.dialogflow.v2.IntentView;
import com.google.cloud.dialogflow.v2.ListIntentsRequest;
import com.google.cloud.dialogflow.v2.ProjectAgentName;
import com.google.cloud.dialogflow.v2.ProjectName;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.TextInput;
import com.google.cloud.dialogflow.v2.TrainAgentRequest;
import com.google.longrunning.Operation;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.xatkit.core.EventDefinitionRegistry;
import com.xatkit.core.XatkitException;
import com.xatkit.core.recognition.EntityMapper;
import com.xatkit.core.recognition.IntentRecognitionProvider;
import com.xatkit.core.recognition.RecognitionMonitor;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.BaseEntityDefinition;
import com.xatkit.intent.CompositeEntityDefinition;
import com.xatkit.intent.CompositeEntityDefinitionEntry;
import com.xatkit.intent.ContextInstance;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.ContextParameterValue;
import com.xatkit.intent.CustomEntityDefinition;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.EntityTextFragment;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.LiteralTextFragment;
import com.xatkit.intent.MappingEntityDefinition;
import com.xatkit.intent.MappingEntityDefinitionEntry;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.intent.TextFragment;
import com.xatkit.util.ExecutionModelHelper;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A {@link IntentRecognitionProvider} implementation for the DialogFlow API.
 * <p>
 * This class is used to easily setup a connection to a given DialogFlow agent. The behavior of this connector can be
 * customized in the Xatkit {@link Configuration}, see {@link DialogFlowConfiguration} for more information on the
 * configuration options.
 */
public class DialogFlowApi extends IntentRecognitionProvider {

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
     * The {@link EventDefinitionRegistry} containing the events defined in the current bot.
     * <p>
     * This registry is used to convert intents returned by DialogFlow into {@link RecognizedIntent}s.
     */
    private EventDefinitionRegistry eventRegistry;

    /**
     * The {@link DialogFlowConfiguration} extracted from the provided {@link Configuration}.
     */
    private DialogFlowConfiguration configuration;

    /**
     * The clients used to access the DialogFlow API.
     */
    private DialogFlowClients dialogFlowClients;

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
     * A local cache used to retrieve registered {@link EntityType}s from their display name.
     * <p>
     * This cache is used to limit the number of calls to the DialogFlow API.
     */
    private Map<String, EntityType> registeredEntityTypes;

    /**
     * The {@link RecognitionMonitor} used to track intent matching information.
     */
    @Nullable
    private RecognitionMonitor recognitionMonitor;

    /**
     * Constructs a {@link DialogFlowApi} with the provided {@code configuration}.
     * <p>
     * This constructor is a placeholder for
     * {@link #DialogFlowApi(EventDefinitionRegistry, Configuration, RecognitionMonitor)}
     * with a {@code null} {@link RecognitionMonitor}.
     *
     * @param eventRegistry the {@link EventDefinitionRegistry} containing the events defined in the current bot
     * @param configuration the {@link Configuration} holding the DialogFlow project ID and language code
     * @throws NullPointerException if the provided {@code eventRegistry}, {@code configuration} or one of the
     *                              mandatory {@code configuration} value is {@code null}.
     * @throws DialogFlowException  if the client failed to start a new session
     * @see #DialogFlowApi(EventDefinitionRegistry, Configuration, RecognitionMonitor)
     */
    public DialogFlowApi(@Nonnull EventDefinitionRegistry eventRegistry, @Nonnull Configuration configuration) {
        this(eventRegistry, configuration, null);
    }

    /**
     * Constructs a {@link DialogFlowApi} with the provided {@code configuration}.
     * <p>
     * The behavior of this class can be customized in the provided {@code configuration}. See
     * {@link DialogFlowConfiguration} for more information on the configuration options.
     *
     * @param eventRegistry      the {@link EventDefinitionRegistry} containing the events defined in the current bot
     * @param configuration      the {@link Configuration} holding the DialogFlow project ID and language code
     * @param recognitionMonitor the {@link RecognitionMonitor} instance storing intent matching information
     * @throws NullPointerException if the provided {@code eventRegistry}, {@code configuration} or one of the mandatory
     *                              {@code configuration} value is {@code null}.
     * @throws DialogFlowException  if the client failed to start a new session
     * @see DialogFlowConfiguration
     */
    public DialogFlowApi(@Nonnull EventDefinitionRegistry eventRegistry, @Nonnull Configuration configuration,
                         @Nullable RecognitionMonitor recognitionMonitor) {
        checkNotNull(eventRegistry, "Cannot construct a DialogFlow API instance from the provided {0} {1}",
                EventDefinitionRegistry.class.getSimpleName(), eventRegistry);
        checkNotNull(configuration, "Cannot construct a DialogFlow API instance from the provided {0} {1}",
                Configuration.class.getSimpleName(), configuration);
        Log.info("Starting DialogFlow Client");
        this.eventRegistry = eventRegistry;
        this.configuration = new DialogFlowConfiguration(configuration);
        this.projectAgentName = ProjectAgentName.of(this.configuration.getProjectId());
        this.dialogFlowClients = new DialogFlowClients(this.configuration);
        this.projectName = ProjectName.of(this.configuration.getProjectId());
        this.intentFactory = IntentFactory.eINSTANCE;
        this.entityMapper = new DialogFlowEntityMapper();
        this.cleanAgent();
        this.importRegisteredIntents();
        this.importRegisteredEntities();
        this.recognitionMonitor = recognitionMonitor;
    }

    /**
     * Deletes all the {@link Intent}s and {@link EntityType}s from the DialogFlow agent.
     * <p>
     * Agent cleaning is enabled by setting the property {@link #CLEAN_AGENT_ON_STARTUP_KEY} in the xatkit
     * configuration file, and allows to easily re-deploy bots under development. Production-ready agents should not
     * be cleaned on startup: re-training the ML engine can take a while.
     */
    private void cleanAgent() {
        if (this.configuration.isCleanAgentOnStartup()) {
            Log.info("Cleaning agent DialogFlow agent");
            List<Intent> registeredIntents = getRegisteredIntents();
            for (Intent intent : registeredIntents) {
                if (!intent.getDisplayName().equals(DEFAULT_FALLBACK_INTENT.getName())) {
                    this.dialogFlowClients.getIntentsClient().deleteIntent(intent.getName());
                }
            }
            List<EntityType> registeredEntityTypes = getRegisteredEntityTypes();
            for (EntityType entityType : registeredEntityTypes) {
                this.dialogFlowClients.getEntityTypesClient().deleteEntityType(entityType.getName());
            }
        }
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
        if (this.configuration.isCleanAgentOnStartup()) {
            Log.info("Skipping intent import, the agent has been cleaned on startup");
            return;
        }
        if (configuration.isEnableIntentLoader()) {
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
     * Imports the entities registered in the DialogFlow project.
     * <p>
     * Entities import can be disabled to reduce the number of queries sent to the DialogFlow API by setting the
     * {@link #ENABLE_ENTITY_LOADING_KEY} property to {@code false} in the provided {@link Configuration}. Note that
     * disabling entities import may generate consistency issues when creating, deleting, and matching intents.
     */
    private void importRegisteredEntities() {
        this.registeredEntityTypes = new HashMap<>();
        if (this.configuration.isCleanAgentOnStartup()) {
            Log.info("Skipping entity types import, the agent has been cleaned on startup");
            return;
        }
        if (this.configuration.isEnableIntentLoader()) {
            Log.info("Loading Entities previously registered in the DialogFlow project {0}", projectName.getProject());
            for (EntityType entityType : getRegisteredEntityTypes()) {
                registeredEntityTypes.put(entityType.getDisplayName(), entityType);
            }
        } else {
            Log.info("Entity loading is disabled, existing Entities in the DialogFlow project {0} will not be " +
                    "imported", projectName.getProject());
        }
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
        for (EntityType entityType :
                this.dialogFlowClients.getEntityTypesClient().listEntityTypes(projectAgentName).iterateAll()) {
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
        for (Intent intent : this.dialogFlowClients.getIntentsClient().listIntents(request).iterateAll()) {
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
        for (Intent intent : this.dialogFlowClients.getIntentsClient().listIntents(projectAgentName).iterateAll()) {
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
            Log.debug("Registering {0} {1}", CustomEntityDefinition.class.getSimpleName(), entityDefinition.getName());
            EntityType entityType = this.registeredEntityTypes.get(entityDefinition.getName());
            if (isNull(entityType)) {
                entityType = createEntityTypeFromCustomEntityDefinition((CustomEntityDefinition) entityDefinition);
                try {
                    /*
                     * Store the EntityType returned by the DialogFlow API: some fields such as the name are
                     * automatically set by the platform.
                     */
                    EntityType createdEntityType =
                            this.dialogFlowClients.getEntityTypesClient().createEntityType(projectAgentName,
                                    entityType);
                    this.registeredEntityTypes.put(entityDefinition.getName(), createdEntityType);
                } catch (FailedPreconditionException e) {
                    throw new DialogFlowException(MessageFormat.format("Cannot register the entity {0}, the entity " +
                            "already exists", entityDefinition), e);
                }
            } else {
                Log.debug("{0} {1} is already registered", EntityType.class.getSimpleName(),
                        entityDefinition.getName());
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
            registerReferencedEntityDefinitions(compositeEntityDefinition);
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
     * Registers the {@link EntityDefinition}s referred by the provided {@code compositeEntityDefinition}.
     * <p>
     * Note that this method only registers {@link CustomEntityDefinition}s referred from the provided {@code
     * compositeEntityDefinition}. {@link BaseEntityDefinition}s are already registered since they are part of the
     * platform.
     *
     * @param compositeEntityDefinition the {@link CompositeEntityDefinition} to register the referred
     *                                  {@link EntityDefinition}s of
     * @throws NullPointerException if the provided {@code compositeEntityDefinition} is {@code null}
     * @see #registerEntityDefinition(EntityDefinition)
     */
    private void registerReferencedEntityDefinitions(CompositeEntityDefinition compositeEntityDefinition) {
        checkNotNull(compositeEntityDefinition, "Cannot register referenced %s from %s %s", EntityDefinition.class
                .getSimpleName(), CompositeEntityDefinition.class.getSimpleName(), compositeEntityDefinition);
        for (CompositeEntityDefinitionEntry entry : compositeEntityDefinition.getEntries()) {
            for (EntityDefinition referredEntityDefinition : entry.getEntities()) {
                if (referredEntityDefinition instanceof CustomEntityDefinition) {
                    /*
                     * Only register CustomEntityDefinitions, the other ones are already part of the system.
                     */
                    try {
                        this.registerEntityDefinition(referredEntityDefinition);
                    } catch (DialogFlowException e) {
                        /*
                         * Simply log a warning here, the entity may have been registered before.
                         */
                        Log.warn(e.getMessage());
                    }
                }
            }
        }
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
        Log.debug("Registering DialogFlow intent {0}", intentDefinition.getName());
        if (this.registeredIntents.containsKey(intentDefinition.getName())) {
            throw new DialogFlowException(MessageFormat.format("Cannot register the intent {0}, the intent " +
                    "already exists", intentDefinition.getName()));
        }

        List<String> trainingSentences = intentDefinition.getTrainingSentences();
        List<Intent.TrainingPhrase> dialogFlowTrainingPhrases = new ArrayList<>();
        for (String trainingSentence : trainingSentences) {
            dialogFlowTrainingPhrases.add(createTrainingPhrase(trainingSentence, intentDefinition.getOutContexts()));
        }

        List<String> inContextNames = createInContextNames(intentDefinition);
        List<Context> outContexts = createOutContexts(intentDefinition);
        List<Intent.Parameter> parameters = createParameters(intentDefinition.getOutContexts());

        List<Intent.Message> messages = new ArrayList<>();

        Intent.Builder builder = Intent.newBuilder().setDisplayName(adaptIntentDefinitionNameToDialogFlow
                (intentDefinition.getName())).addAllTrainingPhrases(dialogFlowTrainingPhrases)
                .addAllInputContextNames(inContextNames)
                .addAllOutputContexts(outContexts).addAllParameters(parameters)
                .addAllMessages(messages);

        Intent intent = builder.build();
        try {
            Intent response = this.dialogFlowClients.getIntentsClient().createIntent(projectAgentName, intent);
            registeredIntents.put(response.getDisplayName(), response);
            Log.debug("Intent {0} successfully registered", response.getDisplayName());
        } catch (FailedPreconditionException | InvalidArgumentException e) {
            if (e.getMessage().contains("already exists")) {
                throw new DialogFlowException(MessageFormat.format("Cannot register the intent {0}, the intent " +
                        "already exists", intentDefinition.getName()), e);
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
     * @param outContexts      the {@link IntentDefinition}'s output {@link com.xatkit.intent.Context}s
     *                         associated to the provided training sentence
     * @return the created DialogFlow's {@link com.google.cloud.dialogflow.v2.Intent.TrainingPhrase}
     * @throws NullPointerException if the provided {@code trainingSentence} or {@code outContexts} {@link List} is
     *                              {@code null}, or if one of the {@link ContextParameter}'s name from the provided
     *                              {@code outContexts} is {@code null}
     */
    protected Intent.TrainingPhrase createTrainingPhrase(String trainingSentence, List<com.xatkit.intent
            .Context> outContexts) {
        checkNotNull(trainingSentence, "Cannot create a %s from the provided training sentence %s", Intent
                .TrainingPhrase.class.getSimpleName(), trainingSentence);
        checkNotNull(outContexts, "Cannot create a %s from the provided output %s list %s", Intent.TrainingPhrase
                .class.getSimpleName(), com.xatkit.intent.Context.class.getSimpleName(), outContexts);
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
            for (com.xatkit.intent.Context context : outContexts) {
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
                for (com.xatkit.intent.Context context : outContexts) {
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
     * This method creates an input {@link Context} for every {@link IntentDefinition} which is not a top-level
     * intent (see {@link ExecutionModelHelper#getTopLevelIntents()}). This means that these intents can be matched
     * iff the input context is set in the DialogFlow session.
     * <p>
     * This method returns an empty {@link List} if the provided {@code intentDefinition} is a top-level intent.
     *
     * @param intentDefinition the {@link IntentDefinition} to create the DialogFlow input {@link Context}s from
     * @return the created {@link List} of DialogFlow {@link Context} identifiers
     * @throws NullPointerException if the provided {@code intentDefinition} is {@code null}
     * @see ExecutionModelHelper#getTopLevelIntents()
     */
    protected List<String> createInContextNames(IntentDefinition intentDefinition) {
        checkNotNull(intentDefinition, "Cannot create the in contexts from the provided %s %s", IntentDefinition
                .class.getSimpleName(), intentDefinition);
        List<String> results = new ArrayList<>();
        Collection<IntentDefinition> topLevelIntents = ExecutionModelHelper.getInstance().getTopLevelIntents();
        if (!topLevelIntents.contains(intentDefinition)) {
            ContextName contextName = ContextName.of(this.configuration.getProjectId(),
                    SessionName.of(this.configuration.getProjectId(), "setup").getSession(),
                    "Enable" + intentDefinition.getName());
            results.add(contextName.toString());
        }
        return results;
    }

    /**
     * Creates the DialogFlow output {@link Context}s from the provided {@code intentDefinition}.
     * <p>
     * This method iterates the provided {@code intentDefinition}'s out {@link com.xatkit.intent.Context}s, and
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
        List<com.xatkit.intent.Context> intentDefinitionContexts = intentDefinition.getOutContexts();
        List<Context> results = new ArrayList<>();
        for (com.xatkit.intent.Context context : intentDefinitionContexts) {
            /*
             * Use a dummy session to create the context.
             */
            // TODO duplicated from above?
            ContextName contextName = ContextName.of(this.configuration.getProjectId(),
                    SessionName.of(this.configuration.getProjectId(), "setup").getSession(),
                    context.getName());
            Context dialogFlowContext = Context.newBuilder().setName(contextName.toString()).setLifespanCount(context
                    .getLifeSpan()).build();
            results.add(dialogFlowContext);
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
     * <a href="https://github.com/xatkit-bot-platform/xatkit/issues/147">#147</a>)
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
        ContextName contextName = ContextName.of(this.configuration.getProjectId(),
                SessionName.of(this.configuration.getProjectId(), "setup").getSession(),
                parentIntentDefinition.getName() + "_followUp");
        return Context.newBuilder().setName(contextName.toString()).setLifespanCount(this.configuration.getCustomFollowupLifespan()).build();
    }

    /**
     * Creates the DialogFlow context parameters from the provided Xatkit {@code contexts}.
     * <p>
     * This method iterates the provided {@link com.xatkit.intent.Context}s, and maps their contained
     * parameter's entities to their concrete DialogFlow implementation.
     *
     * @param contexts the {@link List} of Xatkit {@link com.xatkit.intent.Context}s to create the parameters
     *                 from
     * @return the {@link List} of DialogFlow context parameters
     * @throws NullPointerException if the provided {@code contexts} {@link List} is {@code null}, or if one of the
     *                              provided {@link ContextParameter}'s name is {@code null}
     */
    protected List<Intent.Parameter> createParameters(List<com.xatkit.intent.Context> contexts) {
        checkNotNull(contexts, "Cannot create the DialogFlow parameters from the provided %s List %s",
                com.xatkit.intent.Context.class.getSimpleName(), contexts);
        List<Intent.Parameter> results = new ArrayList<>();
        for (com.xatkit.intent.Context context : contexts) {
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
                Optional<Intent.Parameter> parameterAlreadyRegistered =
                        results.stream().filter(r -> r.getDisplayName().equals(parameter.getDisplayName())).findAny();
                if (parameterAlreadyRegistered.isPresent()) {
                    /*
                     * Don't register the parameter if it has been added to the list, this means that we have a
                     * parameter initialized with different fragments, and this is already handled when constructing
                     * the training sentence.
                     * If the parameter is added the agent seems to work fine, but there is an error message
                     * "Parameter name must be unique within the action" in the corresponding intent page.
                     */
                    Log.warn("Parameter {0} is defined multiple times", parameter.getDisplayName());
                } else {
                    results.add(parameter);
                }
            }
        }
        return results;
    }

    /**
     * Adapts the provided {@code intentDefinitionName} by replacing its {@code _} by spaces.
     * <p>
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
            /*
             * Reduce the number of calls to the DialogFlow API by first looking for the EntityType in the local cache.
             */
            EntityType entityType = this.registeredEntityTypes.get(customEntityDefinition.getName());
            if (isNull(entityType)) {
                /*
                 * The EntityType is not in the local cache, loading it through a DialogFlow query.
                 */
                Optional<EntityType> dialogFlowEntityType = getRegisteredEntityTypes().stream().filter
                        (registeredEntityType -> registeredEntityType.getDisplayName().equals(customEntityDefinition
                                .getName())).findAny();
                if (dialogFlowEntityType.isPresent()) {
                    entityType = dialogFlowEntityType.get();
                } else {
                    Log.warn("Cannot delete the {0} {1}, the entity type does not exist", EntityType.class
                            .getSimpleName(), entityDefinition.getName());
                    return;
                }
            }
            this.dialogFlowClients.getEntityTypesClient().deleteEntityType(entityType.getName());
            Log.debug("{0} {1} successfully deleted", EntityType.class.getSimpleName(), entityType.getDisplayName());
            /*
             * Remove the deleted EntityType from the local cache.
             */
            this.registeredEntityTypes.remove(entityType.getDisplayName());
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
        /*
         * Reduce the number of calls to the DialogFlow API by first looking for the Intent in the local cache.
         */
        Intent intent = this.registeredIntents.get(intentDefinition.getName());
        if (isNull(intent)) {
            /*
             * The Intent is not in the local cache, loading it through a DialogFlow query.
             */
            Optional<Intent> dialogFlowIntent = getRegisteredIntents().stream().filter(registeredIntent ->
                    registeredIntent.getDisplayName().equals(intentDefinition.getName())).findAny();
            if (dialogFlowIntent.isPresent()) {
                intent = dialogFlowIntent.get();
            } else {
                Log.warn("Cannot delete the {0} {1}, the intent does not exist", Intent.class.getSimpleName(),
                        intentDefinition.getName());
                return;
            }
        }
        this.dialogFlowClients.getIntentsClient().deleteIntent(intent.getName());
        Log.debug("{0} {1} successfully deleted", Intent.class.getSimpleName(), intentDefinition.getName());
        /*
         * Remove the deleted Intent from the local cache.
         */
        this.registeredIntents.remove(intent.getDisplayName());
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
        ApiFuture<Operation> future = this.dialogFlowClients.getAgentsClient().trainAgentCallable().futureCall(request);
        try {
            Operation operation = future.get();
            while (!operation.getDone()) {
                Thread.sleep(1000);
                /*
                 * Retrieve the new version of the Operation from the API.
                 */
                operation =
                        this.dialogFlowClients.getAgentsClient().getOperationsClient().getOperation(operation.getName());
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
     * The returned {@link XatkitSession} is configured by the global {@link Configuration} provided in
     * {@link #DialogFlowApi(EventDefinitionRegistry, Configuration)}.
     *
     * @throws DialogFlowException if the {@link DialogFlowApi} is shutdown
     */
    @Override
    public XatkitSession createSession(String sessionId) {
        if (isShutdown()) {
            throw new DialogFlowException("Cannot create a new Session, the DialogFlow API is shutdown");
        }
        SessionName sessionName = SessionName.of(this.configuration.getProjectId(), sessionId);
        return new DialogFlowSession(sessionName, this.configuration.getBaseConfiguration());
    }

    /**
     * Merges the local {@link DialogFlowSession} in the remote DialogFlow API one.
     * <p>
     * This method ensures that the remote DialogFlow API stays consistent with the local {@link XatkitSession} by
     * setting all the local context variables in the remote session. This allows to match intents with input
     * contexts that have been defined locally, such as received events, custom variables, etc.
     * <p>
     * Local context values that are already defined in the remote DialogFlow API will be overridden by this method.
     * <p>
     * This method sets all the variables from the local context in a single query in order to reduce the number of
     * calls to the remote DialogFlow API.
     *
     * @param dialogFlowSession the local {@link DialogFlowSession} to merge in the remote one
     * @throws XatkitException      if at least one of the local context values' type is not supported
     * @throws NullPointerException if the provided {@code dialogFlowSession} is {@code null}
     * @see #getIntent(String, XatkitSession)
     */
    public void mergeLocalSessionInDialogFlow(DialogFlowSession dialogFlowSession) {
        // TODO should we move this in DialogFlow session?
        Log.debug("Merging local context in the DialogFlow session {0}", dialogFlowSession.getSessionId());
        checkNotNull(dialogFlowSession, "Cannot merge the provided %s %s", DialogFlowSession.class.getSimpleName(),
                dialogFlowSession);
        dialogFlowSession.getRuntimeContexts().getContextMap().entrySet().stream().forEach(contextEntry ->
                {
                    String contextName = contextEntry.getKey();
                    int contextLifespanCount = dialogFlowSession.getRuntimeContexts().getContextLifespanCount
                            (contextName);
                    Context.Builder builder =
                            Context.newBuilder().setName(ContextName.of(this.configuration.getProjectId(),
                                    dialogFlowSession.getSessionName().getSession(), contextName).toString());
                    Map<String, Object> contextVariables = contextEntry.getValue();
                    Map<String, Value> dialogFlowContextVariables = new HashMap<>();
                    contextVariables.entrySet().stream().forEach(contextVariableEntry -> {
                        Value value = buildValue(contextVariableEntry.getValue());
                        dialogFlowContextVariables.put(contextVariableEntry.getKey(), value);
                    });
                    /*
                     * Need to put the lifespanCount otherwise the context is ignored.
                     */
                    builder.setParameters(Struct.newBuilder().putAllFields(dialogFlowContextVariables))
                            .setLifespanCount(contextLifespanCount);
                    this.dialogFlowClients.getContextsClient().createContext(dialogFlowSession.getSessionName(),
                            builder.build());
                }
        );
    }

    // TODO comment this

    /**
     * Creates a protobuf {@link Value} from the provided {@link Object}.
     * <p>
     * This method supports {@link String} and {@link Map} as input, other data types should not be passed to this
     * method, because all the values returned by DialogFlow are translated into {@link String} or {@link Map}.
     *
     * @param from the {@link Object} to translate to a protobuf {@link Value}
     * @return the protobuf {@link Value}
     * @throws IllegalArgumentException if the provided {@link Object}'s type is not supported
     * @see #buildStruct(Map)
     */
    private Value buildValue(Object from) {
        Value.Builder valueBuilder = Value.newBuilder();
        if (from instanceof String) {
            valueBuilder.setStringValue((String) from);
            return valueBuilder.build();
        } else if (from instanceof Map) {
            Struct struct = buildStruct((Map<String, Object>) from);
            return valueBuilder.setStructValue(struct).build();
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Cannot build a protobuf value from {0}", from));
        }
    }

    /**
     * Creates a protobuf {@link Struct} from the provided {@link Map}.
     * <p>
     * This method deals with nested {@link Map}s, as long as their values are {@link String}s. The returned
     * {@link Struct} reflects the {@link Map} nesting hierarchy.
     *
     * @param fromMap the {@link Map} to translate to a protobuf {@link Struct}
     * @return the protobuf {@link Struct}
     * @throws IllegalArgumentException if a nested {@link Map}'s value type is not {@link String} or {@link Map}
     */
    private Struct buildStruct(Map<String, Object> fromMap) {
        Struct.Builder structBuilder = Struct.newBuilder();
        for (Map.Entry<String, Object> entry : fromMap.entrySet()) {
            if (entry.getValue() instanceof String) {
                structBuilder.putFields(entry.getKey(),
                        Value.newBuilder().setStringValue((String) entry.getValue()).build());
            } else if (entry.getValue() instanceof Map) {
                structBuilder.putFields(entry.getKey(), Value.newBuilder().setStructValue(buildStruct((Map<String,
                        Object>) entry.getValue())).build());
            } else {
                throw new IllegalArgumentException(MessageFormat.format("Cannot build a protobuf struct " +
                        "from {0}, unsupported data type", fromMap));
            }
        }
        return structBuilder.build();
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
     * local {@link XatkitSession} in the remote DialogFlow one, in order to ensure that all the local contexts are
     * propagated to the recognition engine.
     *
     * @throws NullPointerException     if the provided {@code input} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code input} is empty
     * @throws DialogFlowException      if the {@link DialogFlowApi} is shutdown or if an exception is thrown by the
     *                                  underlying DialogFlow engine
     * @see #ENABLE_LOCAL_CONTEXT_MERGE_KEY
     */
    @Override
    public RecognizedIntent getIntentInternal(String input, XatkitSession session) {
        if (isShutdown()) {
            throw new DialogFlowException("Cannot extract an Intent from the provided input, the DialogFlow API is " +
                    "shutdown");
        }
        checkNotNull(input, "Cannot retrieve the intent from null");
        checkNotNull(session, "Cannot retrieve the intent using null as a session");
        checkArgument(!input.isEmpty(), "Cannot retrieve the intent from empty string");
        checkArgument(session instanceof DialogFlowSession, "Cannot handle the message, expected session type to be " +
                "%s, found %s", DialogFlowSession.class.getSimpleName(), session.getClass().getSimpleName());
        TextInput.Builder textInput =
                TextInput.newBuilder().setText(input).setLanguageCode(this.configuration.getLanguageCode());
        QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();
        DetectIntentResponse response;

        DialogFlowSession dialogFlowSession = (DialogFlowSession) session;
        if (this.configuration.isEnableContextMerge()) {
            mergeLocalSessionInDialogFlow(dialogFlowSession);
        } else {
            Log.debug("Local context not merged in DialogFlow, context merging has been disabled");
        }

        try {
            response =
                    this.dialogFlowClients.getSessionsClient().detectIntent(((DialogFlowSession) session).getSessionName(),
                    queryInput);
        } catch (Exception e) {
            throw new DialogFlowException(e);
        }
        QueryResult queryResult = response.getQueryResult();
        RecognizedIntent recognizedIntent = convertDialogFlowIntentToRecognizedIntent(queryResult);
        if (nonNull(recognitionMonitor)) {
            recognitionMonitor.logRecognizedIntent(session, recognizedIntent);
        }
        return recognizedIntent;
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
         * Reuse the QueryResult values to set the recognition confidence and the matched input, DialogFlow already
         * provides confidence for each matched intent.
         */
        recognizedIntent.setRecognitionConfidence(result.getIntentDetectionConfidence());
        recognizedIntent.setMatchedInput(result.getQueryText());

        /*
         * Handle the confidence threshold: if the matched intent's confidence is lower than the defined threshold we
         *  set its definition to DEFAULT_FALLBACK_INTENT and we skip context registration.
         */
        if (!recognizedIntent.getDefinition().equals(DEFAULT_FALLBACK_INTENT) && recognizedIntent.getRecognitionConfidence() < this.configuration.getConfidenceThreshold()) {
            boolean containsAnyEntity =
                    recognizedIntent.getDefinition().getOutContexts().stream().flatMap(c -> c.getParameters().stream())
                            .anyMatch(
                                    p -> p.getEntity().getReferredEntity() instanceof BaseEntityDefinition &&
                                            ((BaseEntityDefinition) p.getEntity().getReferredEntity()).getEntityType().equals(com.xatkit.intent.EntityType.ANY)
                            );
            /*
             * We should not reject a recognized intent if it contains an any entity, these intents typically have a
             * low confidence level.
             */
            if (!containsAnyEntity) {
                Log.debug("Confidence for matched intent {0} (input = \"{1}\", confidence = {2}) is lower than the " +
                                "configured threshold ({3}), overriding the matched intent with {4}",
                        recognizedIntent.getDefinition().getName(), recognizedIntent.getMatchedInput(),
                        recognizedIntent.getRecognitionConfidence(), this.configuration.getConfidenceThreshold(),
                        DEFAULT_FALLBACK_INTENT.getName());
                recognizedIntent.setDefinition(DEFAULT_FALLBACK_INTENT);
                return recognizedIntent;
            } else {
                Log.debug("Detected a low-confidence value for the intent {0} (inputs = \"{1}\", confidence = {2}). " +
                        "The intent has not been filtered out because it contains an any entity");
            }
        }
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
            com.xatkit.intent.Context contextDefinition = intentDefinition.getOutContext(contextName);
            if (isNull(contextDefinition)) {
                contextDefinition = this.eventRegistry.getEventDefinitionOutContext(contextName);
            }
            if (nonNull(contextDefinition)) {
                int lifespanCount = context.getLifespanCount();
                ContextInstance contextInstance = intentFactory.createContextInstance();
                contextInstance.setDefinition(contextDefinition);
                contextInstance.setLifespanCount(lifespanCount);
                Log.debug("Processing context {0}", context.getName());
                Map<String, Value> parameterValues = context.getParameters().getFieldsMap();
                for (String key : parameterValues.keySet()) {
                    Value value = parameterValues.get(key);

                    Object parameterValue = buildParameterValue(value);

                    ContextParameter contextParameter = contextDefinition.getContextParameter(key);
                    if (nonNull(contextParameter) && !key.contains(".original")) {
                        ContextParameterValue contextParameterValue = intentFactory.createContextParameterValue();
                        contextParameterValue.setContextParameter(contextParameter);
                        contextParameterValue.setValue(parameterValue);
                        contextInstance.getValues().add(contextParameterValue);
                    }
                }
                recognizedIntent.getOutContextInstances().add(contextInstance);
            } else {
                Log.warn("Cannot retrieve the context definition for the context value {0}", contextName);
            }
        }
        return recognizedIntent;
    }

    // TODO comment this

    /**
     * Build a context parameter value from the provided protobuf {@link Value}.
     * <p>
     * The returned value is assignable to {@link ContextParameterValue#setValue(Object)}, and is either a
     * {@link String}, or a {@link Map} for nested {@link Struct} {@link Value}s.
     *
     * @param fromValue the protobuf {@link Value} to translate
     * @return the context parameter value
     */
    private Object buildParameterValue(Value fromValue) {
        if (fromValue.getKindCase().equals(Value.KindCase.STRUCT_VALUE)) {
            Map<String, Object> parameterMap = new HashMap<>();
            fromValue.getStructValue().getFieldsMap().forEach((key, value) -> {
                if (!key.contains(".original")) {
                    /*
                     * Remove .original in inner structures, we don't need them
                     */
                    Object adaptedValue = buildParameterValue(value);
                    parameterMap.put(key, adaptedValue);
                }
            });
            return parameterMap;
        } else {
            return convertParameterValueToString(fromValue);
        }
    }

    /**
     * Converts the provided {@code value} into a {@link String}.
     * <p>
     * This method converts protobuf's {@link Value}s returned by DialogFlow into {@link String}s that can be
     * assigned to {@link ContextParameterValue}s.
     *
     * @param value the protobuf {@link Value} to convert
     * @return the {@link String} representation of the provided {@code value}.
     */
    protected String convertParameterValueToString(Value value) {
        switch (value.getKindCase()) {
            case STRING_VALUE:
                return value.getStringValue();
            case NUMBER_VALUE:
                DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
                decimalFormatSymbols.setDecimalSeparator('.');
                DecimalFormat decimalFormat = new DecimalFormat("0.###", decimalFormatSymbols);
                decimalFormat.setGroupingUsed(false);
                return decimalFormat.format(value.getNumberValue());
            case BOOL_VALUE:
                return Boolean.toString(value.getBoolValue());
            case NULL_VALUE:
                return "null";
            default:
                /*
                 * Includes LIST_VALUE and STRUCT_VALUE
                 */
                Log.error("Cannot convert the provided value {0}", value);
                return "";
        }
    }

    /**
     * Reifies the provided DialogFlow {@code intent} into an Xatkit {@link IntentDefinition}.
     * <p>
     * This method looks in the {@link EventDefinitionRegistry} for an {@link IntentDefinition} associated to the
     * provided {@code intent}'s name and returns it. If there is no such {@link IntentDefinition} the
     * {@link #DEFAULT_FALLBACK_INTENT} is returned.
     *
     * @param intent the DialogFlow {@link Intent} to retrieve the Xatkit {@link IntentDefinition} from
     * @return the {@link IntentDefinition} associated to the provided {@code intent}
     * @throws NullPointerException if the provided {@code intent} is {@code null}
     */
    private IntentDefinition convertDialogFlowIntentToIntentDefinition(Intent intent) {
        checkNotNull(intent, "Cannot retrieve the %s from the provided %s %s", IntentDefinition.class.getSimpleName()
                , Intent.class.getSimpleName(), intent);
        IntentDefinition result = eventRegistry.getIntentDefinition(intent.getDisplayName());
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
        this.dialogFlowClients.shutdown();
        if (nonNull(this.recognitionMonitor)) {
            this.recognitionMonitor.shutdown();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public RecognitionMonitor getRecognitionMonitor() {
        return recognitionMonitor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isShutdown() {
        return this.dialogFlowClients.isShutdown();
    }
}
