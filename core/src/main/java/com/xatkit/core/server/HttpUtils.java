package com.xatkit.core.server;

import org.apache.http.NameValuePair;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Provides utility methods to manipulate Http-related objects.
 */
public class HttpUtils {

    /**
     * Returns the value associated to the provided {@code parameterName} from the given {@code parameters} list.
     *
     * @param parameterName the name of the parameter to retrieve the value of
     * @param parameters    the list of parameters to search in
     * @return the value associated to the provided {@code parameterName} from the given {@code parameters} list
     */
    public static @Nullable
    String getParameterValue(String parameterName, List<NameValuePair> parameters) {
        return parameters.stream().filter(p -> p.getName().equals(parameterName))
                .map(NameValuePair::getValue).findFirst().orElse(null);
    }
}
