package com.xatkit.core.platform.action;

import com.google.gson.JsonElement;
import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.session.JarvisSession;

import java.util.Map;

/**
 * A generic REST POST action.
 * <p>
 * This class extends the generic {@link RestAction} and provides an utility constructor to create POST request.
 *
 * @param <T> the {@link RuntimePlatform} subclass containing this {@link RestPostAction}
 */
public abstract class RestPostAction<T extends RuntimePlatform> extends RestAction<T> {

    /**
     * Constructs a new {@link RestPostAction}.
     * <p>
     * This method doesn't perform the REST API request, this is done asynchronously in the {@link #compute()}
     * method.
     *
     * @param runtimePlatform the {@link RuntimePlatform} containing this action
     * @param session         the {@link JarvisSession} associated to this action
     * @param headers         the {@link Map} of user-defined headers to include in the request
     * @param restEndpoint    the REST API endpoint to request
     * @param params          the {@link Map} of user-defined parameters to include in the request
     * @param jsonContent     the {@link JsonElement} to include in the request's body
     * @throws NullPointerException     if the provided {@code runtimePlatform} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code restEndpoint} is {@code null} or {@code empty}
     */
    public RestPostAction(T runtimePlatform, JarvisSession session, Map<String, String> headers, String restEndpoint,
                          Map<String, Object> params, JsonElement jsonContent) {
        super(runtimePlatform, session, MethodKind.POST, headers, restEndpoint, params, jsonContent);
    }
}
