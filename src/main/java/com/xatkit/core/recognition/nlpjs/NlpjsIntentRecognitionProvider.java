package com.xatkit.core.recognition.nlpjs;

import com.xatkit.core.EventDefinitionRegistry;
import com.xatkit.core.recognition.AbstractIntentRecognitionProvider;
import com.xatkit.core.recognition.IntentRecognitionProviderException;
import com.xatkit.core.recognition.RecognitionMonitor;
import com.xatkit.core.recognition.nlpjs.mapper.NlpjsEntityMapper;
import com.xatkit.core.recognition.nlpjs.mapper.NlpjsEntityReferenceMapper;
import com.xatkit.core.recognition.nlpjs.mapper.NlpjsIntentMapper;
import com.xatkit.core.recognition.nlpjs.mapper.NlpjsRecognitionResultMapper;
import com.xatkit.core.recognition.nlpjs.model.Agent;
import com.xatkit.core.recognition.nlpjs.model.AgentConfig;
import com.xatkit.core.recognition.nlpjs.model.AgentStatus;
import com.xatkit.core.recognition.nlpjs.model.Classification;
import com.xatkit.core.recognition.nlpjs.model.Entity;
import com.xatkit.core.recognition.nlpjs.model.Intent;
import com.xatkit.core.recognition.nlpjs.model.RecognitionResult;
import com.xatkit.core.recognition.nlpjs.model.TrainingData;
import com.xatkit.core.recognition.nlpjs.model.UserMessage;
import com.xatkit.core.recognition.processor.SpacePunctuationPreProcessor;
import com.xatkit.core.recognition.processor.TrimPunctuationPostProcessor;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.BaseEntityDefinition;
import com.xatkit.intent.CompositeEntityDefinition;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.CustomEntityDefinition;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.EntityType;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.RecognizedIntent;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ConfigurationConverter;

import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;

/**
 * An {@link AbstractIntentRecognitionProvider} for NLP.js.
 * <p>
 * This class relies on the agent identifier and NLP.js server URL in the provided {@code configuration} to connect
 * to the NLP.js server.
 * <p>
 * NLP.js uses regular expressions to match any entities. These expressions are sensible to spacing and punctuation,
 * which may alter the recognition, but also appear in matched parameters. This class uses two processors to mitigate
 * these issues (see
 * {@link #NlpjsIntentRecognitionProvider(EventDefinitionRegistry, Configuration, RecognitionMonitor)} for more
 * information).
 */
public class NlpjsIntentRecognitionProvider extends AbstractIntentRecognitionProvider {

    /**
     * The {@link NlpjsConfiguration} extracted from the provided {@code configuration}.
     */
    private NlpjsConfiguration configuration;

    /**
     * The mapper creating NLP.js {@link Intent}s from {@link IntentDefinition}s.
     */
    private NlpjsIntentMapper nlpjsIntentMapper;

    /**
     * The mapper creating NLP.js {@link Entity} instances from {@link EntityDefinition}s.
     */
    private NlpjsEntityMapper nlpjsEntityMapper;

    /**
     * The mapper creating NLP.js {@link Entity} references from {@link EntityDefinition} references.
     */
    private NlpjsEntityReferenceMapper nlpjsEntityReferenceMapper;

    /**
     * The mapper creating {@link RecognizedIntent}s from NLP.js {@link RecognitionResult}.
     */
    private NlpjsRecognitionResultMapper nlpjsRecognitionResultMapper;

    /**
     * The client accessing the NLP.js server.
     */
    private NlpjsClient nlpjsClient;

    /**
     * The identifier of the agent managed by this provider.
     */
    private String agentId;

    /**
     * The list of {@link Intent}s to register in the NLP.js agent.
     * <p>
     * This attribute is updated when calling {@link #registerIntentDefinition(IntentDefinition)}. The stored
     * {@link Intent}s are effectively deployed to the NLP.js agent when {@link #trainMLEngine()} is invoked.
     *
     * @see #registerIntentDefinition(IntentDefinition)
     * @see #trainMLEngine()
     */
    private Map<String, Intent> intentsToRegister;

