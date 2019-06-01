package edu.uoc.som.jarvis.core.server;

import com.google.gson.JsonElement;
import org.apache.http.Header;
import org.apache.http.NameValuePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * A handler that receives HTTP requests and process them.
 * <p>
 * This interface can be used to define REST endpoint from {@link edu.uoc.som.jarvis.core.platform.RuntimePlatform}s.
 * The endpoint can be registered using the following code:
 * <pre>
 * {@code
 * JarvisServer jarvisServer = [...]
 * String restEndpointURI = "/myEndpoint";
 * jarvisServer.registerRestEndpoint(restEndpointURI, (headers, params, content) -> {
 *     // Handle the request
 *     // return a JsonElement that will be sent back to the caller
 * }
 * }
 * </pre>
 *
 * @see JarvisServer
 */
@FunctionalInterface
public interface JsonRestHandler {

    /**
     * Handles the provided HTTP request.
     * <p>
     * This method is called by the {@link JarvisServer} when receiving a HTTP request on the URI associated to this
     * {@link JsonRestHandler}.
     *
     * @param headers the {@link Header}s of the HTTP request
     * @param params  the parameters of the HTTP request
     * @param content the {@link JsonElement} representing the content of the HTTP request
     * @return the {@link JsonElement} containing the endpoint response, or {@code null}
     */
    JsonElement handle(@Nonnull List<Header> headers, @Nonnull List<NameValuePair> params,
                       @Nullable JsonElement content);

}
