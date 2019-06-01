package com.xatkit.plugins.chat.platform.io;

import com.xatkit.core.platform.io.RuntimeIntentProvider;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.EventInstance;
import com.xatkit.plugins.chat.ChatUtils;
import com.xatkit.plugins.chat.platform.ChatPlatform;
import org.apache.commons.configuration2.Configuration;

import java.util.Map;

import static fr.inria.atlanmod.commons.Preconditions.checkState;
import static java.util.Objects.nonNull;

/**
 * An abstract chat {@link RuntimeIntentProvider}.
 * <p>
 * This class is extended by provider of platforms extending the {@link ChatPlatform}, and ensures that all the
 * chat-related context variables have been set before triggering actions associated to the computed event.
 *
 * @param <T> the concrete {@link ChatPlatform} that contains this provider
 */
public abstract class ChatIntentProvider<T extends ChatPlatform> extends RuntimeIntentProvider<T> {

    /**
     * Constructs a new {@link ChatIntentProvider} with the provided {@code runtimePlatform} and {@code configuration}.
     *
     * @param runtimePlatform the {@link ChatPlatform} containing this provider
     * @param configuration   the {@link Configuration} used to initialize this provider
     */
    public ChatIntentProvider(T runtimePlatform, Configuration configuration) {
        super(runtimePlatform, configuration);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method checks that the context variables defined in {@link ChatUtils} have been initialized by the
     * concrete provider. This ensures that all the providers extending {@link ChatIntentProvider} set the same set
     * of variables and can be used transparently in execution models.
     */
    @Override
    public void sendEventInstance(EventInstance eventInstance, XatkitSession session) {
        Map<String, Object> contextVariables =
                session.getRuntimeContexts().getContextVariables(ChatUtils.CHAT_CONTEXT_KEY);
        checkState(nonNull(contextVariables), "Intent provider %s did not define the context %s",
                this.getClass().getSimpleName(), ChatUtils.CHAT_CONTEXT_KEY);
        checkState(nonNull(contextVariables.get(ChatUtils.CHAT_CHANNEL_CONTEXT_KEY)), "Intent provider %s did not " +
                        "define the context variable %s.%s", this.getClass().getSimpleName(),
                ChatUtils.CHAT_CONTEXT_KEY,
                ChatUtils.CHAT_CHANNEL_CONTEXT_KEY);
        checkState(nonNull(contextVariables.get(ChatUtils.CHAT_USERNAME_CONTEXT_KEY)), "Intent provider %s did not " +
                        "define the context variable %s.%s", this.getClass().getSimpleName(),
                ChatUtils.CHAT_USERNAME_CONTEXT_KEY);
        checkState(nonNull(contextVariables.get(ChatUtils.CHAT_RAW_MESSAGE_CONTEXT_KEY)), "Intent provider %s did not" +
                " define the context variable %s.%s", this.getClass().getSimpleName());
        super.sendEventInstance(eventInstance, session);
    }
}
