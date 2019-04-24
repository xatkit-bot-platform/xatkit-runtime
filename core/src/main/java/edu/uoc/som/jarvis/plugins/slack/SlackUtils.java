package edu.uoc.som.jarvis.plugins.slack;

import edu.uoc.som.jarvis.core.JarvisCore;
import edu.uoc.som.jarvis.core.session.RuntimeContexts;
import edu.uoc.som.jarvis.plugins.chat.ChatUtils;
import edu.uoc.som.jarvis.plugins.slack.platform.SlackPlatform;
import edu.uoc.som.jarvis.plugins.slack.platform.io.SlackIntentProvider;
import org.apache.commons.configuration2.Configuration;

/**
 * An utility interface that holds Slack-related helpers.
 * <p>
 * This class defines the jarvis configuration key to store the Slack bot API token, as well as a set of API response
 * types that are used internally to check connection and filter incoming events.
 */
public interface SlackUtils extends ChatUtils {

    /**
     * The {@link Configuration} key to store the Slack bot API token.
     *
     * @see SlackIntentProvider#SlackIntentProvider(SlackPlatform, Configuration)
     * @see SlackPlatform#SlackPlatform(JarvisCore, Configuration)
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
     * The {@link RuntimeContexts} key used to store slack-related information.
     */
    String SLACK_CONTEXT_KEY = "slack";

}
