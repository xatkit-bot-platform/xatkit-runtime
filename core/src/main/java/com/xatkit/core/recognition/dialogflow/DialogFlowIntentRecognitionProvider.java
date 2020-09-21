package com.xatkit.core.recognition.dialogflow;

import com.google.api.gax.rpc.FailedPreconditionException;
import com.google.api.gax.rpc.InvalidArgumentException;
import com.google.cloud.dialogflow.v2.Context;
import com.google.cloud.dialogflow.v2.DetectIntentRequest;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.EntityType;
import com.google.cloud.dialogflow.v2.Intent;
import com.google.cloud.dialogflow.v2.ProjectAgentName;
import com.google.cloud.dialogflow.v2.ProjectName;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.QueryParameters;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.TextInput;
import com.google.cloud.dialogflow.v2.TrainAgentRequest;
import com.xatkit.core.EventDefinitionRegistry;
import com.xatkit.core.XatkitException;
import com.xatkit.core.recognition.AbstractIntentRecognitionProvider;
import com.xatkit.core.recognition.IntentRecognitionProviderException;
import com.xatkit.core.recognition.RecognitionMonitor;
import com.xatkit.core.recognition.dialogflow.mapper.DialogFlowContextMapper;
import com.xatkit.core.recognition.dialogflow.mapper.DialogFlowEntityMapper;
import com.xatkit.core.recognition.dialogflow.mapper.DialogFlowEntityReferenceMapper;
import com.xatkit.core.recognition.dialogflow.mapper.DialogFlowIntentMapper;
import com.xatkit.core.recognition.dialogflow.mapper.RecognizedIntentMapper;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.BaseEntityDefinition;
import com.xatkit.intent.CompositeEntityDefinition;
import com.xatkit.intent.CompositeEntityDefinitionEntry;
import com.xatkit.intent.CustomEntityDefinition;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.RecognizedIntent;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;
import org.apache.commons.configuration2.Configuration;

import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.ArrayList;
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
 * An {@link AbstractIntentRecognitionProvider} bound to the DialogFlow API.
 * <p>
 * This class is used to easily setup a connection to a given DialogFlow agent. The behavior of this connector can be
 * customized in the Xatkit {@link Configuration}, see {@link DialogFlowConfiguration} for more information on the
 * configuration options.
 */
public class DialogFlowIntentRecognitionProvider extends AbstractIntentRecognitionProvider {

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
     * The mapper creating DialogFlow {@link Intent}s from {@link IntentDefinition} instances.
     */
    private DialogFlowIntentMapper dialogFlowIntentMapper;

    /**
     * The mapper creating DialogFlow {@link EntityType}s from {@link EntityDefinition} instances.
     */
    private DialogFlowEntityMapper dialogFlowEntityMapper;

    /**
     * The mapper creating DialogFlow {@link Context}s from {@link DialogFlowStateContext} instances.
     */
    private DialogFlowContextMapper dialogFlowContextMapper;

    /**
     * The mapper creating DialogFlow entity references from {@link EntityDefinition} references.
     * <p>
     * These references are typically used to refer to {@link EntityType}s in {@link Intent}'s training sentences.
     */
    private DialogFlowEntityReferenceMapper dialogFlowEntityReferenceMapper;

    /**
     * The mapper creating {@link RecognizedIntent}s from {@link QueryResult} instances returned by DialogFlow.
     */
    private RecognizedIntentMapper recognizedIntentMapper;

