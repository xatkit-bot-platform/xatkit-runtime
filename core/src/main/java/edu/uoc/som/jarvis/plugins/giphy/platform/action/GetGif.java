package edu.uoc.som.jarvis.plugins.giphy.platform.action;

import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import edu.uoc.som.jarvis.core.platform.action.RuntimeAction;
import edu.uoc.som.jarvis.core.session.JarvisSession;
import edu.uoc.som.jarvis.plugins.giphy.platform.GiphyPlatform;
import org.json.JSONObject;

/**
 * A {@link RuntimeAction} that retrieves a GIF from a given search string.
 * <p>
 * This class relies on the {@link GiphyPlatform} to access the Giphy API using the token stored in the
 * {@link org.apache.commons.configuration2.Configuration}.
 */
public class GetGif extends RuntimeAction<GiphyPlatform> {

    /**
     * The {@link String} used to search GIFs from the Giphy API.
     */
    private String searchString;

    /**
     * Constructs a new {@link GetGif} with the provided {@code runtimePlatform}, {@code session}, and {@code
     * searchString}.
     * <p>
     *
     * @param runtimePlatform the {@link GiphyPlatform} containing this action
     * @param session         the {@link JarvisSession} associated to this action
     * @param searchString    the {@link String} used to search GIFs
     * @throws NullPointerException if the provided {@code runtimePlatform}, {@code session}, or {@code searchString}
     *                              is {@code null}
     */
    public GetGif(GiphyPlatform runtimePlatform, JarvisSession session, String searchString) {
        super(runtimePlatform, session);
        this.searchString = searchString;
    }

    /**
     * Build and send a REST query to the Giphy API to retrieve GIFs associated to the provided {@code searchString}.
     * <p>
     * This method relies on the containing {@link GiphyPlatform} to access the Giphy API.
     *
     * @return a {@link String} containing the URL of the retrieved GIF
     * @see GiphyPlatform#getGiphyToken()
     */
    @Override
    protected Object compute() {
        JsonNode jsonNode = null;
        try {
            jsonNode =
                    Unirest.get(("https://api.giphy.com/v1/gifs/search?api_key=" + this.runtimePlatform.getGiphyToken() + "&q" +
                            "=" + searchString).replace(" ", "%20"))
                            .asJson().getBody();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        JSONObject object = jsonNode.getObject().getJSONArray("data").getJSONObject(0);
        JSONObject images = object.getJSONObject("images");
        JSONObject original = images.getJSONObject("original");
        String url = original.getString("url");
        return url;
    }
}

