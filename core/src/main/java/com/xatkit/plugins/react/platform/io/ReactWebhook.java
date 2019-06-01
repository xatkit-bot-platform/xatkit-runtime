package com.xatkit.plugins.react.platform.io;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xatkit.core.platform.io.JsonWebhookEventProvider;
import com.xatkit.core.session.JarvisSession;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.plugins.chat.ChatUtils;
import com.xatkit.plugins.react.platform.ReactPlatform;
import com.xatkit.plugins.react.platform.ReactUtils;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;
import org.apache.http.Header;

import java.util.Collections;
import java.util.List;

import static java.util.Objects.nonNull;

/**
 * A {@link JsonWebhookEventProvider} that receives jarvis-react messages and convert them into
 * {@link RecognizedIntent}.
 * <p>
 * This class should not be initialized reflexively by the core framework: it is created by the
 * {@link ReactIntentProvider} to receive messages and convert them to {@link RecognizedIntent}.
 */
class ReactWebhook extends JsonWebhookEventProvider<ReactPlatform> {

    private static final String JARVIS_REACT_HEADER = "jarvis-react";

    /**
     * The {@link ReactIntentProvider} managing this {@link ReactWebhook}.
     */
    private ReactIntentProvider baseProvider;

    /**
     * Construct a new {@link ReactWebhook} from the provided {@code runtimePlatform} and {@code configuration}.
     * <p>
     * This constructor is package-private, and should only be called by {@link ReactIntentProvider}.
     *
     * @param runtimePlatform the {@link ReactPlatform} containing the {@link ReactIntentProvider}
     * @param configuration   the platform's {@link Configuration}
     * @param baseProvider    the {@link ReactIntentProvider} managing this webhook
     */
    ReactWebhook(ReactPlatform runtimePlatform, Configuration configuration,
                 ReactIntentProvider baseProvider) {
        super(runtimePlatform, configuration);
        this.baseProvider = baseProvider;
    }

    @Override
    public List<String> getAccessControlAllowHeaders() {
        return Collections.singletonList(JARVIS_REACT_HEADER);
    }

    @Override
    protected void handleParsedContent(JsonElement parsedContent, Header[] headers) {
        if (containsJarvisReact(headers)) {
            JsonObject content = parsedContent.getAsJsonObject();
            String username = content.get("username").getAsString();
            String channel = content.get("channel").getAsString();
            JsonElement message = content.get("message");
            if (nonNull(message)) {
                String textMessage = message.getAsString();
                JarvisSession session = runtimePlatform.createSessionFromChannel(channel);
                RecognizedIntent recognizedIntent = baseProvider.getRecognizedIntent(textMessage, session);
                session.getRuntimeContexts().setContextValue(ReactUtils.REACT_CONTEXT_KEY, 1,
                        ReactUtils.CHAT_USERNAME_CONTEXT_KEY, username);
                session.getRuntimeContexts().setContextValue(ReactUtils.REACT_CONTEXT_KEY, 1,
                        ReactUtils.CHAT_CHANNEL_CONTEXT_KEY, channel);
                session.getRuntimeContexts().setContextValue(ReactUtils.REACT_CONTEXT_KEY, 1,
                        ReactUtils.CHAT_RAW_MESSAGE_CONTEXT_KEY, textMessage);
                /*
                 * This provider extends ChatIntentProvider, and must set chat-related context values.
                 */
                session.getRuntimeContexts().setContextValue(ChatUtils.CHAT_CONTEXT_KEY, 1,
                        ChatUtils.CHAT_USERNAME_CONTEXT_KEY, username);
                session.getRuntimeContexts().setContextValue(ChatUtils.CHAT_CONTEXT_KEY, 1,
                        ChatUtils.CHAT_CHANNEL_CONTEXT_KEY, channel);
                session.getRuntimeContexts().setContextValue(ChatUtils.CHAT_CONTEXT_KEY, 1,
                        ChatUtils.CHAT_RAW_MESSAGE_CONTEXT_KEY, textMessage);
                /*
                 * Use the base provider sendEventInstance method to ensure that the chat context are checked.
                 */
                baseProvider.sendEventInstance(recognizedIntent, session);
            }
            Log.info("Received a message from user {0} (channel {1}): {2}", username, channel, message);
        } else {
            Log.error("Does not contain Xatkit REACt header");
        }
    }

    @Override
    public void run() {
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {

            }
        }
    }

    /**
     * Checks if the provided {@link Header}s contain the JARVIS_REACT_HEADER.
     *
     * @param headers the {@link Header}s to check
     * @return {@code true} if the provided {@code headers} contain the JARVIS_REACT_HEADER, {@code false} otherwise
     */
    private boolean containsJarvisReact(Header[] headers) {
        for (int i = 0; i < headers.length; i++) {
            Header h = headers[i];
            if (h.getName().equals(JARVIS_REACT_HEADER)) {
                return true;
            }
        }
        return false;
    }
}