    /**
     * Constructs a {@link DialogFlowIntentRecognitionProvider} with the provided {@code eventRegistry}, {@code
     * configuration}, and {@code
     * recognitionMonitor}.
     * <p>
     * The behavior of this class can be customized in the provided {@code configuration}. See
     * {@link DialogFlowConfiguration} for more information on the configuration options.
     *
     * @param eventRegistry      the {@link EventDefinitionRegistry} containing the events defined in the current bot
     * @param configuration      the {@link Configuration} holding the DialogFlow project ID and language code
     * @param recognitionMonitor the {@link RecognitionMonitor} instance storing intent matching information
     * @throws NullPointerException if the provided {@code eventRegistry}, {@code configuration} or one of the mandatory
     *                              {@code configuration} value is {@code null}.
     * @throws XatkitException      if an internal error occurred while creating the DialogFlow connector
     * @see DialogFlowConfiguration
     */
    public DialogFlowIntentRecognitionProvider(@NonNull EventDefinitionRegistry eventRegistry,
                                               @NonNull Configuration configuration,
                                               @Nullable RecognitionMonitor recognitionMonitor) {
        Log.info("Starting DialogFlow Client");
        this.configuration = new DialogFlowConfiguration(configuration);
        this.projectAgentName = ProjectAgentName.of(this.configuration.getProjectId());
        try {
            this.dialogFlowClients = new DialogFlowClients(this.configuration);
        } catch (IntentRecognitionProviderException e) {
            throw new XatkitException("An error occurred when creating the DialogFlow clients, see attached " +
                    "exception", e);
        }
        this.projectName = ProjectName.of(this.configuration.getProjectId());
        this.dialogFlowEntityReferenceMapper = new DialogFlowEntityReferenceMapper();
        this.dialogFlowIntentMapper = new DialogFlowIntentMapper(this.configuration,
                this.dialogFlowEntityReferenceMapper);
        this.dialogFlowEntityMapper = new DialogFlowEntityMapper(this.dialogFlowEntityReferenceMapper);
        this.dialogFlowContextMapper = new DialogFlowContextMapper(this.configuration);
        this.recognizedIntentMapper = new RecognizedIntentMapper(this.configuration, eventRegistry);
        try {
            this.cleanAgent();
            this.importRegisteredIntents();
            this.importRegisteredEntities();
        } catch (IntentRecognitionProviderException e) {
            throw new XatkitException(MessageFormat.format("Cannot start the {0}, see attached exception",
                    this.getClass().getSimpleName()), e);
        }
        this.recognitionMonitor = recognitionMonitor;
    }

