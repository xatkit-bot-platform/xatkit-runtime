package com.xatkit.core.server;

import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URISyntaxException;
import java.util.List;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * Provides utility methods to manipulate Http-related objects.
 */
public class HttpUtils {

    /**
     * Returns a {@link URIBuilder} from the provided {@code httpRequest}.
     *
     * @param httpRequest the {@link HttpRequest} to get an {@link URIBuilder} from
     * @return the created {@link URIBuilder}
     * @throws URISyntaxException if the provided {@code httpRequest}'s URI is invalid
     */
    public static URIBuilder getURIBuilderFrom(@Nonnull HttpRequest httpRequest) throws URISyntaxException {
        checkNotNull(httpRequest, "Cannot create an %s from the provided %s %s", URIBuilder.class.getSimpleName(),
                HttpRequest.class.getSimpleName(), httpRequest);
        return new URIBuilder(httpRequest.getRequestLine().getUri());
    }

    /**
     * Returns the path associated to the provided {@code httpRequest}.
     * <p>
     * The returned path corresponds to the target of the provided {@code httpRequest} without the requests
     * parameters. For example, calling this method on a request with the URI {@code /target?param=value} will return
     * {@code /target}.
     *
     * @param httpRequest the {@link HttpRequest} to get the path from
     * @return the path associated to the provided {@code httpRequest}
     * @throws URISyntaxException if the provided {@code httpRequest}'s URI is invalid
     */
    public static String getPath(@Nonnull HttpRequest httpRequest) throws URISyntaxException {
        checkNotNull(httpRequest, "Cannot retrieve the path from the provided %s %s", HttpRequest.class.getSimpleName()
                , httpRequest);
        URIBuilder uriBuilder = getURIBuilderFrom(httpRequest);
        return uriBuilder.getPath();
    }

    /**
     * Returns a {@link List} containing the parameters associated to the provided {@code httpRequest}.
     * <p>
     * Calling this method on a {@code httpRequest} instance that does not define any parameter in its request URI
     * returns an empty {@link List}.
     *
     * @param httpRequest the {@link HttpRequest} to get the parameters from
     * @return a {@link List} containing the parameters associated to the provided {@code httpRequest}
     * @throws URISyntaxException if the provided {@code httpRequest}'s URI is invalid
     */
    public static List<NameValuePair> getParameters(@Nonnull HttpRequest httpRequest) throws URISyntaxException {
        checkNotNull(httpRequest, "Cannot retrieve the parameters from the provided %s %s",
                HttpRequest.class.getSimpleName(), httpRequest);
        URIBuilder uriBuilder = getURIBuilderFrom(httpRequest);
        return uriBuilder.getQueryParams();
    }

    /**
     * Returns the value associated to the provided {@code parameterName} from the given {@code parameters} list.
     *
     * @param parameterName the name of the parameter to retrieve the value of
     * @param parameters    the list of parameters to search in
     * @return the value associated to the provided {@code parameterName} from the given {@code parameters} list
     */
    public static @Nullable
    String getParameterValue(@Nonnull String parameterName, @Nonnull List<NameValuePair> parameters) {
        checkNotNull(parameterName, "Cannot retrieve the value of parameter %s", parameterName);
        checkNotNull(parameters, "Cannot retrieve parameters from the provided list: %s", parameters);
        return parameters.stream().filter(p -> p.getName().equals(parameterName))
                .map(NameValuePair::getValue).findFirst().orElse(null);
    }
}
