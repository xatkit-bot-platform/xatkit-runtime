package com.xatkit.plugins.giphy.platform;

import com.xatkit.core.XatkitCore;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.plugins.giphy.platform.action.GetGif;
import com.xatkit.core.platform.RuntimePlatform;
import org.apache.commons.configuration2.Configuration;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;

/**
 * A {@link RuntimePlatform} class that connects and interacts with the Giphy API.
 * <p>
 * This platform provides the following {@link RuntimeAction}s to interact
 * with the Giphy API:
 * <ul>
 * <li>{@link GetGif}: retrieve a GIF from a given search
 * query</li>
 * </ul>
 * <p>
 * This class is part of xatkit's core platform, and can be used in an execution model by importing the
 * <i>GiphyPlatform</i> package.
 */
public class GiphyPlatform extends RuntimePlatform {

    /**
     * The {@link Configuration} key to store the Giphy API token.
     */
    public static String GIPHY_TOKEN_KEY = "xatkit.giphy.token";

    /**
     * The Giphy API token used to initialize this class.
     */
    private String giphyToken;

    /**
     * Constructs a new {@link GiphyPlatform} from the provided {@link XatkitCore} and {@link Configuration}.
     * <p>
     * This constructor retrieves the Giphy API token from the provided configuration.
     *
     * @param xatkitCore    the {@link XatkitCore} instance associated to this runtimePlatform
     * @param configuration the {@link Configuration} used to retrieve the Giphy API token
     * @throws NullPointerException     if the provided {@code xatkitCore} or {@code configuration} is {@code null}
     * @throws IllegalArgumentException if the provided {@link Configuration} does not contain a Giphy API token
     */
    public GiphyPlatform(XatkitCore xatkitCore, Configuration configuration) {
        super(xatkitCore, configuration);
        checkArgument(configuration.containsKey(GIPHY_TOKEN_KEY), "Cannot construct a %s, please ensure that the " +
                        "configuration contains a valid Giphy API token (configuration key: %s)",
                this.getClass().getSimpleName(), GIPHY_TOKEN_KEY);
        giphyToken = configuration.getString(GIPHY_TOKEN_KEY);
    }

    /**
     * Returns the Giphy API token.
     *
     * @return the Giphy API token
     */
    public String getGiphyToken() {
        return this.giphyToken;
    }
}
