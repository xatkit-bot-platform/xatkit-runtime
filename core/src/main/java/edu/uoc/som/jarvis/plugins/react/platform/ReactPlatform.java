package edu.uoc.som.jarvis.plugins.react.platform;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import edu.uoc.som.jarvis.core.JarvisCore;
import edu.uoc.som.jarvis.core.session.JarvisSession;
import edu.uoc.som.jarvis.plugins.chat.platform.ChatPlatform;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.isNull;

/**
 * A {@link ChatPlatform} class that interacts with the
 * <a href="https://github.com/jarvis-platform/jarvis-react">Jarvis React component</a>.
 * <p>
 * This {@link ChatPlatform} manages a list of pending messages that can be queried by the jarvis-react application
 * to display them. It initializes a REST endpoint and registers it to the Jarvis server, allowing to reply to
 * jarvis-react REST queries.
 * <p>
 * This platform provides the following actions:
 * <ul>
 * <li>{@link edu.uoc.som.jarvis.plugins.react.platform.action.Reply}: replies to a user input</li>
 * <li>{@link edu.uoc.som.jarvis.plugins.react.platform.action.PostMessage}: post a message to a given channel
 * (i.e. window running a jarvis-react instance)</li>
 * </ul>
 * <p>
 * This platform registers a webhook at {@code /react/getAnswer}, that provides the last answer associated to a given
 * channel.
 * <p>
 * This class is part of jarvis" core paltform, and can be used in an execution model by importing the
 * <i>ReactPlatform</i> package.
 */
public class ReactPlatform extends ChatPlatform {

    /**
     * The {@link Map} containing the pending messages that can be queried by the jarvis-react application.
     */
    private Map<String, String> storedMessages;

    /**
     * Constructs a new {@link ReactPlatform} from the provided {@link JarvisCore} and {@link Configuration}.
     * <p>
     * This constructor initializes the underlying REST endpoint at {@code /react/getAnswer}, allowing to retrieve
     * the last answer associated to a channel.
     * <p>
     * A REST request on {@code /rest/getAnswer} must contain a JSON object with a {@code channel} field containing
     * the channel to retrieve the answer for.
     *
     * @param jarvisCore    the {@link JarvisCore} instance associated to this runtimePlatform
     * @param configuration the platform's {@link Configuration}
     * @throws NullPointerException if the provided {@code jarvisCore} or {@code configuration} is {@code null}
     */
    public ReactPlatform(JarvisCore jarvisCore, Configuration configuration) {
        super(jarvisCore, configuration);
        this.storedMessages = new HashMap<>();
        /*
         * Register the REST endpoint at /react/getAnswer. Note that the received request must contain a JSON object
         * defining the channel field.
         */
        this.getJarvisCore().getJarvisServer().registerRestEndpoint("/react/getAnswer",
                ((headers, param, content) -> {
                    JsonObject contentObject = content.getAsJsonObject();
                    String channel = contentObject.get("channel").getAsString();
                    JsonObject result = new JsonObject();
                    String lastMessage = this.getLastMessageFor(channel);
                    if (isNull(lastMessage)) {
                        Log.warn("No message awaiting for channel {1}", channel);
                    } else {
                        result.add("value", new JsonPrimitive(lastMessage));
                    }
                    return result;
                }));
    }

    /**
     * Stores the provided {@code message} for the given {@code channel}.
     * <p>
     * The stored message can be accessed by jarvis-react through the {@code /react/getAnswer} endpoint.
     *
     * @param channel the channel associated to the message to store
     * @param message the message to store
     */
    public void storeMessage(String channel, String message) {
        this.storedMessages.put(channel, message);
    }

    /**
     * Returns the last message stored for the provided {@code channel}.
     * <p>
     * This method removes the returned message from the message queue, meaning that calling again this method will
     * return a different result or {@code null}. Storing returned messages is the responsibility of client
     * applications.
     *
     * @param channel the channel to retrieve the message for
     * @return the last message stored for the provided {@code channel}
     */
    public String getLastMessageFor(String channel) {
        String result = this.storedMessages.get(channel);
        this.storedMessages.remove(channel);
        return result;
    }

    /**
     * Creates a {@link JarvisSession} from the provided {@code channel}.
     * <p>
     * This method ensures that the same {@link JarvisSession} is returned for the same {@code channel}.
     *
     * @param channel the channel to create a {@link JarvisSession} from
     * @return the created {@link JarvisSession}
     */
    public JarvisSession createSessionFromChannel(String channel) {
        return this.jarvisCore.getOrCreateJarvisSession(channel);
    }

}
