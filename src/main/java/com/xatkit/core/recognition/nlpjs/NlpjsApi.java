package com.xatkit.core.recognition.nlpjs;

import com.xatkit.core.recognition.nlpjs.model.Agent;
import com.xatkit.core.recognition.nlpjs.model.AgentInit;
import com.xatkit.core.recognition.nlpjs.model.RecognitionResult;
import com.xatkit.core.recognition.nlpjs.model.TrainingData;
import com.xatkit.core.recognition.nlpjs.model.UserMessage;
import lombok.NonNull;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Defines the NLP.js REST endpoints that can be queried with retrofit.
 * <p>
 * Each method in this interface corresponds to a REST endpoint of the NLP.js server. See the methods' signatures and
 * annotations for more information on the method, url, and parameters.
 */
public interface NlpjsApi {

    /**
     * Creates a {@link Call} wrapping {@code GET /agent/{agentId}}.
     * <p>
     * The returned {@link Call} retrieves the information of the provided {@code agentId}. See
     * {@link Call#execute()} to execute the request and handle the server response.
     *
     * @param agentId the identifier of the agent to retrieve the information of
     * @return the created {@link Call}
     */
    @GET("agent/{agentId}")
    Call<Agent> getAgentInfo(@Path("agentId") @NonNull String agentId);

    /**
     * Creates a {@link Call} wrapping {@code POST /agent}.
     * <p>
     * The returned {@link Call} creates a new agent with the provided {@link AgentInit} configuration. See
     * {@link Call#execute()} to execute the request and handle the server response.
     *
     * @param agentInit the configuration of the agent to create
     * @return the created {@link Call}
     */
    @POST("agent")
    Call<Void> createAgent(@Body @NonNull AgentInit agentInit);

    /**
     * Creates a {@link Call} wrapping {@code POST /agent/{agentId}/train}.
     * <p>
     * The returned {@link Call} trains the provided agent with the given {@code trainingData}. See
     * {@link Call#execute()} to execute the request and handle the server response.
     *
     * @param agentId      the identifier of the agent to train
     * @param trainingData the {@link TrainingData} used to train the agent
     * @return the created {@link Call}
     */
    @POST("agent/{agentId}/train")
    Call<Void> trainAgent(@Path("agentId") @NonNull String agentId, @NonNull @Body TrainingData trainingData);

    /**
     * Creates a {@link Call} wrapping {@code POST /agent/{agentId}/process}.
     * <p>
     * The returned {@link Call} sends the provided {@code userMessage} for recognition to the NLP.js server and
     * returns the server response. See {@link Call#execute()} to execute the request and handle the server response.
     *
     * @param agentId the identifier of the agent
     * @param message the message to recognize
     * @return the created {@link Call}
     */
    @POST("agent/{agentId}/process")
    Call<RecognitionResult> getIntent(@Path("agentId") @NonNull String agentId, @Body @NonNull UserMessage message);
}
