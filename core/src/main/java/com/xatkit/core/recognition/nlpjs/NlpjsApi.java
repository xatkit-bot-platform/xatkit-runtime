package com.xatkit.core.recognition.nlpjs;

import com.xatkit.core.recognition.nlpjs.model.*;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface NlpjsApi {

    @GET("agent/{agentId}")
    Call<Agent> getAgentInfo(@Path("agentId") String agentId);

    @POST("agent")
    Call<Void> createAgent(@Body AgentInit agentInit);

    @POST("agent/{agentId}/train")
    Call<Void> trainAgent(@Path("agentId") String agentId, @Body TrainingData trainingData);

    @POST("agent/{agentId}/process")
    Call<RecognizedIntent> getIntent(@Path("agentId") String agentId, @Body UserMessage userMessage);


}
