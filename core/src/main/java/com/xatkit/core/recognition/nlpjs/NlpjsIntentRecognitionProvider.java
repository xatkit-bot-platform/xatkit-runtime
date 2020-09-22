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

import javax.annotation.Nullable;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

public class NlpjsIntentRecognitionProvider extends AbstractIntentRecognitionProvider {


    private NlpjsConfiguration configuration;

    private NlpjsIntentMapper nlpjsIntentMapper;

    private NlpjsEntityMapper nlpjsEntityMapper;

    private NlpjsEntityReferenceMapper nlpjsEntityReferenceMapper;

    private NlpjsRecognitionResultMapper nlpjsRecognitionResultMapper;

    private NlpjsService nlpjsService;

    private String agentId;

    private String nlpjsServer;

    private Map<String, Intent> intentsToRegister;

    private Map<String, Entity> entitiesToRegister;


    /**
     * The {@link RecognitionMonitor} used to track intent matching information.
     */
    @Nullable
    private RecognitionMonitor recognitionMonitor;


    public NlpjsIntentRecognitionProvider(@NonNull EventDefinitionRegistry eventRegistry, @NonNull Configuration configuration,
                                          @Nullable RecognitionMonitor recognitionMonitor) {
        Log.info("Starting NLP.js Client");
        this.configuration = new NlpjsConfiguration(configuration);
        this.agentId = this.configuration.getAgentId();
        this.nlpjsServer = this.configuration.getNlpjsServer();
        this.nlpjsEntityReferenceMapper = new NlpjsEntityReferenceMapper();
        this.nlpjsIntentMapper = new NlpjsIntentMapper(this.configuration, nlpjsEntityReferenceMapper);
        this.nlpjsRecognitionResultMapper = new NlpjsRecognitionResultMapper(this.configuration, eventRegistry, nlpjsEntityReferenceMapper);
        this.nlpjsService = new NlpjsService(this.nlpjsServer);
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
            if (nlpjsEntityReferenceMapper.isSupported(baseEntityDefinition.getEntityType())) {
                throw new IntentRecognitionProviderException(MessageFormat.format("Cannot register the entity " +
                        "{0}. This type is not supported by NLP.js", entityDefinition));
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
        Intent intent = nlpjsIntentMapper.mapIntentDefinition(intentDefinition);
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
        TrainingData trainingData = TrainingData.newBuilder()
                .config(new AgentConfig(this.configuration.getLanguageCode()))
                .intents(new ArrayList<>(this.intentsToRegister.values()))
                .entities(new ArrayList<>(this.entitiesToRegister.values()))
                .build();
        try {
            boolean isDone = false;
            int attemptsLeft = 10;
            this.nlpjsService.trainAgent(agentId, trainingData);
            while (!isDone && attemptsLeft > 0) {
                Thread.sleep(2000);
                Agent agent = this.nlpjsService.getAgentInfo(this.agentId);
                if (agent.getStatus().equals(AgentStatus.READY))
                    isDone = true;
                else
                    attemptsLeft--;
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
        StateContext stateContext = ExecutionFactory.eINSTANCE.createStateContext();
        stateContext.setContextId(sessionId);
        return stateContext;
    }

    @Override
    public void shutdown() throws IntentRecognitionProviderException {

    }

    @Override
    public boolean isShutdown() {
        return nlpjsService.isShutdown();
    }

    @Override
    protected RecognizedIntent getIntentInternal(@NonNull String input, @NonNull StateContext context) throws IntentRecognitionProviderException {
        checkNotShutdown();
        checkArgument(!input.isEmpty(), "Cannot retrieve the intent from empty string");
        try {
            UserMessage userMessage = new UserMessage(input);
            RecognitionResult recognitionResult = this.nlpjsService.getIntent(agentId, userMessage);
            List<Classification> classifications = recognitionResult.getClassifications();
            if (!classifications.isEmpty() && classifications.get(0).getIntent().equals("None")) {
                RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
                recognizedIntent.setDefinition(DEFAULT_FALLBACK_INTENT);
                recognizedIntent.setRecognitionConfidence(recognitionResult.getScore());
                recognizedIntent.setMatchedInput(recognitionResult.getUtterance());
                return recognizedIntent;
            }
            List<RecognizedIntent> recognizedIntents = nlpjsRecognitionResultMapper.mapRecognitionResult(recognitionResult);
            RecognizedIntent recognizedIntent = getBestCandidate(recognizedIntents, context);
            recognizedIntent.getValues().addAll(nlpjsRecognitionResultMapper.mapParamterValues(recognizedIntent,
                    recognitionResult.getEntities()));
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
