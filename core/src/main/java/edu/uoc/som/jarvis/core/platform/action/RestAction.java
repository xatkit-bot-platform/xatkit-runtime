package edu.uoc.som.jarvis.core.platform.action;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mashape.unirest.http.Headers;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.BaseRequest;
import com.mashape.unirest.request.HttpRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import edu.uoc.som.jarvis.core.platform.RuntimePlatform;
import edu.uoc.som.jarvis.core.session.JarvisSession;
import fr.inria.atlanmod.commons.log.Log;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Map;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

/**
 * A generic REST action.
 * <p>
 * This action provides the execution logic to compute GET and POST REST requests over a provided endpoint. Request
 * parameters, headers, and body can be specified through the constructor parameters.
 * <p>
 * <b>Note</b>: this class assumes that the requested REST API expects nothing or JSON in the query body. This class
 * also expects that the API response contains an empty body or a {@link JsonElement}.
 *
 * @param <T> the {@link RuntimePlatform} subclass containing this {@link RestAction}
 */
public abstract class RestAction<T extends RuntimePlatform> extends RuntimeAction<T> {

    /**
     * The {@link Gson} instance used to print the provided {@link JsonElement} content in a {@link String}.
     * <p>
     * This {@link String} is embedded in the REST POST request body.
     */
    private Gson gson;

    /**
     * The {@link MethodKind} of the request to perform.
     */
    protected MethodKind method;

    /**
     * A {@link Map} containing user-defined headers to include in the request.
     * <p>
     * This {@link Map} is filled with the following template: {@code header_name -> header_value}.
     */
    protected Map<String, String> headers;

    /**
     * The REST API endpoint to request.
     * <p>
     * The provided REST endpoint url must be <b>absolute</b>. Relative endpoint urls are not supported for now.
     */
    protected String restEndpoint;

    /**
     * A {@link Map} containing user-defined parameters to include in the request.
     * <p>
     * This {@link Map} is filled with the following template {@code param_name -> param_value}. Note that the
     * {@link Map}'s values are {@link Object}s in order to support multipart body.
     */
    protected Map<String, Object> params;

    /**
     * The {@link JsonElement} to include in the request's body.
     * <p>
     * This element is only used for POST requests.
     */
    protected JsonElement jsonContent;

    /**
     * Constructs a new {@link RestAction}.
     * <p>
     * This method doesn't perform the REST API request, this is done asynchronously in the {@link #compute()} method.
     *
     * @param runtimePlatform the {@link RuntimePlatform} containing this action
     * @param session         the {@link JarvisSession} associated to this action
     * @param method          the REST method to use
     * @param headers         the {@link Map} of user-defined headers to include in the request
     * @param restEndpoint    the REST API endpoint to request
     * @param params          the {@link Map} of user-defined parameters to include in the request
     * @param jsonContent     the {@link JsonElement} to include in the request's body
     * @throws NullPointerException     if the provided {@code runtimePlatform} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code method} is {@code null}, or if the provided {@code
     *                                  restEndpoint} is {@code null} or {@code empty}
     */
    public RestAction(T runtimePlatform, JarvisSession session, MethodKind method,
                      @Nullable Map<String, String> headers, String restEndpoint,
                      @Nullable Map<String, Object> params, @Nullable JsonElement jsonContent) {
        super(runtimePlatform, session);
        checkArgument(nonNull(method), "Cannot construct a %s action with the provided method %s, expected \"GET\" " +
                "or \"POST\"", this.getClass().getSimpleName(), method);
        checkArgument(nonNull(restEndpoint) && !restEndpoint.isEmpty(), "Cannot construct a %s action with the " +
                "provided REST endpoint %s", this.getClass().getSimpleName(), restEndpoint);
        this.method = method;
        this.headers = headers;
        this.restEndpoint = restEndpoint;
        this.params = params;
        this.jsonContent = jsonContent;
        this.gson = new Gson();
    }

    /**
     * Computes the REST request and returned the handled result.
     * <p>
     * This method performs the request over the defined REST API, and delegates the result computation to the
     * {@link #handleResponse(Headers, int, JsonElement)} method.
     *
     * @return the result of handling the REST API response
     * @throws Exception if an error occurred when executing the REST request
     * @see #handleResponse(Headers, int, JsonElement)
     */
    @Override
    protected final Object compute() throws Exception {
        BaseRequest request;
        switch (method) {
            case GET:
                request = Unirest.get(restEndpoint)
                        .headers(headers)
                        .queryString(params);
                Log.info("Sent GET query on {0}", ((HttpRequest) request).getUrl());
                break;
            case POST:
                request = Unirest.post(restEndpoint)
                        .headers(headers);
                if (nonNull(params) && !params.isEmpty()) {
                    /*
                     * Params correspond to multipart body in POST requests.
                     */
                    request = ((HttpRequestWithBody) request).fields(params);
                } else if (nonNull(jsonContent)) {
                    request = ((HttpRequestWithBody) request).body(gson.toJson(jsonContent));
                }
                break;
            default:
                throw new IllegalStateException(MessageFormat.format("{0} was initialized with the invalid method {1}",
                        this.getClass().getSimpleName(), this.method.name()));
        }
        HttpResponse<InputStream> response = request.asBinary();
        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElement = jsonParser.parse(new InputStreamReader(response.getBody()));
        return this.handleResponse(response.getHeaders(), response.getStatus(), jsonElement);
    }

    /**
     * Handles the REST API response and computes the action's results
     * <p>
     * This method is overridden in concrete subclasses to implement action-specific behaviors.
     *
     * @param headers     the {@link Headers} returned by the REST API
     * @param status      the status code returned by the REST API
     * @param jsonElement the optional {@link JsonElement} returned by the REST API
     * @return the action's result
     */
    protected abstract Object handleResponse(Headers headers, int status, @Nullable JsonElement jsonElement);

    /**
     * The kind of REST methods supported by this class.
     */
    public enum MethodKind {
        GET,
        POST
    }
}
