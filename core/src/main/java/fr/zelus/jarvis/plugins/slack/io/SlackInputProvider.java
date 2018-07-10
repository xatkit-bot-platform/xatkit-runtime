package fr.zelus.jarvis.plugins.slack.io;

import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.request.users.UsersInfoRequest;
import com.github.seratch.jslack.api.methods.response.users.UsersInfoResponse;
import com.github.seratch.jslack.api.model.User;
import com.github.seratch.jslack.api.rtm.RTMClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.core.JarvisException;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.io.InputProvider;
import fr.zelus.jarvis.plugins.slack.JarvisSlackUtils;
import org.apache.commons.configuration2.Configuration;

import javax.websocket.DeploymentException;
import java.io.IOException;
import java.text.MessageFormat;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static fr.zelus.jarvis.plugins.slack.JarvisSlackUtils.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A Slack user input provider.
 * <p>
 * This class relies on the Slack RTM API to receive direct messages and react to them. Note that this input provider
 * only captures direct messages sent to the Slack bot associated to this class.
 * <p>
 * Instances of this class must be configured with a {@link Configuration} instance holding the Slack bot API token
 * in the property {@link JarvisSlackUtils#SLACK_TOKEN_KEY}. This token is used to authenticate the bot and receive
 * messages through the RTM API.
 *
 * @see JarvisSlackUtils
 * @see InputProvider
 */
public class SlackInputProvider extends InputProvider {

    /**
     * The default username returned by {@link #getUsernameFromUserId(String)}.
     *
     * @see #getUsernameFromUserId(String)
     */
    private static String DEFAULT_USERNAME = "unknown user";

    /**
     * The {@link String} representing the Slack bot API token.
     * <p>
     * This token is used to authenticate the bot and receive messages through the RTM API.
     */
    private String slackToken;

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
     * Constructs a new {@link SlackInputProvider} from the provided {@link JarvisCore} and {@link Configuration}.
     * <p>
     * This constructor initializes the underlying RTM connection and creates a message listener that forwards to
     * the {@code jarvisCore} instance not empty direct messages sent by users (not bots) to the Slack bot associated
     * to this class.
     * <p>
     * <b>Note:</b> {@link SlackInputProvider} requires a valid Slack bot API token to be initialized, and calling
     * the default constructor will throw an {@link IllegalArgumentException} when looking for the Slack bot API token.
     *
     * @param jarvisCore    the {@link JarvisCore} instance used to handle messages
     * @param configuration the {@link Configuration} used to retrieve the Slack bot API token
     * @throws NullPointerException     if the provided {@link Configuration} is {@code null}
     * @throws IllegalArgumentException if the provided Slack bot API token is {@code null} or empty
     */
    public SlackInputProvider(JarvisCore jarvisCore, Configuration configuration) {
        super(jarvisCore, configuration);
        checkNotNull(configuration, "Cannot construct a SlackInputProvider from a null configuration");
        this.slackToken = configuration.getString(SLACK_TOKEN_KEY);
        checkArgument(nonNull(slackToken) && !slackToken.isEmpty(), "Cannot construct a SlackInputProvider from the " +
                "provided token %s, please ensure that the jarvis configuration contains a valid Slack bot API token " +
                "associated to the key %s", slackToken, SLACK_TOKEN_KEY);
        slack = new Slack();
        try {
            this.rtmClient = slack.rtm(slackToken);
        } catch (IOException e) {
            String errorMessage = MessageFormat.format("Cannot connect SlackInputProvider, please ensure that the bot" +
                    " API token is valid and stored in jarvis configuration with the key {0}", SLACK_TOKEN_KEY);
            Log.error(errorMessage);
            throw new JarvisException(errorMessage, e);
        }
        this.jsonParser = new JsonParser();
        Log.info("Starting to listen jarvis Slack direct messages");
        rtmClient.addMessageHandler((message) -> {
                    JsonObject json = jsonParser.parse(message).getAsJsonObject();
                    if (nonNull(json.get("type"))) {
                        /*
                         * The message has a type, this should always be true
                         */
                        Log.info("received {0}", json);
                        if (json.get("type").getAsString().equals(HELLO_TYPE)) {
                            Log.info("Slack listener connected");
                        }
                        if (json.get("type").getAsString().equals(MESSAGE_TYPE)) {
                            /*
                             * The message has the MESSAGE_TYPE type, and can be processed as a user input
                             */
                            if (isNull(json.get("bot_id"))) {
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
                                        JsonElement textObject = json.get("text");
                                        if (nonNull(textObject)) {
                                            String text = textObject.getAsString();
                                            if (!text.isEmpty()) {
                                                Log.info("Received message {0} from user {1} (channel: {2})", text,
                                                        user, channel);
                                                JarvisSession session = jarvisCore.getOrCreateJarvisSession(channel);
                                                session.getJarvisContext().setContextValue(JarvisSlackUtils
                                                        .SLACK_CONTEXT_KEY, JarvisSlackUtils
                                                        .SLACK_CHANNEL_CONTEXT_KEY, channel);
                                                session.getJarvisContext().setContextValue(JarvisSlackUtils
                                                        .SLACK_CONTEXT_KEY, JarvisSlackUtils
                                                        .SLACK_USERNAME_CONTEXT_KEY, getUsernameFromUserId(user));
                                                jarvisCore.handleMessage(text, session);
                                            } else {
                                                Log.warn("Received an empty message, skipping it");
                                            }
                                        } else {
                                            Log.warn("The message does not contain a \"text\" field, skipping it");
                                        }
                                    } else {
                                        Log.warn("Skipping {0}, the message does not contain a \"user\" field",
                                                json);
                                    }
                                } else {
                                    Log.warn("Skipping {0}, the message does not contain a \"channel\" field", json);
                                }
                            } else {
                                Log.trace("Skipping {0}, the message has been sent by a bot", json);
                            }
                        } else {
                            Log.trace("Skipping {0}, the message type is not \"{1}\"", json, MESSAGE_TYPE);
                        }
                    } else {
                        Log.error("The message does not define a \"type\" field, skipping it");
                    }
                }
        );
        try {
            rtmClient.connect();
        } catch (DeploymentException | IOException e) {
            String errorMessage = "Cannot start the Slack RTM websocket, please check your internet connection";
            Log.error(errorMessage);
            throw new JarvisException(errorMessage, e);
        }
    }

    /**
     * Returns the Slack username associated to the provided {@code userId}.
     * <p>
     * This method returns {@link #DEFAULT_USERNAME} if the Slack API is not reachable or if the provided {@code
     * userId} does not match any known user.
     *
     * @param userId the user identifier to retrieve the username from
     * @return the Slack username associated to the provided {@code userId}
     */
    private String getUsernameFromUserId(String userId) {
        Log.info("Retrieving username for {0}", userId);
        String username = DEFAULT_USERNAME;
        UsersInfoRequest usersInfoRequest = UsersInfoRequest.builder()
                .token(slackToken)
                .user(userId)
                .build();
        try {
            UsersInfoResponse response = slack.methods().usersInfo(usersInfoRequest);
            User user = response.getUser();
            if (nonNull(user)) {
                username = response.getUser().getProfile().getDisplayName();
                Log.info("Found username {0}", username);
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
            throw new JarvisException(errorMessage, e);
        }
    }
}
