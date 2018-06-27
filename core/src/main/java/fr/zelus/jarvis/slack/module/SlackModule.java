package fr.zelus.jarvis.slack.module;

import com.github.seratch.jslack.Slack;
import fr.zelus.jarvis.core.JarvisModule;
import org.apache.commons.configuration2.Configuration;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.zelus.jarvis.slack.JarvisSlackUtils.SLACK_TOKEN_KEY;
import static java.util.Objects.nonNull;

/**
 * A {@link JarvisModule} class that connect and interacts with the Slack API.
 * <p>
 * This module manages a connection to the Slack API, and provides a single {@link fr.zelus.jarvis.core.JarvisAction}
 * to post messages on a given channel.
 * <p>
 * This class is part of jarvis' core modules, and can be used in an orchestration model by importing the
 * <i>core.SlackModule</i> package.
 */
public class SlackModule extends JarvisModule {

    /**
     * The {@link String} representing the Slack bot API token.
     * <p>
     * This token is retrieved from this class' {@link Configuration} constructor parameter, and is used to
     * authenticate the bot and post messages through the Slack API.
     * <p>
     * This field should not be {@code public} nor {@code static}, see the
     * <a href="https://github.com/gdaniel/jarvis/issues/16">corresponding issue on GitHub</a>
     *
     * @see #SlackModule(Configuration)
     */
    public static String slackToken;

    /**
     * The {@link Slack} API client used to post messages.
     * <p>
     * This field should not be {@code public} nor {@code static}, see the
     * <a href="https://github.com/gdaniel/jarvis/issues/16">corresponding issue on GitHub</a>
     */
    public static Slack slack;

    /**
     * Constructs a new {@link SlackModule} from the provided {@link Configuration}.
     * <p>
     * This constructor initializes the underlying {@link Slack} client with the Slack bot API token retrieved from
     * the {@link Configuration}.
     * <p>
     * <b>Note:</b> {@link SlackModule} requires a valid Slack bot API token to be initialized, and calling the
     * default constructor will throw an {@link IllegalArgumentException} when looking for the Slack bot API token.
     *
     * @param configuration the {@link Configuration} used to retrieve the Slack bot API token
     * @throws NullPointerException     if the provided {@link Configuration} is {@code null}
     * @throws IllegalArgumentException if the provided Slack bot API token is {@code null} or empty
     */
    public SlackModule(Configuration configuration) {
        super(configuration);
        checkNotNull(configuration, "Cannot construct a SlackModule from a null configuration");
        slackToken = configuration.getString(SLACK_TOKEN_KEY);
        checkArgument(nonNull(slackToken) && !slackToken.isEmpty(), "Cannot construct a SlackModule from the " +
                "provided token %s, please ensure that the jarvis configuration contains a valid Slack bot API token " +
                "associated to the key %s", slackToken, SLACK_TOKEN_KEY);
        slack = new Slack();
    }
}
