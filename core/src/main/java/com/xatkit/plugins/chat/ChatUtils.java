package com.xatkit.plugins.chat;

import com.xatkit.core.session.RuntimeContexts;

/**
 * An utility class that holds Chat-related helpers and context variable keys.
 */
public interface ChatUtils {

    /**
     * The {@link RuntimeContexts} key used to store chat-related information.
     */
    String CHAT_CONTEXT_KEY = "chat";

    /**
     * The {@link RuntimeContexts} variable key used to store chat channel information.
     */
    String CHAT_CHANNEL_CONTEXT_KEY = "channel";

    /**
     * The {@link RuntimeContexts} variable key used to store chat username information.
     */
    String CHAT_USERNAME_CONTEXT_KEY = "username";

    /**
     * The {@link RuntimeContexts} variable key used to store chat raw message
     * information.
     */
    String CHAT_RAW_MESSAGE_CONTEXT_KEY = "rawMessage";
}