    /**
     * Deletes all the {@link Intent}s and {@link EntityType}s from the DialogFlow agent.
     * <p>
     * Agent cleaning is enabled by setting the property {@link DialogFlowConfiguration#CLEAN_AGENT_ON_STARTUP_KEY}
     * in the xatkit configuration file, and allows to easily re-deploy bots under development. Production-ready
     * agents should not be cleaned on startup: re-training the ML engine can take a while.
     *
     * @throws IntentRecognitionProviderException if an error occurred when accessing the intent provider
     */
    private void cleanAgent() throws IntentRecognitionProviderException {
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
     * {@link DialogFlowConfiguration#ENABLE_INTENT_LOADING_KEY} property to {@code false} in the provided
     * {@link Configuration}. Note that disabling intents import may generate consistency issues when creating,
     * deleting, and matching intents.
     *
     * @throws IntentRecognitionProviderException if an error occurred when accessing the intent provider
     */
    private void importRegisteredIntents() throws IntentRecognitionProviderException {
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
     * {@link DialogFlowConfiguration#ENABLE_ENTITY_LOADING_KEY} property to {@code false} in the provided
     * {@link Configuration}. Note that disabling entities import may generate consistency issues when creating,
     * deleting, and matching intents.
     *
     * @throws IntentRecognitionProviderException if an error occurred when accessing the intent provider
     */
    private void importRegisteredEntities() throws IntentRecognitionProviderException {
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
     *
     * @return the descriptions of the {@link EntityType}s that are registered in the DialogFlow project
     * @throws IntentRecognitionProviderException if an error occurred when accessing the intent provider
     */
    private List<EntityType> getRegisteredEntityTypes() throws IntentRecognitionProviderException {
        checkNotShutdown();
        List<EntityType> registeredEntityTypes = new ArrayList<>();
        for (EntityType entityType :
                this.dialogFlowClients.getEntityTypesClient().listEntityTypes(projectAgentName).iterateAll()) {
            registeredEntityTypes.add(entityType);
        }
        return registeredEntityTypes;
    }

    /**
     * Returns the partial description of the {@link Intent}s that are registered in the DialogFlow project.
     * <p>
     * The partial descriptions of the {@link Intent}s does not include the {@code training phrases}.
     *
     * @return the partial descriptions of the {@link Intent}s that are registered in the DialogFlow project
     * @throws IntentRecognitionProviderException if an error occurred when accessing the intent provider
     */
    private List<Intent> getRegisteredIntents() throws IntentRecognitionProviderException {
        checkNotShutdown();
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
     * @throws NullPointerException if the provided {@code entityDefinition} is {@code null}
     */
    public void registerEntityDefinition(@NonNull EntityDefinition entityDefinition) throws IntentRecognitionProviderException {
        checkNotShutdown();
        if (entityDefinition instanceof BaseEntityDefinition) {
            BaseEntityDefinition baseEntityDefinition = (BaseEntityDefinition) entityDefinition;
            Log.trace("Skipping registration of {0} ({1}), {0} are natively supported by DialogFlow",
                    BaseEntityDefinition.class.getSimpleName(), baseEntityDefinition.getEntityType().getLiteral());
        } else if (entityDefinition instanceof CustomEntityDefinition) {
            Log.debug("Registering {0} {1}", CustomEntityDefinition.class.getSimpleName(), entityDefinition.getName());
            EntityType entityType = this.registeredEntityTypes.get(entityDefinition.getName());
            if (isNull(entityType)) {
                if (entityDefinition instanceof CompositeEntityDefinition) {
                    this.registerReferencedEntityDefinitions((CompositeEntityDefinition) entityDefinition);
                }
                entityType =
                        dialogFlowEntityMapper.mapEntityDefinition(entityDefinition);
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
                    throw new IntentRecognitionProviderException(MessageFormat.format("Cannot register the entity " +
                            "{0}, the entity already exists", entityDefinition), e);
                }
            } else {
                Log.debug("{0} {1} is already registered", EntityType.class.getSimpleName(),
                        entityDefinition.getName());
            }
        } else {
            throw new IntentRecognitionProviderException(MessageFormat.format("Cannot register the provided {0}, " +
                            "unsupported {1}", entityDefinition.getClass().getSimpleName(),
                    EntityDefinition.class.getSimpleName()));
        }
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
    private void registerReferencedEntityDefinitions(@NonNull CompositeEntityDefinition compositeEntityDefinition) {
        for (CompositeEntityDefinitionEntry entry : compositeEntityDefinition.getEntries()) {
            for (EntityDefinition referredEntityDefinition : entry.getEntities()) {
                if (referredEntityDefinition instanceof CustomEntityDefinition) {
                    /*
                     * Only register CustomEntityDefinitions, the other ones are already part of the system.
                     */
                    try {
                        this.registerEntityDefinition(referredEntityDefinition);
                    } catch (IntentRecognitionProviderException e) {
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
     * {@inheritDoc}
     * <p>
     * This method reuses the information contained in the provided {@link IntentDefinition} to create a new
     * DialogFlow {@link Intent} and add it to the current project.
     *
     * @param intentDefinition the {@link IntentDefinition} to register to the DialogFlow project
     * @throws NullPointerException if the provided {@code intentDefinition} is {@code null}
     * @see DialogFlowIntentMapper
     */
    @Override
    public void registerIntentDefinition(@NonNull IntentDefinition intentDefinition) throws IntentRecognitionProviderException {
        checkNotShutdown();
        checkNotNull(intentDefinition.getName(), "Cannot register the %s with the provided name %s",
                IntentDefinition.class.getSimpleName());
        if (this.registeredIntents.containsKey(intentDefinition.getName())) {
            throw new IntentRecognitionProviderException(MessageFormat.format("Cannot register the intent {0}, the " +
                    "intent already exists", intentDefinition.getName()));
        }
        Log.debug("Registering DialogFlow intent {0}", intentDefinition.getName());
        Intent intent = dialogFlowIntentMapper.mapIntentDefinition(intentDefinition);
        try {
            Intent response = this.dialogFlowClients.getIntentsClient().createIntent(projectAgentName, intent);
            registeredIntents.put(response.getDisplayName(), response);
            Log.debug("Intent {0} successfully registered", response.getDisplayName());
        } catch (FailedPreconditionException | InvalidArgumentException e) {
            if (e.getMessage().contains("already exists")) {
                throw new IntentRecognitionProviderException(MessageFormat.format("Cannot register the intent {0}, " +
                        "the intent already exists", intentDefinition.getName()), e);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the provided {@code entityDefinition} is {@code null}
     */
    @Override
    public void deleteEntityDefinition(@NonNull EntityDefinition entityDefinition) throws IntentRecognitionProviderException {
        checkNotShutdown();
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
            try {
                this.dialogFlowClients.getEntityTypesClient().deleteEntityType(entityType.getName());
            } catch (InvalidArgumentException e) {
                throw new IntentRecognitionProviderException(MessageFormat.format("An error occurred while deleting " +
                        "entity {0}", entityDefinition.getName()), e);
            }
            Log.debug("{0} {1} successfully deleted", EntityType.class.getSimpleName(), entityType.getDisplayName());
            /*
             * Remove the deleted EntityType from the local cache.
             */
            this.registeredEntityTypes.remove(entityType.getDisplayName());
        } else {
            throw new IntentRecognitionProviderException(MessageFormat.format("Cannot delete the provided {0}, " +
                            "unsupported {1}", entityDefinition.getClass().getSimpleName(),
                    EntityDefinition.class.getSimpleName()));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the provided {@code intentDefinition} is {@code null}
     */
    @Override
    public void deleteIntentDefinition(@NonNull IntentDefinition intentDefinition) throws IntentRecognitionProviderException {
        checkNotShutdown();
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
     */
    @Override
    public void trainMLEngine() throws IntentRecognitionProviderException {
        checkNotShutdown();
        Log.info("Starting DialogFlow agent training (this may take a few minutes)");
        TrainAgentRequest request = TrainAgentRequest.newBuilder()
                .setParent(projectName.toString())
                .build();
        // This is the proper way of training an agent, but we've got some issues with it in the past (see this
        // issue https://github.com/xatkit-bot-platform/xatkit-runtime/issues/294).
        boolean isDone = false;
        try {
            isDone =
                    this.dialogFlowClients.getAgentsClient().trainAgentAsync(request).getPollingFuture().get()
                            .isDone();
        } catch (InterruptedException | ExecutionException e) {
            throw new IntentRecognitionProviderException("An error occurred during the DialogFlow agent training", e);
        }
        if (!isDone) {
            throw new IntentRecognitionProviderException("Failed to train the DialogFlow agent, returned " +
                    "Operation#getDone returned false");
        }
        Log.info("DialogFlow agent trained, intent matching will be available in a few seconds");
        try {
            /*
             * From our experience the agent may return DEFAULT_FALLBACK intents in the few seconds after it has been
             * trained. We try to mitigate this by a simple wait.
             */
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new IntentRecognitionProviderException("An error occurred during the DialogFlow agent training", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The created context wraps the internal DialogFlow context that is used on the DialogFlow project to retrieve
     * conversation parts from a given user.
     * <p>
     * The returned {@link StateContext} is configured by the global {@link Configuration} provided in
     * {@link #DialogFlowIntentRecognitionProvider(EventDefinitionRegistry, Configuration, RecognitionMonitor)}.
     *
     * @throws NullPointerException if the provided {@code sessionId} is {@code null}
     */
    @Override
    public StateContext createContext(@NonNull String sessionId) throws IntentRecognitionProviderException {
        checkNotShutdown();
        SessionName sessionName = SessionName.of(this.configuration.getProjectId(), sessionId);
        return new DialogFlowStateContext(sessionName, this.configuration.getBaseConfiguration());
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method ensures the the context values stored in the provided {@code context} are set in the DialogFlow
     * agent when detecting the intent. This ensure that the current state is correctly reflected in DialogFlow.
     * <p>
     * The returned {@link RecognizedIntent} is constructed from the raw {@link Intent} returned by the DialogFlow
     * API, using the mapping defined in {@link RecognizedIntentMapper}.
     * <p>
     * If the {@link DialogFlowConfiguration#ENABLE_LOCAL_CONTEXT_MERGE_KEY} property is set to {@code true} this
     * method will first merge the local context in the remote DialogFlow one, in order to ensure that
     * all the local contexts are propagated to the recognition engine.
     *
     * @throws NullPointerException     if the provided {@code input} or {@code context} is {@code null}
     * @throws IllegalArgumentException if the provided {@code input} is empty
     */
    @Override
    protected RecognizedIntent getIntentInternal(@NonNull String input, @NonNull StateContext context) throws IntentRecognitionProviderException {
        checkNotShutdown();
        checkArgument(!input.isEmpty(), "Cannot retrieve the intent from empty string");
        checkArgument(context instanceof DialogFlowStateContext, "Cannot handle the message, expected context type to be " +
                "%s, found %s", DialogFlowStateContext.class.getSimpleName(), context.getClass().getSimpleName());
        DialogFlowStateContext dialogFlowStateContext = (DialogFlowStateContext) context;

        TextInput.Builder textInput =
                TextInput.newBuilder().setText(input).setLanguageCode(this.configuration.getLanguageCode());
        QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();

        Iterable<Context> contexts = dialogFlowContextMapper.createOutContextsForState(dialogFlowStateContext);

        DetectIntentRequest request = DetectIntentRequest.newBuilder().setQueryInput(queryInput)
                .setQueryParams(QueryParameters.newBuilder()
                        .addAllContexts(contexts)
                        .build())
                .setSession(dialogFlowStateContext.getSessionName().toString())
                .build();

        DetectIntentResponse response;
        try {
            response = this.dialogFlowClients.getSessionsClient().detectIntent(request);
        } catch (Exception e) {
            throw new IntentRecognitionProviderException(e);
        }
        QueryResult queryResult = response.getQueryResult();
        RecognizedIntent recognizedIntent = recognizedIntentMapper.mapQueryResult(queryResult);
        if (nonNull(recognitionMonitor)) {
            recognitionMonitor.logRecognizedIntent(context, recognizedIntent);
        }
        return recognizedIntent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() throws IntentRecognitionProviderException {
        checkNotShutdown();
        this.dialogFlowClients.shutdown();
        if (nonNull(this.recognitionMonitor)) {
            this.recognitionMonitor.shutdown();
        }
    }

    /**
     * Throws a {@link IntentRecognitionProviderException} if the provided
     * {@link DialogFlowIntentRecognitionProvider} is shutdown.
     * <p>
     * This method is typically called in methods that need to interact with the DialogFlow API, and cannot complete
     * if the connector is shutdown.
     *
     * @throws IntentRecognitionProviderException if the provided {@code DialogFlowIntentRecognitionProvider} is
     *                                            shutdown
     * @throws NullPointerException               if the provided {@code DialogFlowIntentRecognitionProvider} is
     *                                            {@code null}
     */
    private void checkNotShutdown() throws IntentRecognitionProviderException {
        if (this.isShutdown()) {
            throw new IntentRecognitionProviderException("Cannot perform the operation, the DialogFlow API is " +
                    "shutdown");
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
