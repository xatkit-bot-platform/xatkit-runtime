package com.xatkit.plugins.slack.platform;

import com.github.seratch.jslack.Slack;
import com.xatkit.core.XatkitCore;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.plugins.slack.SlackUtils;
import com.xatkit.plugins.slack.platform.action.PostMessage;
import com.xatkit.plugins.slack.platform.action.Reply;
import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.plugins.chat.platform.ChatPlatform;
import org.apache.commons.configuration2.Configuration;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

/**
 * A {@link RuntimePlatform} class that connects and interacts with the Slack API.
 * <p>
 * This runtimePlatform manages a connection to the Slack API, and provides a set of
 * {@link RuntimeAction}s to interact with the Slack API:
 * <ul>
 * <li>{@link Reply}: reply to a user input</li>
 * <li>{@link PostMessage}: post a message to a given Slack channel</li>
 * </ul>
 * <p>
 * This class is part of jarvis' core platform, and can be used in an execution model by importing the
 * <i>SlackPlatform</i> package.
 */
public class SlackPlatform extends ChatPlatform {

    /**
     * The {@link String} representing the Slack bot API token.
     * <p>
     * This token is retrieved from this class' {@link Configuration} constructor parameter, and is used to
     * authenticate the bot and post messages through the Slack API.
     *
     * @see #getSlackToken()
     * @see #SlackPlatform(XatkitCore, Configuration)
     */
    private String slackToken;

    /**
     * The {@link Slack} API client used to post messages.
     */
    private Slack slack;

    /**
     * Constructs a new {@link SlackPlatform} from the provided {@link XatkitCore} and {@link Configuration}.
     * <p>
     * This constructor initializes the underlying {@link Slack} client with the Slack bot API token retrieved from
     * the {@link Configuration}.
     * <p>
     * <b>Note:</b> {@link SlackPlatform} requires a valid Slack bot API token to be initialized, and calling the
     * default constructor will throw an {@link IllegalArgumentException} when looking for the Slack bot API token.
     *
     * @param xatkitCore    the {@link XatkitCore} instance associated to this runtimePlatform
     * @param configuration the {@link Configuration} used to retrieve the Slack bot API token
     * @throws NullPointerException     if the provided {@code xatkitCore} or {@code configuration} is {@code null}
     * @throws IllegalArgumentException if the provided Slack bot API token is {@code null} or empty
     */
    public SlackPlatform(XatkitCore xatkitCore, Configuration configuration) {
        super(xatkitCore, configuration);
        slackToken = configuration.getString(SlackUtils.SLACK_TOKEN_KEY);
        checkArgument(nonNull(slackToken) && !slackToken.isEmpty(), "Cannot construct a SlackPlatform from the " +
                "provided token %s, please ensure that the jarvis configuration contains a valid Slack bot API token " +
                "associated to the key %s", slackToken, SlackUtils.SLACK_TOKEN_KEY);
        slack = new Slack();
    }

    /**
     * Returns the Slack bot API token used to initialize this class.
     *
     * @return the Slack bot API token used to initialize this class
     */
    public String getSlackToken() {
        return slackToken;
    }

    /**
     * Returns the Slack API client used to post messages.
     *
     * @return the Slack API client used to post messages
     */
    public Slack getSlack() {
        return slack;
    }

    /**
     * Returns the {@link XatkitSession} associated to the provided {@code channel}.
     *
     * @param channel the {@code channel} identifier to retrieve the {@link XatkitSession} from
     * @return the {@link XatkitSession} associated to the provided {@code channel}
     */
    public XatkitSession createSessionFromChannel(String channel) {
        return this.xatkitCore.getOrCreateXatkitSession(channel);
    }
}
