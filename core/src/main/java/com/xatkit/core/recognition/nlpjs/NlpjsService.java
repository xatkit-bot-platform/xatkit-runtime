package com.xatkit.core.recognition.nlpjs;

import com.xatkit.core.recognition.nlpjs.model.*;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;

public class NlpjsService {

    private static NlpjsService INSTANCE = null;
    private NlpjsApi nlpjsApi;

    private NlpjsService(NlpjsApi nlpjsApi){
        this.nlpjsApi = nlpjsApi;
    }

    public static NlpjsService getInstance(){
        if(INSTANCE == null){
            OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                    .build();
            Retrofit retrofit = new Retrofit.Builder().baseUrl("http://localhost:8080/api/").client(httpClient)
                    .addConverterFactory(GsonConverterFactory.create()).build();
            NlpjsApi nlpjsApi = retrofit.create(NlpjsApi.class);
            INSTANCE = new NlpjsService(nlpjsApi);
        }
        return INSTANCE;
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

}