    /**
     * The list of {@link Entity} instances ot register in the NLP.js agent.
     * <p>
     * This attribute is updated when calling {@link #registerEntityDefinition(EntityDefinition)}. The stored
     * {@link Entity} instances are effectively deployed to the NLP.js agent when {@link #trainMLEngine()} is invoked.
     * <p>
     * This attribute is also updated when calling {@link #registerIntentDefinition(IntentDefinition)} for
     * {@link IntentDefinition}s with references to {@code any} entities.
     *
     * @see #registerEntityDefinition(EntityDefinition)
     * @see #registerIntentDefinition(IntentDefinition)
     * @see #trainMLEngine()
     */
    private Map<String, Entity> entitiesToRegister;

    /**
     * A flag indicating if the provider has been shutdown.
     * <p>
     * A shutdown provider cannot be used to extract or configure intents and entities, and will throw an
     * {@link IntentRecognitionProviderException} when accessed.
     * <p>
     * This flag is set to {@code false} by default, and is updated iff the framework calls the {@link #shutdown()}
     * method (typically when a bot is stopped). The value of this flag is used in combination with internal checks
     * to compute the value to return by {@link #isShutdown()}.
     *
     * @see #shutdown()
     * @see #isShutdown()
     */
    private boolean isShutdown = false;


    /**
     * The {@link RecognitionMonitor} used to track intent matching information.
     */
    @Nullable
    private RecognitionMonitor recognitionMonitor;


