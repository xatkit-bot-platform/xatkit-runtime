package com.xatkit.plugins.chat.platform;

import com.xatkit.core.XatkitCore;
import com.xatkit.core.platform.RuntimePlatform;
import org.apache.commons.configuration2.Configuration;

/**
 * An abstract chat {@link RuntimePlatform}.
 */
public abstract class ChatPlatform extends RuntimePlatform {

    /**
     * Constructs a new {@link ChatPlatform} from the provided {@link XatkitCore} and {@link Configuration}.
     *
     * @param xatkitCore    the {@link XatkitCore} instance associated to this runtimePlatform
     * @param configuration the {@link Configuration} used to initialize this runtimePlatform
     */
    public ChatPlatform(XatkitCore xatkitCore, Configuration configuration) {
        super(xatkitCore, configuration);
    }
}
