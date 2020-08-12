package com.xatkit.core.recognition.nlpjs;

import com.xatkit.core.recognition.nlpjs.model.*;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class NlpjsService {

    private final static String NLPJS_BASE_PATH = "/api";

    private String nlpjsServer;

    private NlpjsApi nlpjsApi;

    public NlpjsService(@NonNull String nlpjsServer){
        this.nlpjsServer = nlpjsServer;
        String nlpjsApiFullPath = this.nlpjsServer + NLPJS_BASE_PATH;
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .readTimeout(3, TimeUnit.SECONDS)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(nlpjsApiFullPath)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create()).build();
            this.nlpjsApi = retrofit.create(NlpjsApi.class);
    }


    public Agent getAgentInfo(String agentId) throws IOException {
        return nlpjsApi.getAgentInfo(agentId).execute().body();
    }


    public boolean createAgent(AgentInit agentInit) throws IOException {
        nlpjsApi.createAgent(agentInit).execute();
        return true;
    }

    public boolean trainAgent(String agentId, TrainingData trainingData) throws IOException {
        nlpjsApi.trainAgent(agentId, trainingData).execute();
        return true;
    }

    public RecognizedIntent getIntent(String agentId, UserMessage userMessage) throws IOException {
        return nlpjsApi.getIntent(agentId,userMessage).execute().body();
    }

    public boolean isShutdown() {
        try {
            Response<Agent> response = nlpjsApi.getAgentInfo("default").execute();
            return response.isSuccessful();
        } catch (IOException e) {
            Log.error("The NLP.js API is not responding. See the attached error: {0}", e.getMessage());
            return true;
        }
    }
}