    /**
     * Initializes the {@link NlpjsIntentRecognitionProvider}.
     * <p>
     * This method relies on the agent identifier and NLP.js server URL in the provided {@code configuration} to
     * connect to the NLP.js server. An {@link IllegalArgumentException} is thrown if the {@code configuration} does not
     * contains these values.
     * <p>
     * NLP.js uses regular expressions to match any entities. These expressions are sensible to spacing and
     * punctuation, which may alter the recognition, but also appear in matched parameters. This class uses two
     * processors to mitigate these issues:
     * <ul>
     *     <li>{@link TrimPunctuationPostProcessor}: removes the punctuation from matched parameters (e.g.
     *     "Barcelona?" becomes "Barcelona")</li>
     *     <li>{@link SpacePunctuationPreProcessor}: adds an extra space before punctuation symbol to ease the
     *     recognition based on regular expressions (e.g. input "Barcelona?" is pre-processed as "Barcelona ?")</li>
     * </ul>
     * See {@link #getPreProcessors()} and {@link #getPostProcessors()} to access the list of processors configured
     * with this class.
     *
     * @param eventRegistry      the {@link EventDefinitionRegistry} containing the events defined in the current bot
     * @param configuration      the {@link Configuration} holding the DialogFlow project ID and language code
     * @param recognitionMonitor the {@link RecognitionMonitor} instance storing intent matching information
     * @throws NullPointerException     if the provided {@code eventRegistry} or {@code configuration} is {@code null}
     * @throws IllegalArgumentException if the provided {@code configuration} does not contain the required
     *                                  information to connect to the NLP.js server
     */
    public NlpjsIntentRecognitionProvider(@NonNull EventDefinitionRegistry eventRegistry,
                                          @NonNull Configuration configuration,
                                          @Nullable RecognitionMonitor recognitionMonitor) {
        Log.info("Starting NLP.js Client");
        this.getPostProcessors().add(new TrimPunctuationPostProcessor());
        this.getPreProcessors().add(new SpacePunctuationPreProcessor());
        this.configuration = new NlpjsConfiguration(configuration);
        this.agentId = this.configuration.getAgentId();
        this.nlpjsEntityReferenceMapper = new NlpjsEntityReferenceMapper();
        this.nlpjsIntentMapper = new NlpjsIntentMapper(nlpjsEntityReferenceMapper);
        this.nlpjsRecognitionResultMapper = new NlpjsRecognitionResultMapper(eventRegistry, nlpjsEntityReferenceMapper);
        this.nlpjsClient = new NlpjsClient(this.configuration.getNlpjsServer());
        this.nlpjsEntityMapper = new NlpjsEntityMapper(this.nlpjsEntityReferenceMapper);
        this.recognitionMonitor = recognitionMonitor;
        this.intentsToRegister = new HashMap<>();
        this.entitiesToRegister = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method does not register {@link BaseEntityDefinition} because they are registered by default in NLP.js
     * agents. {@code Any} entities are handled by {@link #registerIntentDefinition(IntentDefinition)} because they
     * require additional information from the {@link IntentDefinition} referring to them.
     * <p>
     * This method does not support {@link CompositeEntityDefinition}s.
     * <p>
     * This method does not access the NLP.js server, see {@link #trainMLEngine()} to serialize the registered
     * entities and train the NLP.js agent.
     *
     * @param entityDefinition the {@link EntityDefinition} to register in the NLP.js agent
     * @throws NullPointerException     if the provided {@code entityDefinition} is {@code null}
     * @throws IllegalArgumentException if the provided {@code entityDefinition} is a {@link CompositeEntityDefinition}
     */
    @SuppressWarnings("checkstyle:EmptyBlock")
    @Override
    public void registerEntityDefinition(@NonNull EntityDefinition entityDefinition) throws IntentRecognitionProviderException {
        checkNotShutdown();
        if (entityDefinition instanceof BaseEntityDefinition) {
            /*
             * Do nothing, we don't register BaseEntityDefinition instances in NLP.js
             */
        } else if (entityDefinition instanceof CustomEntityDefinition) {
            Log.debug("Registering {0} {1}", CustomEntityDefinition.class.getSimpleName(), entityDefinition.getName());
            if (entityDefinition instanceof CompositeEntityDefinition) {
                throw new IllegalArgumentException(MessageFormat.format("Cannot register the entity {0}. "
                        + "Composite entities are not supported by NLP.js", entityDefinition));
            } else {
                Entity entity = this.nlpjsEntityMapper.mapEntityDefinition(entityDefinition);
                this.entitiesToRegister.put(entityDefinition.getName(), entity);
            }

        } else {
            throw new IntentRecognitionProviderException(MessageFormat.format("Cannot register the provided {0}, "
                            + "unsupported {1}", entityDefinition.getClass().getSimpleName(),
                    EntityDefinition.class.getSimpleName()));
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method degrades the {@link BaseEntityDefinition}s used in the provided {@code intentDefinition} to {@code
     * any} if they are not supported by NLP.js. This relaxes intent matching but allows to support any Xatkit
     * {@link BaseEntityDefinition}s.
     * <p>
     * This method also registers the {@code any} entities that are referenced in the provided {@code
     * intentDefinition}. Since NLP.js requires intent-level information to register these entities this process
     * cannot be done in {@link #registerEntityDefinition(EntityDefinition)}.
     * <p>
     * This method does not access the NLP.js server, see {@link #trainMLEngine()} to serialize the registered
     * intents and train the NLP.js agent.
     *
     * @param intentDefinition the {@link IntentDefinition} to register in the NLP.js agent
     * @throws NullPointerException               if the provided {@code intentDefinition} is {@code null}
     * @throws IntentRecognitionProviderException if the provided {@code intentDefinition} is already registered
     */
    @Override
    public void registerIntentDefinition(@NonNull IntentDefinition intentDefinition) throws IntentRecognitionProviderException {
        checkNotShutdown();
        checkNotNull(intentDefinition.getName(), "Cannot register the %s with the provided name %s",
                IntentDefinition.class.getSimpleName(), intentDefinition.getName());
        if (this.intentsToRegister.containsKey(intentDefinition.getName())) {
            throw new IntentRecognitionProviderException(MessageFormat.format("Cannot register the intent {0}, the "
                    + "intent already exists", intentDefinition.getName()));
        }
        this.degradeUnsupportedBaseEntities(intentDefinition);
        Log.debug("Registering NLP.js intent {0}", intentDefinition.getName());
        /*
         * We need to deal with the any entities here because they are configured with the intent's name and training
         *  sentences.
         */
        Collection<Entity> anyEntities = nlpjsEntityMapper.mapAnyEntityDefinition(intentDefinition);
        anyEntities.forEach(e -> this.entitiesToRegister.put(e.getEntityName(), e));
        Intent intent = nlpjsIntentMapper.mapIntentDefinition(intentDefinition);
        this.intentsToRegister.put(intentDefinition.getName(), intent);
    }

    /**
     * Degrades the unsupported {@link BaseEntityDefinition} referenced in {@code intentDefinition} to {@code any}.
     * <p>
     * NLP.js does not support all the Xatkit {@link BaseEntityDefinition}. This method mitigates this issue by
     * degrading these entities to {@code any}, allowing to register them in the NLP.js agent. If an entity is
     * degraded this way a warning log message is printed to the user because the agent may wrongly match some inputs.
     *
     * @param intentDefinition the {@link IntentDefinition} to degrade
     * @throws NullPointerException if the provided {@code intentDefinition} is {@code null}
     */
    private void degradeUnsupportedBaseEntities(@NonNull IntentDefinition intentDefinition) {
        for (ContextParameter parameter : intentDefinition.getParameters()) {
            if (nlpjsEntityReferenceMapper.getMappingFor(parameter.getEntity().getReferredEntity()).equals("none")) {
                if (parameter.getEntity().getReferredEntity() instanceof BaseEntityDefinition) {
                    /*
                     * Gracefully degrades the provided parameter type to "any" if the base entity it refers to
                     * is not supported. This allows NLP.js to match the entity, but it is more permissive
                     * than a correct implementation of it (it will basically match any value for the
                     * parameter).
                     * This pre-processing is done before any-specific entity configuration, allowing to
                     * reuse before/after extraction rules or regex, depending on the nature of the training
                     * sentence.
                     */
                    BaseEntityDefinition baseEntityDefinition =
                            (BaseEntityDefinition) parameter.getEntity().getReferredEntity();
                    baseEntityDefinition.setEntityType(EntityType.ANY);
                    Log.warn("Entity \"{0}\" (intent: {1}, parameter: {2}) is not supported by NLP.js, Xatkit will "
                                    + "gracefully degrade to using an \"any\" entity instead. This makes the "
                                    + "recognition more permissive and may produce false positive matches. You can "
                                    + "migrate your bot to another provider if needed.",
                            baseEntityDefinition.getName(), intentDefinition.getName(),
                            parameter.getName());
                }
            }
        }
    }

    /**
     * This method is not supported by the {@link NlpjsIntentRecognitionProvider}.
     *
     * @param entityDefinition the {@link EntityDefinition} to delete from the NLP.js agent
     * @throws UnsupportedOperationException when called
     */
    @Override
    public void deleteEntityDefinition(@NonNull EntityDefinition entityDefinition) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * This method is not supported by the {@link NlpjsIntentRecognitionProvider}.
     *
     * @param intentDefinition the {@link IntentDefinition} to delete from the NLP.js agent
     * @throws UnsupportedOperationException when called
     */
    @Override
    public void deleteIntentDefinition(@NonNull IntentDefinition intentDefinition) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method serializes the intents/entities stored in this class and deploy them to the NLP.js agent.
     *
     * @throws IntentRecognitionProviderException if an error occurred while training the NLP.js agent
     */
    @Override
    public void trainMLEngine() throws IntentRecognitionProviderException {
        checkNotShutdown();
        Log.info("Starting NLP.js agent training (this may take a few minutes)");
        TrainingData trainingData = TrainingData.builder()
                .config(new AgentConfig(this.configuration.getLanguageCode(),
                        this.configuration.isCleanAgentOnStartup()))
                .intents(new ArrayList<>(this.intentsToRegister.values()))
                .entities(new ArrayList<>(this.entitiesToRegister.values()))
                .build();
        try {
            /*
             * We try to train the agent 10 times (every 2 seconds) in case there is a network issue. After these 10
             * tries we assume the NLP.js server is unreachable and throw an exception.
             */
            boolean isDone = false;
            int attemptsLeft = 10;
            this.nlpjsClient.trainAgent(agentId, trainingData);
            while (!isDone && attemptsLeft > 0) {
                Thread.sleep(2000);
                Agent agent = this.nlpjsClient.getAgentInfo(this.agentId);
                if (agent.getStatus().equals(AgentStatus.READY)) {
                    isDone = true;
                } else {
                    attemptsLeft--;
                }
            }
            if (isDone) {
                Log.info("NLP.js agent trained.");
            } else {
                throw new IntentRecognitionProviderException("Failed to train the NLP.js agent");
            }
        } catch (NlpjsClientException | InterruptedException e) {
            throw new IntentRecognitionProviderException("An error occurred during the NLP agent training: ", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StateContext createContext(@NonNull String sessionId) {
        /*
         * FIXME duplicated code from RegExIntentRecognitionProvider
         */
        StateContext stateContext = ExecutionFactory.eINSTANCE.createStateContext();
        stateContext.setContextId(sessionId);
        stateContext.setConfiguration(ConfigurationConverter.getMap(configuration.getBaseConfiguration()));
        return stateContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() throws IntentRecognitionProviderException {
        checkNotShutdown();
        if (nonNull(this.recognitionMonitor)) {
            this.recognitionMonitor.shutdown();
        }
        this.isShutdown = true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method returns {@code true} if the NLP.js server is unreachable, or if {@link #shutdown()} has been called.
     *
     * @return {@code true} if the intent recognition provider client is shutdown, {@code false} otherwise
     */
    @Override
    public boolean isShutdown() {
        /*
         * We use the isShutdown flag here in case the provider has been stopped by the framework, but we also want
         * to return true if the underlying NlpjsService is not able to access the agent.
         */
        return this.isShutdown || nlpjsClient.isShutdown();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException               if the provided {@code input} or {@code context} is {@code null}
     * @throws IntentRecognitionProviderException if an error occurred when accessing the intent provider
     */
    @Override
    protected RecognizedIntent getIntentInternal(@NonNull String input, @NonNull StateContext context)
            throws IntentRecognitionProviderException {
        checkNotShutdown();
        checkArgument(!input.isEmpty(), "Cannot retrieve the intent from empty string");
        try {
            RecognizedIntent recognizedIntent;
            UserMessage userMessage = new UserMessage(input);
            RecognitionResult recognitionResult = this.nlpjsClient.getIntent(agentId, userMessage);
            List<Classification> classifications = recognitionResult.getClassifications();
            if (!classifications.isEmpty() && classifications.get(0).getIntent().equals("None")) {
                recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
                recognizedIntent.setDefinition(DEFAULT_FALLBACK_INTENT);
                recognizedIntent.setRecognitionConfidence(recognitionResult.getScore());
                recognizedIntent.setMatchedInput(recognitionResult.getUtterance());
            } else {
                List<RecognizedIntent> recognizedIntents =
                        nlpjsRecognitionResultMapper.mapRecognitionResult(recognitionResult);
                recognizedIntent = getBestCandidate(recognizedIntents, context);
                recognizedIntent.getValues().addAll(nlpjsRecognitionResultMapper.mapParameterValues(
                        (IntentDefinition) recognizedIntent.getDefinition(), recognitionResult.getEntities()));
            }
            if (nonNull(recognitionMonitor)) {
                recognitionMonitor.logRecognizedIntent(context, recognizedIntent);
            }
            return recognizedIntent;

        } catch (NlpjsClientException e) {
            throw new IntentRecognitionProviderException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public RecognitionMonitor getRecognitionMonitor() {
        return this.recognitionMonitor;
    }

    /**
     * Checks that the {@link NlpjsIntentRecognitionProvider} is not shutdown.
     * <p>
     * This method is called before any register/delete operation to make sure the NLP.js configuration is not
     * updated if the provider is shutdown.
     *
     * @throws IntentRecognitionProviderException if the provider is shutdown.
     */
    private void checkNotShutdown() throws IntentRecognitionProviderException {
        if (this.isShutdown()) {
            throw new IntentRecognitionProviderException("Cannot perform the operation, the NLP API is shutdown");
        }
    }
}
