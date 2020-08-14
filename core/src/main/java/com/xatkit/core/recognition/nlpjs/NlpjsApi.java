package com.xatkit.core.recognition.nlpjs;

import com.xatkit.core.recognition.nlpjs.model.*;
import lombok.NonNull;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface NlpjsApi {

    @GET("agent/{agentId}")
    Call<Agent> getAgentInfo(@Path("agentId") @NonNull String agentId);

    @POST("agent")
    Call<Void> createAgent(@Body @NonNull AgentInit agentInit);

    @POST("agent/{agentId}/train")
    Call<Void> trainAgent(@Path("agentId") @NonNull String agentId, @NonNull @Body TrainingData trainingData);

    @POST("agent/{agentId}/process")
    Call<RecognitionResult> getIntent(@Path("agentId") @NonNull String agentId, @Body @NonNull UserMessage userMessage);


}
