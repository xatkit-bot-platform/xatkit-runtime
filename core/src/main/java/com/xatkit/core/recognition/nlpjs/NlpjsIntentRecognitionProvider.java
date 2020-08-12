package com.xatkit.core.recognition.nlpjs;

import com.xatkit.core.EventDefinitionRegistry;
import com.xatkit.core.recognition.AbstractIntentRecognitionProvider;
import com.xatkit.core.recognition.IntentRecognitionProviderException;
import com.xatkit.core.recognition.RecognitionMonitor;
import com.xatkit.core.recognition.nlpjs.mapper.NlpjsIntentMapper;
import com.xatkit.core.recognition.nlpjs.model.AgentConfig;
import com.xatkit.core.recognition.nlpjs.model.Entity;
import com.xatkit.core.recognition.nlpjs.model.Intent;
import com.xatkit.core.recognition.nlpjs.model.TrainingData;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.RecognizedIntent;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;
import org.apache.commons.configuration2.Configuration;

import javax.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

public class NlpjsIntentRecognitionProvider extends AbstractIntentRecognitionProvider {


    private NlpjsConfiguration configuration;

    private NlpjsIntentMapper nlpjsIntentMapper;

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
        this.nlpjsIntentMapper = new NlpjsIntentMapper(this.configuration);
        this.nlpjsService = new NlpjsService(this.nlpjsServer);

        this.recognitionMonitor = recognitionMonitor;
        this.intentsToRegister = new HashMap<>();
        this.entitiesToRegister = new HashMap<>();

    }


    @Override
    public void registerEntityDefinition(@NonNull EntityDefinition entityDefinition) throws IntentRecognitionProviderException {
    }

    @Override
    public void registerIntentDefinition(@NonNull IntentDefinition intentDefinition) throws IntentRecognitionProviderException {
        checkNotShutdown();
        checkNotNull(intentDefinition.getName(), "Cannot register the %s with the provided name %s",
                IntentDefinition.class.getSimpleName());
        Log.debug("Registering NLP.js intent {0}", intentDefinition.getName());
        Intent intent = nlpjsIntentMapper.mapIntentDefinition(intentDefinition);
        this.intentsToRegister.put(intentDefinition.getName(),intent);
    }

    @Override
    public void deleteEntityDefinition(@NonNull EntityDefinition entityDefinition) throws IntentRecognitionProviderException {

    }

    @Override
    public void deleteIntentDefinition(@NonNull IntentDefinition intentDefinition) throws IntentRecognitionProviderException {

    }

    @Override
    public void trainMLEngine() throws IntentRecognitionProviderException {
        TrainingData trainingData = new TrainingData();
        trainingData.setConfig(new AgentConfig(this.configuration.getLanguageCode()));
        trainingData.setIntents(new ArrayList<>(this.intentsToRegister.values()));
        try {
            this.nlpjsService.trainAgent(agentId,trainingData);
        } catch (IOException e) {
            throw new IntentRecognitionProviderException(e);
        }
    }

    @Override
    public XatkitSession createContext(@NonNull String sessionId) throws IntentRecognitionProviderException {
        return null;
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
        return null;
    }

    @Nullable
    @Override
    public RecognitionMonitor getRecognitionMonitor() {
        return null;
    }

    private void checkNotShutdown() throws IntentRecognitionProviderException {
        if (this.isShutdown()) {
            throw new IntentRecognitionProviderException("Cannot perform the operation, the NLP API is " +
                    "shutdown");
        }
    }
}
