package com.xatkit.plugins.slack.platform;

import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.methods.request.conversations.ConversationsListRequest;
import com.github.seratch.jslack.api.methods.request.users.UsersInfoRequest;
import com.github.seratch.jslack.api.methods.response.conversations.ConversationsListResponse;
import com.github.seratch.jslack.api.methods.response.users.UsersInfoResponse;
import com.github.seratch.jslack.api.model.Conversation;
import com.github.seratch.jslack.api.model.ConversationType;
import com.xatkit.core.XatkitCore;
import com.xatkit.core.XatkitException;
import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.plugins.chat.platform.ChatPlatform;
import com.xatkit.plugins.slack.SlackUtils;
import com.xatkit.plugins.slack.platform.action.PostMessage;
import com.xatkit.plugins.slack.platform.action.Reply;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.isNull;
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
 * This class is part of xatkit's core platform, and can be used in an execution model by importing the
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
     * A {@link Map} containing the names of all the channels in the workspace.
     * <p>
     * This map contains entries for conversation name, user name, user display name, and IDs, allowing fast lookups
     * to retrieve a channel identifier from a given name.
     * <p>
     * This {@link Map} is populated by {@link #loadChannels()}.
     *
     * @see #loadChannels()
     */
    private Map<String, String> channelNames;

    /**
     * A {@link List} containing the IDs of all the group channels in the workspace.
     * <p>
     * A group channel correspond to any conversation that can be joined by multiple users (typically everything
     * excepted user channels).
     * <p>
     * This {@link List} is populated by {@link #loadChannels()}.
     *
     * @see #loadChannels()
     */
    private List<String> groupChannels;

    /**
     * A {@link List} containing the IDs of all the user channels in the workspace.
     * <p>
     * A user channel is a direct message channel between an user and the bot.
     * <p>
     * This {@link List} is populated by {@link #loadChannels()}
     *
     * @see #loadChannels()
     */
    private List<String> userChannels;

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
                "provided token %s, please ensure that the Xatkit configuration contains a valid Slack bot API token " +
                "associated to the key %s", slackToken, SlackUtils.SLACK_TOKEN_KEY);
        slack = new Slack();
        this.channelNames = new HashMap<>();
        this.groupChannels = new ArrayList<>();
        this.userChannels = new ArrayList<>();
        this.loadChannels();
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
        return this.xatkitCore.getOrCreateXatkitSession(this.getChannelId(channel));
    }

    /**
     * Retrieves the ID of the provided {@code channelName}.
     * <p>
     * This method supports channel IDs, names, as well as user names, real names, and display names (in this case
     * the private {@code im} channel ID between the bot and the user is returned). The returned ID can be used to
     * send messages to the channel.
     *
     * @param channelName the name of the channel to retrieve the ID from
     * @return the channel ID if it exists
     * @throws XatkitException if the provided {@code channelName} does not correspond to any channel accessible by
     *                         the bot
     */
    public String getChannelId(String channelName) {
        String id = this.channelNames.get(channelName);
        if (isNull(id)) {
            /*
             * Check if the channel has been created since the previous lookup. This is not done by default because
             * it reloads all the channel and may take some time.
             */
            loadChannels();
            id = this.channelNames.get(channelName);
            if (isNull(id)) {
                /*
                 * Cannot find the channel after a fresh lookup.
                 */
                throw new XatkitException(MessageFormat.format("Cannot find the channel {0}, please ensure that the " +
                        "provided channel is either a valid channel ID, name, or a valid user name, real name, or " +
                        "display name", channelName));
            }
        }
        return id;
    }

    /**
     * Returns whether the provided {@code channelId} is a group channel (that can contain multiple users) or not.
     *
     * @param channelId the identifier of the Slack channel to check
     * @return {@code true} if the channel is a group channel, {@code false} otherwise
     */
    public boolean isGroupChannel(String channelId) {
        /*
         * First check if it's a user channel, if so no need to do additional calls on the Slack API, we know it's
         * not a group channel.
         */
        if (this.userChannels.contains(channelId)) {
            return false;
        } else {
            if (this.groupChannels.contains(channelId)) {
                return true;
            } else {
                /*
                 * Reload the channels in case the group channel has been created since the last check.
                 */
                loadChannels();
                return this.groupChannels.contains(channelId);
            }
        }
    }

    /**
     * Loads the channels associated to the workspace and store channel-related information.
     * <p>
     * The stored information can be retrieved with dedicated methods, and reduce the number of calls to the Slack API.
     *
     * @see #getChannelId(String)
     * @see #isGroupChannel(String)
     */
    private void loadChannels() {
        this.channelNames = new HashMap<>();
        this.groupChannels = new ArrayList<>();
        this.userChannels = new ArrayList<>();
        try {
            ConversationsListResponse response =
                    slack.methods().conversationsList(ConversationsListRequest.builder()
                            .token(slackToken)
                            .types(Arrays.asList(ConversationType.PUBLIC_CHANNEL, ConversationType.PUBLIC_CHANNEL,
                                    ConversationType.IM, ConversationType.MPIM))
                            .build());
            for (Conversation conversation : response.getChannels()) {
                String conversationId = conversation.getId();
                /*
                 * Store the conversation ID as an entry for itself, this is because we cannot differentiate IDs from
                 * regular strings when retrieving a channel ID.
                 */
                this.channelNames.put(conversationId, conversationId);
                if (nonNull(conversation.getName())) {
                    this.channelNames.put(conversation.getName(), conversation.getId());
                    this.groupChannels.add(conversation.getId());
                    Log.debug("Conversation name: {0}, ID: {1}", conversation.getName(), conversationId);
                } else {
                    String userId = conversation.getUser();
                    UsersInfoResponse userResponse = slack.methods().usersInfo(UsersInfoRequest.builder()
                            .token(slackToken)
                            .user(userId)
                            .build());
                    this.channelNames.put(userResponse.getUser().getName(), conversationId);
                    this.channelNames.put(userResponse.getUser().getRealName(), conversationId);
                    this.channelNames.put(userResponse.getUser().getProfile().getDisplayName(), conversationId);
                    this.userChannels.add(conversationId);
                    Log.debug("User name: {0}", userResponse.getUser().getName());
                    Log.debug("User real name: {0}", userResponse.getUser().getRealName());
                    Log.debug("User display name: {0}", userResponse.getUser().getProfile().getDisplayName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
