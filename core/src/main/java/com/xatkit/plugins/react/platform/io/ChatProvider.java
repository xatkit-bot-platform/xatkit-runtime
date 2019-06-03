package com.xatkit.plugins.react.platform.io;

import com.xatkit.core.platform.io.RuntimeIntentProvider;
import com.xatkit.plugins.react.platform.ReactPlatform;
import org.apache.commons.configuration2.Configuration;

/**
 * A generic React {@link RuntimeIntentProvider}.
 * <p>
 * This class wraps the {@link ReactIntentProvider} and allows to use it as a generic <i>ChatIntentProvider</i> from
 * the <i>ChatPlatform</i>.
 *
 * @see ReactIntentProvider
 */
public class ChatProvider extends ReactIntentProvider {

    /**
     * Constructs a new {@link ChatProvider} from the provided {@code runtimePlatform} and {@code configuration}.
     *
     * @param runtimePlatform the {@link ReactPlatform} containing this {@link ChatProvider}
     * @param configuration   the {@link Configuration} used to initialize the {@link ReactPlatform}
     */
    public ChatProvider(ReactPlatform runtimePlatform, Configuration configuration) {
        super(runtimePlatform, configuration);
    }
}
