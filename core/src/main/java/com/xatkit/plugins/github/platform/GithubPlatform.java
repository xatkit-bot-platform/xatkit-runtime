package com.xatkit.plugins.github.platform;

import com.jcabi.github.Github;
import com.jcabi.github.RtGithub;
import com.xatkit.core.XatkitCore;
import com.xatkit.plugins.github.platform.action.OpenIssue;
import com.xatkit.core.XatkitException;
import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.platform.io.RuntimeEventProvider;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;

import java.io.IOException;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;

/**
 * A {@link RuntimePlatform} class that connects and interacts with the Github API.
 * <p>
 * This runtimePlatform manages a output connection to the Github API, and provides a set of
 * {@link RuntimeAction}s to interact with the Github API:
 * <ul>
 *     <li>{@link OpenIssue}: open an issue on a given repository</li>
 * </ul>
 * <p>
 * This class is part of xatkit's core platforms, and can be used in an execution model by importing the
 * <i>GithubPlatform</i> package.
 */
public class GithubPlatform extends RuntimePlatform {

    /**
     * The {@link Configuration} key to store the Github username.
     * <p>
     * The {@link GithubPlatform} can handle {@link Configuration} with username/password as well as oauth token. Note
     * that if an username/password is provided it will be used instead of the oauth token to initialize the client.
     *
     * @see #GITHUB_PASSWORD_KEY
     * @see #GITHUB_OAUTH_TOKEN
     */
    public static String GITHUB_USERNAME_KEY = "xatkit.github.username";

    /**
     * The {@link Configuration} key to store the Github password associated to the defined username.
     * <p>
     * The {@link GithubPlatform} can handle {@link Configuration} with username/password as well as oauth token. Note
     * that if an username/password is provided it will be used instead of the oauth token to initialize the client.
     *
     * @see #GITHUB_USERNAME_KEY
     * @see #GITHUB_OAUTH_TOKEN
     */
    public static String GITHUB_PASSWORD_KEY = "xatkit.github.password";

    /**
     * The {@link Configuration} key to store the Github oauth token.
     * <p>
     * The {@link GithubPlatform} can handle {@link Configuration} with username/password as well as oauth token. Note
     * that if an username/password is provided it will be used instead of the oauth token to initialize the client.
     *
     * @see #GITHUB_USERNAME_KEY
     * @see #GITHUB_PASSWORD_KEY
     */
    public static String GITHUB_OAUTH_TOKEN = "xatkit.github.oauth.token";

    /**
     * The {@link Github} client used to access the Github API.
     * <p>
     * This client is initialized from the provided {@link Configuration}, and can be {@code null} if the
     * authentication failed or if the {@link Configuration} does not define any credentials information. Note that
     * {@link GithubPlatform} initialized with no credentials can still be used to construct Github-related
     * {@link RuntimeEventProvider}s.
     */
    private Github githubClient;

    /**
     * Constructs a new {@link GithubPlatform} from the provided {@link XatkitCore} and {@link Configuration}.
     * <p>
     * This constructor tries to initialize the {@link Github} client used to access the Github API by looking for
     * the username/login or oauth token in the provided {@link Configuration}. If no authentication credentials can
     * be found the {@link GithubPlatform} is still initialized, but will not be able to access and manipulate the
     * Github API.
     * <p>
     * {@link GithubPlatform} initialized with no credentials can still be used to construct Github-related
     * {@link RuntimeEventProvider}s, that will work as expected. Note that if the provided
     * {@link Configuration} defines credentials the constructed {@link GithubPlatform} will check that these
     * credentials are valid.
     *
     * @param xatkitCore    the {@link XatkitCore} instance associated to this runtimePlatform
     * @param configuration the {@link Configuration} used to customize Github events and actions
     * @throws NullPointerException     if the provided {@code xatkitCore} or {@code configuration} is {@code null}
     * @throws IllegalArgumentException if the provided {@link Configuration} contains a {@code username} but does not
     *                                  contain a valid {@code password}
     * @throws XatkitException          if the provided credentials are not valid or if a network error occurred when
     *                                  accessing the Github API.
     */
    public GithubPlatform(XatkitCore xatkitCore, Configuration configuration) {
        super(xatkitCore, configuration);
        String username = configuration.getString(GITHUB_USERNAME_KEY);
        if (nonNull(username)) {
            String password = configuration.getString(GITHUB_PASSWORD_KEY);
            checkArgument(nonNull(password) && !password.isEmpty(), "Cannot construct a %s from the " +
                            "provided username and password, please ensure that the Xatkit configuration contains a " +
                            "valid password for the username %s (configuration key: %s)", this.getClass()
                            .getSimpleName(),
                    username, GITHUB_PASSWORD_KEY);
            githubClient = new RtGithub(username, password);
            checkGithubClient(githubClient);
        } else {
            String oauthToken = configuration.getString(GITHUB_OAUTH_TOKEN);
            if (nonNull(oauthToken) && !oauthToken.isEmpty()) {
                githubClient = new RtGithub(oauthToken);
                checkGithubClient(githubClient);
            } else {
                Log.warn("No authentication method set in the configuration, {0} will not be able to call methods on " +
                        "the remote Github API. If you want to use the Github API you must provide a " +
                        "username/password or an oauth token in the Xatkit configuration", this.getClass().getSimpleName
                        ());
            }
        }
    }

    /**
     * Checks that provided {@code githubClient} is initialized and has valid credentials.
     * <p>
     * Credentials are checked by trying to retrieve the <i>self user login</i> from the {@link Github} client. If
     * the credentials are valid the {@link Github} client defines a <i>self</i> instance, otherwise it throws an
     * {@link AssertionError} when receiving the API result.
     *
     * @param githubClient the {@link Github} client to check
     * @throws XatkitException if the provided {@link Github} client credentials are invalid or if an
     *                         {@link IOException} occurred when accessing the Github API.
     */
    private void checkGithubClient(Github githubClient) {
        checkNotNull(githubClient, "Cannot check the provided %s %s", Github.class.getSimpleName(), githubClient);
        try {
            String selfLogin = githubClient.users().self().login();
            Log.info("Logged in Github under the user {0}", selfLogin);
        } catch (IOException e) {
            throw new XatkitException(e);
        } catch (AssertionError e) {
            throw new XatkitException("Cannot access the Github API, please check your credentials", e);
        }
    }

    /**
     * Returns the {@link Github} client used to access the Github API.
     *
     * @return the {@link Github} client used to access the Github API
     * @throws XatkitException if the {@link GithubPlatform} does not define a valid {@link Github} client (i.e. if the
     *                         provided {@link Configuration} does not contain any credentials information or if the
     *                         authentication failed).
     */
    public Github getGithubClient() {
        if (nonNull(githubClient)) {
            return githubClient;
        } else {
            throw new XatkitException("Cannot access the Github client, make sure it has been initialized correctly");
        }
    }
}
