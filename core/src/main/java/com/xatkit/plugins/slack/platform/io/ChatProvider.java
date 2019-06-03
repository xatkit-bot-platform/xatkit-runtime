package com.xatkit.plugins.slack.platform.io;

import com.xatkit.core.platform.io.RuntimeIntentProvider;
import com.xatkit.plugins.slack.platform.SlackPlatform;
import org.apache.commons.configuration2.Configuration;

/**
 * A generic Slack user {@link RuntimeIntentProvider}.
 * <p>
 * This class wraps the {@link SlackIntentProvider} and allows to use it as a generic <i>ChatProvider</i> from the
 * <i>ChatPlatform</i>.
 *
 * @see SlackIntentProvider
 */
public class ChatProvider extends SlackIntentProvider {

    /**
     * Constructs a new {@link ChatProvider} from the provided {@code runtimePlatform} and {@code configuration}.
     *
     * @param runtimePlatform the {@link SlackPlatform} containing this {@link ChatProvider}
     * @param configuration   the {@link Configuration} used to retrieve the Slack bot API token
     * @throws NullPointerException     if the provided {@code runtimePlatform} or {@code configuration} is {@code null}
     * @throws IllegalArgumentException if the provided Slack bot API token is {@code null} or empty
     * @see SlackIntentProvider
     */
    public ChatProvider(SlackPlatform runtimePlatform, Configuration configuration) {
        super(runtimePlatform, configuration);
    }


}
