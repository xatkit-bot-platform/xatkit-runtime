package com.xatkit.core.recognition.nlpjs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

/**
 * Provides utility methods to interact with a NLP.js server.
 * <p>
 * This class is used by the {@link NlpjsIntentRecognitionProvider} to setup a NLP.js agent for a bot and interact
 * with it to register/retrieve intents and entities.
 * <p>
 * This class relies on {@link NlpjsApi}, a retrofit-based interface used to define the REST operations available in
 * the NLP.js server and compute them.
 *
 * @see NlpjsApi
 */
public class NlpjsClient {

    /**
     * The base path of the NLP.js server API.
     * <p>
     * The {@link NlpjsClient} appends this path to the {@code nlpjsServerUrl} provided in the constructor and uses
     * the produced URL to send REST requests.
     *
     * @see NlpjsClient
     */
    private static final String NLPJS_API_BASE_PATH = "/api";

    /**
     * The retrofit-based interface used to define and compute REST operations.
     */
    private NlpjsApi nlpjsApi;

    /**
     * The {@link Gson} instance used to (de)serialize Json elements.
     * <p>
     * This instance takes care of serializing {@link com.xatkit.core.recognition.nlpjs.model} classes into Json
     * elements before they are sent to the NLP.js server. Customization of the serialization process should be done
     * in this class.
     */
    private Gson gson;

    /**
     * Creates a {@link NlpjsClient} managing the NLP.js server at {@code nlpjsServerUrl}.
     * <p>
     * The provided {@code nlpjsServerUrl} must be the base URL of the NLP.js server, including its port (e.g. {@code
     * http://localhost:8080}). The client appends the {@link #NLPJS_API_BASE_PATH} prefix to the provided {@code
     * nlpjsServerUrl} and uses the produced URL to send REST requests.
     *
     * @param nlpjsServerUrl the URL of the NLP.js server to connect to
     * @throws NullPointerException     if the provided {@code nlpjsServerUrl} is {@code null}
     * @throws IllegalArgumentException if the provided {@code nlpjsServerUrl} is empty
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    public NlpjsClient(@NonNull String nlpjsServerUrl) {
        checkArgument(!nlpjsServerUrl.isEmpty(), "Cannot create %s, the provided URL for the NLP.js server is empty",
                this.getClass().getSimpleName());
        gson = new GsonBuilder().registerTypeAdapter(ExtractedEntity.class, new ExtractedEntityDeserializer())
                .create();
        String nlpjsApiUrl = nlpjsServerUrl + NLPJS_API_BASE_PATH + "/";
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor(message -> {
                    /*
                     * We need to process the Json characters that conflict with the message formatting method we use
                     * in the logger (see MessageFormat for more information).
                     */
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

    /**
     * Returns the information of the provided {@code agentId}.
     *
     * @param agentId the identifier of the agent to retrieve the information of
     * @return the information of the provided {@code agentId}
     * @throws NlpjsClientException if the server is unreachable or if the API returned an error status code
     * @throws NullPointerException if the provided {@code agentId} is {@code null}
     */
    public Agent getAgentInfo(@NonNull String agentId) throws NlpjsClientException {
        Response<Agent> agentResponse = this.executeCall(nlpjsApi.getAgentInfo(agentId));
        return agentResponse.body();
    }

    /**
     * Creates a new agent with the provided {@link AgentInit} configuration.
     *
     * @param agentInit the configuration of the agent to create
     * @return {@code true} if the agent is properly created
     * @throws NlpjsClientException if the server is unreachable or if the API returned an error status code
     * @throws NullPointerException if the provided {@code agentInit} is {@code null}
     */
    public boolean createAgent(@NonNull AgentInit agentInit) throws NlpjsClientException {
        Response<Void> response = this.executeCall(nlpjsApi.createAgent(agentInit));
        return true;
    }

    /**
     * Trains the agent with the provided {@link TrainingData}.
     * <p>
     * The provided {@code agentId} must match the identifier of an existing agent, see
     * {@link #createAgent(AgentInit)} to create an agent.
     *
     * @param agentId      the identifier of the agent to train
     * @param trainingData the {@link TrainingData} used to train the agent
     * @return {@code true} if the agent is properly trained
     * @throws NlpjsClientException if the server is unreachable or if the API returned an error status code
     * @throws NullPointerException if the provided {@code agentId} or {@code trainingData} is {@code null}
     */
    public boolean trainAgent(@NonNull String agentId, @NonNull TrainingData trainingData) throws NlpjsClientException {
        this.executeCall(nlpjsApi.trainAgent(agentId, trainingData));
        return true;
    }

    /**
     * Returns the {@link RecognitionResult} computed by NLP.js for the provided {@code message}.
     * <p>
     * The provided {@code agentId} must match the identifier of an existing agent, see
     * {@link #createAgent(AgentInit)} to create an agent.
     *
     * @param agentId the identifier of the agent
     * @param message the message to recognize
     * @return the {@link RecognitionResult} computed by NLP.js for the provided {@code message}
     * @throws NlpjsClientException if the server is unreachable or if the API returned an error status code
     * @throws NullPointerException if the provided {@code agentId} or {@code message} is {@code null}
     */
    public RecognitionResult getIntent(@NonNull String agentId, @NonNull UserMessage message)
            throws NlpjsClientException {
        Response<RecognitionResult> response = this.executeCall(nlpjsApi.getIntent(agentId, message));
        return response.body();
    }

    /**
     * Returns {@code true} if the client is shutdown, {@code false} otherwise.
     * <p>
     * This method checks that the NLP.js server is reachable, and returns {@code true} if an error occurred when
     * pinging the server.
     *
     * @return {@code true} if the client is shutdown, {@code false} otherwise
     */
    public boolean isShutdown() {
        try {
            /*
             * We consider the client shutdown if the NLP.js server doesn't answer a request or if it returns an
             * error when we access the agent information (if the agent isn't accessible the client cannot do
             * anything and it's better to consider it shutdown).
             */
            this.executeCall(nlpjsApi.getAgentInfo("default"));
            return false;
        } catch (NlpjsClientException e) {
            Log.error("The NLP.js API is not responding. See the attached error: {0}", e);
            return true;
        }
    }

    /**
     * Executes the provided {@code requestCall} and wraps errors in {@link NlpjsClientException}.
     * <p>
     * This method returns the {@link Response} associated to a successful request call, and throws a
     * {@link NlpjsClientException} if an error occurred when computing the request.
     * <p>
     * Note that an {@link NlpjsClientException} is thrown if the server is unreachable (wrapping an
     * {@link IOException}), but also if the NLP.js REST API returned an error status code ({@code != 200}).
     *
     * @param requestCall the request call to perform
     * @param <T>         the type of the expected element in the response
     * @return the {@link Response} if the request call is successful
     * @throws NlpjsClientException if the server is unreachable or if the API returned an error status code
     * @throws NullPointerException if the provided {@code requestCall} is {@code null}
     */
    private <T> Response<T> executeCall(@NonNull Call<T> requestCall) {
        try {
            Response<T> response = requestCall.execute();
            if (!response.isSuccessful()) {
                String message = "";
                if (nonNull(response.errorBody())) {
                    ErrorBody errorBody = gson.fromJson(response.errorBody().string(), ErrorBody.class);
                    message = errorBody.getMessage();
                }
                throw new NlpjsClientException(requestCall.request().method(), requestCall.request().url().toString(),
                        response.code(), message);
            }
            return response;
        } catch (IOException e) {
            throw new NlpjsClientException(e);
        }
    }
}
