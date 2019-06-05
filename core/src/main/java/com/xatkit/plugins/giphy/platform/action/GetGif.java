package com.xatkit.plugins.giphy.platform.action;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mashape.unirest.http.Headers;
import com.xatkit.core.platform.action.RestGetAction;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.plugins.giphy.platform.GiphyPlatform;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link RuntimeAction} that retrieves a GIF from a given search string.
 * <p>
 * This class relies on the {@link GiphyPlatform} to access the Giphy API using the token stored in the
 * {@link org.apache.commons.configuration2.Configuration}.
 */
public class GetGif extends RestGetAction<GiphyPlatform> {

    /**
     * The Giphy REST endpoint to retrieve GIFs from search strings.
     */
    private static String GIFS_SEARCH_URL = "https://api.giphy.com/v1/gifs/search";

    /**
     * Constructs the REST request parameter map containing the Giphy API key and the query search string.
     *
     * @param giphyPlatform the {@link GiphyPlatform} containing the Giphy API token to use in the parameter map
     * @param searchString  the search string used to retrieve GIFs
     * @return a {@link Map} containing the Giphy API token and the provided {@code searchString}
     */
    private static Map<String, Object> getParams(GiphyPlatform giphyPlatform, String searchString) {
        Map<String, Object> params = new HashMap<>();
        params.put("api_key", giphyPlatform.getGiphyToken());
        params.put("q", searchString);
        return params;
    }

    /**
     * Constructs a new {@link GetGif} with the provided {@code runtimePlatform}, {@code session}, and {@code
     * searchString}.
     * <p>
     * This constructor requires a valid Giphy API token in order to build the REST query used to retrieve GIF urls.
     *
     * @param runtimePlatform the {@link GiphyPlatform} containing this action
     * @param session         the {@link XatkitSession} associated to this action
     * @param searchString    the {@link String} used to search GIFs
     * @throws NullPointerException if the provided {@code runtimePlatform}, {@code session}, or {@code searchString}
     *                              is {@code null}
     * @see GiphyPlatform#getGiphyToken()
     */
    public GetGif(GiphyPlatform runtimePlatform, XatkitSession session, String searchString) {
        super(runtimePlatform, session, Collections.emptyMap(), GIFS_SEARCH_URL, getParams(runtimePlatform,
                searchString));
    }

    /**
     * Handles the Giphy API result and returns the retrieved GIF url.
     *
     * @param headers     the response's {@link Headers}
     * @param status      the response status code
     * @param body the response {@link JsonElement} containing the GIF url.
     * @return the retrieve GIF url
     */
    @Override
    protected Object handleResponse(Headers headers, int status, InputStream body) {
        JsonElement jsonElement = getJsonBody(body);
        JsonObject object = jsonElement.getAsJsonObject().getAsJsonArray("data").get(0).getAsJsonObject();
        JsonObject images = object.getAsJsonObject("images");
        JsonObject original = images.getAsJsonObject("original");
        String url = original.get("url").getAsString();
        return url;
    }
}

