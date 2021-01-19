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
import com.xatkit.intent.CustomEntityDefinition;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.RecognizedIntent;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ConfigurationConverter;

import javax.annotation.Nullable;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;

public class NlpjsIntentRecognitionProvider extends AbstractIntentRecognitionProvider {


    private NlpjsConfiguration configuration;

    private NlpjsIntentMapper nlpjsIntentMapper;

    private NlpjsEntityMapper nlpjsEntityMapper;

    private NlpjsEntityReferenceMapper nlpjsEntityReferenceMapper;

    private NlpjsRecognitionResultMapper nlpjsRecognitionResultMapper;

    private NlpjsClient nlpjsClient;

    private String agentId;

    private String nlpjsServer;

    private Map<String, Intent> intentsToRegister;

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


    public NlpjsIntentRecognitionProvider(@NonNull EventDefinitionRegistry eventRegistry, @NonNull Configuration configuration,
                                          @Nullable RecognitionMonitor recognitionMonitor) {
        Log.info("Starting NLP.js Client");
        /*
         * Nlp.js uses regular expressions to match any entities. These expressions are quite sensible to spacing and
         * punctuation, which may alter the recognition, but also appear in matched parameter. We use the following
         * processors to mitigate these issues:
         * - TrimPunctuationPostProcessor: remove punctuation from matched parameters (e.g. "Barcelona?" becomes
         * "Barcelona")
         * - SpacePunctuationPreProcessor: adds an extra space before punctuation symbol to ease the recognition
         * based on regular expressions (e.g. input "Barcelona?" is pre-processed as "Barcelona ?" before it is sent
         * for recognition).
         * These capabilities are defined a processors because they may be useful for other use cases, even for bots
         * not using Nlp.js.
         */
        this.getPostProcessors().add(new TrimPunctuationPostProcessor());
        this.getPreProcessors().add(new SpacePunctuationPreProcessor());
        this.configuration = new NlpjsConfiguration(configuration);
        this.agentId = this.configuration.getAgentId();
        this.nlpjsServer = this.configuration.getNlpjsServer();
        this.nlpjsEntityReferenceMapper = new NlpjsEntityReferenceMapper();
        this.nlpjsIntentMapper = new NlpjsIntentMapper(this.configuration, nlpjsEntityReferenceMapper);
        this.nlpjsRecognitionResultMapper = new NlpjsRecognitionResultMapper(this.configuration, eventRegistry, nlpjsEntityReferenceMapper);
        this.nlpjsClient = new NlpjsClient(this.nlpjsServer);
        this.nlpjsEntityMapper = new NlpjsEntityMapper();
        this.recognitionMonitor = recognitionMonitor;
        this.intentsToRegister = new HashMap<>();
        this.entitiesToRegister = new HashMap<>();
    }

    @Override
    public void registerEntityDefinition(@NonNull EntityDefinition entityDefinition) throws IntentRecognitionProviderException {
        checkNotShutdown();
        if (entityDefinition instanceof BaseEntityDefinition) {
            BaseEntityDefinition baseEntityDefinition = (BaseEntityDefinition) entityDefinition;
            if (!nlpjsEntityReferenceMapper.isSupported(baseEntityDefinition.getEntityType())) {
                Log.warn("Entity \"{0}\" is not supported by NLP.js, Xatkit will gracefully degrade to using an "
                        + "\"any\" entity instead. This makes the recognition more permissive and may produce "
                                + "false positive matches. You can migrate your bot to another provider if needed.",
                        entityDefinition.getName());
            }
        } else if (entityDefinition instanceof CustomEntityDefinition) {
            Log.debug("Registering {0} {1}", CustomEntityDefinition.class.getSimpleName(), entityDefinition.getName());
            if (entityDefinition instanceof CompositeEntityDefinition) {
                throw new IntentRecognitionProviderException(MessageFormat.format("Cannot register the entity " +
                        "{0}. Composite entities are not supported by NLP.js", entityDefinition));
            } else {
                Entity entity = this.nlpjsEntityMapper.mapEntiyDefinition(entityDefinition);
                this.entitiesToRegister.put(entityDefinition.getName(), entity);
            }

        } else {
            throw new IntentRecognitionProviderException(MessageFormat.format("Cannot register the provided {0}, " +
                            "unsupported {1}", entityDefinition.getClass().getSimpleName(),
                    EntityDefinition.class.getSimpleName()));
        }
    }

