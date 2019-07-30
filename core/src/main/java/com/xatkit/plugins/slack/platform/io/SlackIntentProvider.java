package com.xatkit.plugins.slack.platform.io;

import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.request.auth.AuthTestRequest;
import com.github.seratch.jslack.api.methods.request.users.UsersInfoRequest;
import com.github.seratch.jslack.api.methods.response.auth.AuthTestResponse;
import com.github.seratch.jslack.api.methods.response.users.UsersInfoResponse;
import com.github.seratch.jslack.api.model.User;
import com.github.seratch.jslack.api.rtm.RTMClient;
import com.github.seratch.jslack.api.rtm.RTMCloseHandler;
import com.github.seratch.jslack.api.rtm.RTMMessageHandler;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xatkit.core.XatkitException;
import com.xatkit.core.platform.io.RuntimeEventProvider;
import com.xatkit.core.platform.io.RuntimeIntentProvider;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.plugins.chat.ChatUtils;
import com.xatkit.plugins.chat.platform.io.ChatIntentProvider;
import com.xatkit.plugins.slack.SlackUtils;
import com.xatkit.plugins.slack.platform.SlackPlatform;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;

import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import java.io.IOException;
import java.text.MessageFormat;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A Slack user {@link RuntimeIntentProvider}.
 * <p>
 * This class relies on the Slack RTM API to receive direct messages and react to them. Note that this input provider
 * only captures direct messages sent to the Slack bot associated to this class.
 * <p>
 * Instances of this class must be configured with a {@link Configuration} instance holding the Slack bot API token
 * in the property {@link SlackUtils#SLACK_TOKEN_KEY}. This token is used to authenticate the bot and receive
 * messages through the RTM API.
 *
 * @see SlackUtils
 * @see RuntimeEventProvider
 */
public class SlackIntentProvider extends ChatIntentProvider<SlackPlatform> {

    /**
     * The default username returned by {@link #getUsernameFromUserId(String)}.
     *
     * @see #getUsernameFromUserId(String)
     */
    private static String DEFAULT_USERNAME = "unknown user";

    /**
     * The delay (in ms) to wait before attempting to reconnect the RTM client.
     * <p>
     * When the RTM client is disconnected abnormally the {@link SlackIntentProvider} attempts to reconnect it by
     * waiting {@code RECONNECT_WAIT_TIME * <number_of_attempts>} ms. The delay is reset after each successful
     * reconnection.
     *
     * @see XatkitRTMCloseHandler
     */
    private static int RECONNECT_WAIT_TIME = 2000;

    /**
     * The {@link String} representing the Slack bot API token.
     * <p>
     * This token is used to authenticate the bot and receive messages through the RTM API.
     */
    private String slackToken;

    /**
     * The {@link String} representing the Slack bot identifier.
     * <p>
     * This identifier is used to check input message and filter the ones that are sent by this bot, in order to avoid
     * infinite message loops.
     */
    private String botId;

    /**
     * The {@link RTMClient} managing the RTM connection to the Slack API.
     */
    private RTMClient rtmClient;

    /**
     * The Slack API client used to retrieve Slack-related information.
     */
    private Slack slack;

    /**
     * The {@link JsonParser} used to manipulate Slack API answers.
     */
    private JsonParser jsonParser;

    /**
     * Specifies whether {@code DEFAULT_FALLBACK_INTENT}s should be ignored in group channel (default to {@code false}).
     */
    private boolean ignoreFallbackOnGroupChannels;

    /**
     * Constructs a new {@link SlackIntentProvider} from the provided {@code runtimePlatform} and
     * {@code configuration}.
     * <p>
     * This constructor initializes the underlying RTM connection and creates a message listener that forwards to
     * the {@code xatkitCore} instance not empty direct messages sent by users (not bots) to the Slack bot associated
     * to this class.
     * <p>
     * <b>Note:</b> {@link SlackIntentProvider} requires a valid Slack bot API token to be initialized, and calling
     * the default constructor will throw an {@link IllegalArgumentException} when looking for the Slack bot API token.
     *
     * @param runtimePlatform the {@link SlackPlatform} containing this {@link SlackIntentProvider}
     * @param configuration   the {@link Configuration} used to retrieve the Slack bot API token
     * @throws NullPointerException     if the provided {@code runtimePlatform} or {@code configuration} is {@code
     *                                  null}
     * @throws IllegalArgumentException if the provided Slack bot API token is {@code null} or empty
     */
    public SlackIntentProvider(SlackPlatform runtimePlatform, Configuration configuration) {
        super(runtimePlatform, configuration);
        checkNotNull(configuration, "Cannot construct a SlackIntentProvider from a null configuration");
        this.slackToken = configuration.getString(SlackUtils.SLACK_TOKEN_KEY);
        checkArgument(nonNull(slackToken) && !slackToken.isEmpty(), "Cannot construct a SlackIntentProvider from the " +
                "provided token %s, please ensure that the Xatkit configuration contains a valid Slack bot API token " +
                "associated to the key %s", slackToken, SlackUtils.SLACK_TOKEN_KEY);
        this.ignoreFallbackOnGroupChannels = configuration.getBoolean(SlackUtils.IGNORE_FALLBACK_ON_GROUP_CHANNELS_KEY,
                SlackUtils.DEFAULT_IGNORE_FALLBACK_ON_GROUP_CHANNELS_KEY);
        this.slack = new Slack();
        this.botId = getSelfId();
        try {
            this.rtmClient = slack.rtm(slackToken);
        } catch (IOException e) {
            String errorMessage = MessageFormat.format("Cannot connect SlackIntentProvider, please ensure that the " +
                            "bot API token is valid and stored in Xatkit configuration with the key {0}",
                    SlackUtils.SLACK_TOKEN_KEY);
            Log.error(errorMessage);
            throw new XatkitException(errorMessage, e);
        }
        this.jsonParser = new JsonParser();
        Log.info("Starting to listen Slack direct messages");
        rtmClient.addMessageHandler(new XatkitRTMMessageHandler());
        rtmClient.addCloseHandler(new XatkitRTMCloseHandler());
        try {
            rtmClient.connect();
        } catch (DeploymentException | IOException e) {
            String errorMessage = "Cannot start the Slack RTM websocket, please check your internet connection";
            Log.error(errorMessage);
            throw new XatkitException(errorMessage, e);
        }
    }

    /**
     * Returns the Slack bot identifier.
     * <p>
     * This identifier is used to check input message and filter the ones that are sent by this bot, in order to avoid
     * infinite message loops.
     *
     * @return the Slack bot identifier
     */
    private String getSelfId() {
        AuthTestRequest request = AuthTestRequest.builder().token(slackToken).build();
        try {
            AuthTestResponse response = slack.methods().authTest(request);
            return response.getUserId();
        } catch (IOException | SlackApiException e) {
            throw new XatkitException("Cannot retrieve the bot identifier", e);
        }
    }

    /**
     * Returns the Slack username associated to the provided {@code userId}.
     * <p>
     * This method returns the <i>display name</i> associated to the provided {@code userId} if it is set in the user
     * profile. If the user profile does not contain a non-empty display name this method returns the <i>real
     * name</i> associated to the provided {@code userId}.
     * <p>
     * This method returns {@link #DEFAULT_USERNAME} if the Slack API is not reachable or if the provided {@code
     * userId} does not match any known user.
     *
     * @param userId the user identifier to retrieve the username from
     * @return the Slack username associated to the provided {@code userId}
     */
    private String getUsernameFromUserId(String userId) {
        Log.info("Retrieving username for user ID {0}", userId);
        String username = DEFAULT_USERNAME;
        UsersInfoRequest usersInfoRequest = UsersInfoRequest.builder()
                .token(slackToken)
                .user(userId)
                .build();
        try {
            UsersInfoResponse response = slack.methods().usersInfo(usersInfoRequest);
            User user = response.getUser();
            if (nonNull(user)) {
                User.Profile profile = response.getUser().getProfile();
                /*
                 * Use the display name if it exists, otherwise use the real name that should always be set.
                 */
                username = profile.getDisplayName();
                if (isNull(username) || username.isEmpty()) {
                    username = profile.getRealName();
                }
                Log.info("Found username \"{0}\"", username);
            } else {
                Log.error("Cannot retrieve the username for {0}, returning the default username {1}", userId,
                        DEFAULT_USERNAME);
            }
        } catch (IOException | SlackApiException e) {
            Log.error("Cannot retrieve the username for {0}, returning the default username {1}", userId,
                    DEFAULT_USERNAME);
        }
        return username;
    }

    /**
     * Returns the {@link RTMClient} associated to this class.
     * <p>
     * <b>Note:</b> this method is protected for testing purposes, and should not be called by client code.
     *
     * @return the {@link RTMClient} associated to this class
     */
    protected RTMClient getRtmClient() {
        return rtmClient;
    }

    @Override
    public void run() {
        /*
         * Required because the RTM listener is started in another thread, and if this thread terminates the main
         * application terminates.
         */
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {

            }
        }
    }

    /**
     * Disconnects the underlying Slack RTM client.
     */
    @Override
    public void close() {
        Log.info("Closing Slack RTM connection");
        try {
            this.rtmClient.disconnect();
        } catch (IOException e) {
            String errorMessage = "Cannot close the Slack RTM connection";
            Log.error(errorMessage);
            throw new XatkitException(errorMessage, e);
        }
    }

    /**
     * The {@link RTMMessageHandler} used to process user messages.
     */
    private class XatkitRTMMessageHandler implements RTMMessageHandler {

        @Override
        public void handle(String message) {
            JsonObject json = jsonParser.parse(message).getAsJsonObject();
            if (nonNull(json.get("type"))) {
                /*
                 * The message has a type, this should always be true
                 */
                Log.info("received {0}", json);
                if (json.get("type").getAsString().equals(SlackUtils.HELLO_TYPE)) {
                    Log.info("Slack listener connected");
                }
                if (json.get("type").getAsString().equals(SlackUtils.MESSAGE_TYPE)) {
                    /*
                     * The message hasn't been sent by a bot
                     */
                    JsonElement channelObject = json.get("channel");
                    if (nonNull(channelObject)) {
                        /*
                         * The message channel is set
                         */
                        String channel = channelObject.getAsString();
                        JsonElement userObject = json.get("user");
                        if (nonNull(userObject)) {
                            /*
                             * The name of the user that sent the message
                             */
                            String user = userObject.getAsString();
                            if (!user.equals(botId)) {
                                JsonElement textObject = json.get("text");
                                if (nonNull(textObject)) {
                                    String text = textObject.getAsString();
                                    if (!text.isEmpty()) {
                                        Log.info("Received message {0} from user {1} (channel: {2})", text,
                                                user, channel);
                                        XatkitSession session = runtimePlatform.createSessionFromChannel(channel);
                                        /*
                                         * Call getRecognizedIntent before setting any context variable, the
                                         * recognition triggers a decrement of all the context variables.
                                         */
                                        RecognizedIntent recognizedIntent = SlackIntentProvider.this
                                                .getRecognizedIntent(text, session);
                                        /*
                                         * The slack-related values are stored in the local context with a
                                         * lifespan count of 1: they are reset every time a message is
                                         * received, and may cause consistency issues when using multiple
                                         * IntentProviders.
                                         */
                                        session.getRuntimeContexts().setContextValue(SlackUtils
                                                .SLACK_CONTEXT_KEY, 1, SlackUtils
                                                .CHAT_CHANNEL_CONTEXT_KEY, channel);
                                        session.getRuntimeContexts().setContextValue(SlackUtils
                                                .SLACK_CONTEXT_KEY, 1, SlackUtils
                                                .CHAT_USERNAME_CONTEXT_KEY, getUsernameFromUserId(user));
                                        session.getRuntimeContexts().setContextValue(SlackUtils.SLACK_CONTEXT_KEY, 1,
                                                SlackUtils.CHAT_RAW_MESSAGE_CONTEXT_KEY, text);
                                        /*
                                         * Copy the variables in the chat context (this context is inherited from the
                                         * Chat platform)
                                         */
                                        session.getRuntimeContexts().setContextValue(ChatUtils.CHAT_CONTEXT_KEY, 1,
                                                ChatUtils.CHAT_CHANNEL_CONTEXT_KEY, channel);
                                        session.getRuntimeContexts().setContextValue(ChatUtils.CHAT_CONTEXT_KEY, 1,
                                                ChatUtils.CHAT_USERNAME_CONTEXT_KEY, getUsernameFromUserId(user));
                                        session.getRuntimeContexts().setContextValue(ChatUtils.CHAT_CONTEXT_KEY, 1,
                                                ChatUtils.CHAT_RAW_MESSAGE_CONTEXT_KEY, text);

                                        if (recognizedIntent.getDefinition().getName().equals(
                                                "Default_Fallback_Intent") && ignoreFallbackOnGroupChannels) {
                                            /*
                                             * First check the property, if fallback intents are not ignored no need to
                                             * check if this is a group channel or not (this may trigger additional
                                             * Slack
                                             * API calls).
                                             */
                                            if (!SlackIntentProvider.this.runtimePlatform.isGroupChannel(channel)) {
                                                SlackIntentProvider.this.sendEventInstance(recognizedIntent, session);
                                            } else {
                                                /*
                                                 * Do nothing, fallback intents are ignored in group channels and
                                                 * this is a group channel.
                                                 */
                                            }
                                        } else {
                                            SlackIntentProvider.this.sendEventInstance(recognizedIntent, session);
                                        }
                                    } else {
                                        Log.warn("Received an empty message, skipping it");
                                    }
                                } else {
                                    Log.warn("The message does not contain a \"text\" field, skipping it");
                                }
                            } else {
                                Log.trace("Skipping {0}, the message was sent by this bot", json);
                            }
                        } else {
                            Log.warn("Skipping {0}, the message does not contain a \"user\" field",
                                    json);
                        }
                    } else {
                        Log.warn("Skipping {0}, the message does not contain a \"channel\" field", json);
                    }
                } else {
                    Log.trace("Skipping {0}, the message type is not \"{1}\"", json, SlackUtils.MESSAGE_TYPE);
                }
            } else {
                Log.error("The message does not define a \"type\" field, skipping it");
            }
        }
    }

    /**
     * The {@link RTMCloseHandler} used to handle RTM client connection issues.
     * <p>
     * This handler will attempt to reconnect the RTM client by creating a new {@link RTMClient} instance after
     * waiting {@code RECONNECT_WAIT_TIME * <number_of_attempts>} ms. Note that reconnecting the RTM client will be
     * executed in the main thread and will block Xatkit execution.
     *
     * @see #RECONNECT_WAIT_TIME
     */
    private class XatkitRTMCloseHandler implements RTMCloseHandler {

        @Override
        public void handle(CloseReason reason) {
            if (reason.getCloseCode().equals(CloseReason.CloseCodes.CLOSED_ABNORMALLY)) {
                Log.error("Connection to the Slack RTM client lost");
                int attempts = 1;
                while (true) {
                    try {
                        int waitTime = attempts * RECONNECT_WAIT_TIME;
                        Log.info("Trying to reconnect in {0}ms", waitTime);
                        Thread.sleep(waitTime);
                        rtmClient = slack.rtm(slackToken);
                        rtmClient.addMessageHandler(new XatkitRTMMessageHandler());
                        rtmClient.addCloseHandler(new XatkitRTMCloseHandler());
                        rtmClient.connect();
                        /*
                         * The RTM client is reconnected and the handlers are set.
                         */
                        break;
                    } catch (DeploymentException | IOException e) {
                        Log.error("Unable to reconnect the RTM client");
                    } catch (InterruptedException e) {
                        Log.error("An error occurred while waiting to reconnect the RTM client");
                    }
                }
            }
        }
    }
}
