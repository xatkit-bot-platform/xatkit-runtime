package fr.zelus.jarvis.slack.io;

import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.rtm.RTMClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisException;
import fr.zelus.jarvis.io.InputProvider;
import fr.zelus.jarvis.slack.JarvisSlackUtils;
import org.apache.commons.configuration2.Configuration;

import javax.websocket.DeploymentException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static fr.zelus.jarvis.slack.JarvisSlackUtils.HELLO_TYPE;
import static fr.zelus.jarvis.slack.JarvisSlackUtils.MESSAGE_TYPE;
import static fr.zelus.jarvis.slack.JarvisSlackUtils.SLACK_TOKEN_KEY;
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
 * messages
 * through
 * the RTM API.
 *
 * @see InputProvider
 */
public class SlackInputProvider extends InputProvider {

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
     * The {@link JsonParser} used to manipulate Slack API answers.
     */
    private JsonParser jsonParser;

    /**
     * The {@link PrintWriter} used to write received messages to the {@link #outputStream}.
     */
    private PrintWriter writer;


    /**
     * Constructs a new {@link SlackInputProvider} from the provided {@link Configuration}.
     * <p>
     * This constructor initializes the underlying RTM connection and creates a message listener that forwards to
     * the output stream not empty direct messages sent by users (not bots) to the Slack bot associated to this class.
     * <p>
     * <b>Note:</b> {@link SlackInputProvider} requires a valid Slack bot API token to be initialized, and calling
     * the default constructor will throw a {@link IllegalArgumentException} when looking for the Slack bot API token.
     *
     * @param configuration the {@link Configuration} used to retrieve the Slack bot API token
     * @throws NullPointerException     if the provided {@link Configuration} is {@code null}
     * @throws IllegalArgumentException if the provided Slack bot API token is {@code null} or empty
     */
    public SlackInputProvider(Configuration configuration) {
        super(configuration);
        checkNotNull(configuration, "Cannot construct a SlackInputProvider from a null configuration");
        this.slackToken = configuration.getString(SLACK_TOKEN_KEY);
        checkArgument(nonNull(slackToken) && !slackToken.isEmpty(), "Cannot construct a SlackInputProvider from the " +
                "provided token %s, please ensure that the jarvis configuration contains a valid Slack bot API token " +
                "associated to the key %s", slackToken, SLACK_TOKEN_KEY);
        this.writer = new PrintWriter(outputStream, true);
        Slack slack = new Slack();
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
                                JsonElement textObject = json.get("text");
                                if (nonNull(textObject)) {
                                    String text = textObject.getAsString();
                                    if (!text.isEmpty()) {
                                        Log.info("Received message {0}", text);
                                        this.writer.println(text);
                                    } else {
                                        Log.warn("Received an empty message, skipping it");
                                    }
                                } else {
                                    Log.warn("The message does not contain a \"text\" field, skipping it");
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
     * Returns the {@link RTMClient} associated to this class.
     * <p>
     * <b>Note:</b> this method is protected for testing purposes, and should not be called by client code.
     *
     * @return the {@link RTMClient} associated to this class
     */
    protected RTMClient getRtmClient() {
        return rtmClient;
    }

    /**
     * Disconnects the underlying Slack RTM client and closes the message output stream.
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
        /*
         * Close parent-related elements (such as the output stream).
         */
        super.close();
    }
}
