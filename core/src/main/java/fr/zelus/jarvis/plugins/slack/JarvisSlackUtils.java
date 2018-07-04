package fr.zelus.jarvis.plugins.slack;

import fr.zelus.jarvis.core.JarvisCore;
import org.apache.commons.configuration2.Configuration;

/**
 * An utility interface that holds Slack-related helpers.
 * <p>
 * This class defines the jarvis configuration key to store the Slack bot API token, as well as a set of API response
 * types that are used internally to check connection and filter incoming events.
 */
public interface JarvisSlackUtils {

    /**
     * The {@link Configuration} key to store the Slack bot API token.
     *
     * @see fr.zelus.jarvis.plugins.slack.io.SlackInputProvider#SlackInputProvider(JarvisCore, Configuration)
     * @see fr.zelus.jarvis.plugins.slack.module.SlackModule#SlackModule(Configuration)
     */
    String SLACK_TOKEN_KEY = "jarvis.slack.token";

    /**
     * The Slack API answer type representing a {@code message}.
     */
    String MESSAGE_TYPE = "message";

    /**
     * The Slack API answer type representing a successful authentication.
     */
    String HELLO_TYPE = "hello";

    /**
     * The {@link fr.zelus.jarvis.core.session.JarvisContext} key used to store slack-related information.
     */
    String SLACK_CONTEXT_KEY = "slack";

    /**
     * The {@link fr.zelus.jarvis.core.session.JarvisContext} variable key used to store slack channel information.
     */
    String SLACK_CHANNEL_CONTEXT_KEY = "channel";

    /**
     * The {@link fr.zelus.jarvis.core.session.JarvisContext} variable key used to store slack username information.
     */
    String SLACK_USERNAME_CONTEXT_KEY = "username";
}