    @Override
    public void registerIntentDefinition(@NonNull IntentDefinition intentDefinition) throws IntentRecognitionProviderException {
        checkNotShutdown();
        checkNotNull(intentDefinition.getName(), "Cannot register the %s with the provided name %s",
                IntentDefinition.class.getSimpleName(), intentDefinition.getName());
        if (this.intentsToRegister.containsKey(intentDefinition.getName())) {
            throw new IntentRecognitionProviderException(MessageFormat.format("Cannot register the intent {0}, the " +
                    "intent already exists", intentDefinition.getName()));
        }
        Log.debug("Registering NLP.js intent {0}", intentDefinition.getName());
        List<Entity> anyEntitiesCollector = new ArrayList<>();
        Intent intent = nlpjsIntentMapper.mapIntentDefinition(intentDefinition,anyEntitiesCollector);
        if (!anyEntitiesCollector.isEmpty()) {
            for (Entity entity: anyEntitiesCollector) {
                this.entitiesToRegister.put(entity.getEntityName(),entity);
            }
        }
        this.intentsToRegister.put(intentDefinition.getName(), intent);
    }

    @Override
    public void deleteEntityDefinition(@NonNull EntityDefinition entityDefinition) throws IntentRecognitionProviderException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void deleteIntentDefinition(@NonNull IntentDefinition intentDefinition) throws IntentRecognitionProviderException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void trainMLEngine() throws IntentRecognitionProviderException {
        checkNotShutdown();
        Log.info("Starting NLP.js agent training (this may take a few minutes)");
        TrainingData trainingData = TrainingData.builder()
                .config(new AgentConfig(this.configuration.getLanguageCode(), this.configuration.isCleanAgentOnStartup()))
                .intents(new ArrayList<>(this.intentsToRegister.values()))
                .entities(new ArrayList<>(this.entitiesToRegister.values()))
                .build();
        try {
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
        } catch (IOException | InterruptedException e) {
            throw new IntentRecognitionProviderException("An error occurred during the NLP agent training: ", e);
        }
    }

    @Override
    public StateContext createContext(@NonNull String sessionId) throws IntentRecognitionProviderException {
        /*
         * FIXME duplicated code from RegExIntentRecognitionProvider
         */
        StateContext stateContext = ExecutionFactory.eINSTANCE.createStateContext();
        stateContext.setContextId(sessionId);
        stateContext.setConfiguration(ConfigurationConverter.getMap(configuration.getBaseConfiguration()));
        return stateContext;
    }

    @Override
    public void shutdown() throws IntentRecognitionProviderException {
        checkNotShutdown();
        if (nonNull(this.recognitionMonitor)) {
            this.recognitionMonitor.shutdown();
        }
        this.isShutdown = true;
    }

    @Override
    public boolean isShutdown() {
        /*
         * We use the isShutdown flag here in case the provider has been stopped by the framework, but we also want
         * to return true if the underlying NlpjsService is not able to access the agent.
         */
        return this.isShutdown || nlpjsClient.isShutdown();
    }

    @Override
    protected RecognizedIntent getIntentInternal(@NonNull String input, @NonNull StateContext context) throws IntentRecognitionProviderException {
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
                List<RecognizedIntent> recognizedIntents = nlpjsRecognitionResultMapper.mapRecognitionResult(recognitionResult);
                recognizedIntent = getBestCandidate(recognizedIntents, context);
                recognizedIntent.getValues().addAll(nlpjsRecognitionResultMapper.mapParameterValues(recognizedIntent,
                        recognitionResult.getEntities()));
            }
            if (nonNull(recognitionMonitor)) {
                recognitionMonitor.logRecognizedIntent(context, recognizedIntent);
            }
            return recognizedIntent;

        } catch (IOException e) {
            throw new IntentRecognitionProviderException(e);
        }
    }

    @Nullable
    @Override
    public RecognitionMonitor getRecognitionMonitor() {
        return this.recognitionMonitor;
    }

    private void checkNotShutdown() throws IntentRecognitionProviderException {
        if (this.isShutdown()) {
            throw new IntentRecognitionProviderException("Cannot perform the operation, the NLP API is " +
                    "shutdown");
        }
    }
}
