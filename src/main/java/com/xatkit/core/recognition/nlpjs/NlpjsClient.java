package com.xatkit.core.recognition.nlpjs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xatkit.core.recognition.IntentRecognitionProviderException;
import com.xatkit.core.recognition.nlpjs.adapter.ExtractedEntityDeserializer;
import com.xatkit.core.recognition.nlpjs.model.Agent;
import com.xatkit.core.recognition.nlpjs.model.AgentInit;
import com.xatkit.core.recognition.nlpjs.model.ErrorBody;
import com.xatkit.core.recognition.nlpjs.model.ExtractedEntity;
import com.xatkit.core.recognition.nlpjs.model.RecognitionResult;
import com.xatkit.core.recognition.nlpjs.model.TrainingData;
import com.xatkit.core.recognition.nlpjs.model.UserMessage;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

public class NlpjsClient {

    private static final String NLPJS_API_BASE_PATH = "/api";

    private NlpjsApi nlpjsApi;

    private Gson gson;

    public NlpjsClient(@NonNull String nlpjsServerUrl) {
        gson = new GsonBuilder().registerTypeAdapter(ExtractedEntity.class, new ExtractedEntityDeserializer())
                .create();
        String nlpjsApiUrl = nlpjsServerUrl + NLPJS_API_BASE_PATH + "/";
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor(message -> {
                    message = message.replaceAll("'", "''");
                    message = message.replaceAll("\\{", "'{'");
                    message = message.replaceAll("}", "'}'");
                    Log.debug(message);
                }).setLevel(HttpLoggingInterceptor.Level.BODY))
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(nlpjsApiUrl)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create(gson)).build();
        this.nlpjsApi = retrofit.create(NlpjsApi.class);
    }


    public Agent getAgentInfo(String agentId) throws IOException, IntentRecognitionProviderException {
        Response<Agent> agentResponse = nlpjsApi.getAgentInfo(agentId).execute();
        if (!agentResponse.isSuccessful()) {
            ErrorBody errorBody = gson.fromJson(agentResponse.errorBody().string(), ErrorBody.class);
            String errorMessage = MessageFormat.format("Unsuccessful REST operation {0} on {1}. The API responded "
                            + "with the status {2} and the error message \"{3}\" ",
                    nlpjsApi.getAgentInfo(agentId).request().method(),
                    nlpjsApi.getAgentInfo(agentId).request().url(), agentResponse.code(), errorBody.getMessage());
            throw new IntentRecognitionProviderException(errorMessage);
        }
        return agentResponse.body();
    }


    public boolean createAgent(AgentInit agentInit) throws IOException, IntentRecognitionProviderException {
        Response<Void> response = nlpjsApi.createAgent(agentInit).execute();
        if (!response.isSuccessful()) {
            ErrorBody errorBody = gson.fromJson(response.errorBody().string(), ErrorBody.class);
            String errorMessage = MessageFormat.format("Unsuccessful REST operation {0} on {1}. The API responded "
                            + "with the status {2} and the error message \"{3}\" ",
                    nlpjsApi.createAgent(agentInit).request().method(),
                    nlpjsApi.createAgent(agentInit).request().url(), response.code(), errorBody.getMessage());
            throw new IntentRecognitionProviderException(errorMessage);
        }
        return true;
    }

    public boolean trainAgent(String agentId, TrainingData trainingData) throws IOException,
            IntentRecognitionProviderException {
        Response<Void> response = nlpjsApi.trainAgent(agentId, trainingData).execute();
        if (!response.isSuccessful()) {
            ErrorBody errorBody = gson.fromJson(response.errorBody().string(), ErrorBody.class);
            String errorMessage = MessageFormat.format("Unsuccessful REST operation {0} on {1}. The API responded "
                            + "with the status {2} and the error message: {3} ",
                    nlpjsApi.trainAgent(agentId, trainingData).request().method(),
                    nlpjsApi.trainAgent(agentId, trainingData).request().url(), response.code(),
                    errorBody.getMessage());
            throw new IntentRecognitionProviderException(errorMessage);
        }
        return true;
    }

    public RecognitionResult getIntent(@NonNull String agentId, @NonNull UserMessage userMessage) throws IOException,
            IntentRecognitionProviderException {
        Response<RecognitionResult> response = nlpjsApi.getIntent(agentId, userMessage).execute();
        if (!response.isSuccessful()) {
            ErrorBody errorBody = gson.fromJson(response.errorBody().string(), ErrorBody.class);
            String errorMessage = MessageFormat.format("Unsuccessful REST operation {0} on {1}. The API responded "
                            + "with the status {2} and the error code {3} ",
                    nlpjsApi.getIntent(agentId, userMessage).request().method(),
                    nlpjsApi.getIntent(agentId, userMessage).request().url(), response.code(), errorBody.getMessage());
            throw new IntentRecognitionProviderException(errorMessage);
        }
        return response.body();
    }

    public boolean isShutdown() {
        try {
            Response<Agent> response = nlpjsApi.getAgentInfo("default").execute();
            return !response.isSuccessful();
        } catch (IOException e) {
            Log.error("The NLP.js API is not responding. See the attached error: {0}", e);
            return true;
        }
    }
}
