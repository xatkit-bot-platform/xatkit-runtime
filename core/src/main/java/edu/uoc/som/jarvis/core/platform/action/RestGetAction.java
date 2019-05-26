package edu.uoc.som.jarvis.core.platform.action;

import edu.uoc.som.jarvis.core.platform.RuntimePlatform;
import edu.uoc.som.jarvis.core.session.JarvisSession;

import java.util.Map;

/**
 * A generic REST GET action.
 * <p>
 * This class extends the generic {@link RestAction} and provides an utility constructor to create GET request.
 *
 * @param <T> the {@link RuntimePlatform} subclass containing this {@link RestGetAction}
 */
public abstract class RestGetAction<T extends RuntimePlatform> extends RestAction<T> {

    /**
     * Constructs a new {@link RestGetAction}.
     * <p>
     * This method doesn't perform the REST API request, this is done asynchronously in the {@link #compute()}
     * method.
     *
     * @param runtimePlatform the {@link RuntimePlatform} containing this action
     * @param session         the {@link JarvisSession} associated to this action
     * @param headers         the {@link Map} of user-defined headers to include in the request
     * @param restEndpoint    the REST API endpoint to request
     * @param params          the {@link Map} of user-defined parameters to include in the request
     * @throws NullPointerException     if the provided {@code runtimePlatform} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code restEndpoint} is {@code null} or {@code empty}
     */
    public RestGetAction(T runtimePlatform, JarvisSession session, Map<String, String> headers, String restEndpoint,
                         Map<String, Object> params) {
        super(runtimePlatform, session, MethodKind.GET, headers, restEndpoint, params, null);
    }
}
