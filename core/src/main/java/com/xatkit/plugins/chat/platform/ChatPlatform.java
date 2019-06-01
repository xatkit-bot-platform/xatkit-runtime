package edu.uoc.som.jarvis.plugins.chat.platform;

import edu.uoc.som.jarvis.core.JarvisCore;
import edu.uoc.som.jarvis.core.platform.RuntimePlatform;
import org.apache.commons.configuration2.Configuration;

/**
 * An abstract chat {@link RuntimePlatform}.
 */
public abstract class ChatPlatform extends RuntimePlatform {

    /**
     * Constructs a new {@link ChatPlatform} from the provided {@link JarvisCore} and {@link Configuration}.
     *
     * @param jarvisCore    the {@link JarvisCore} instance associated to this runtimePlatform
     * @param configuration the {@link Configuration} used to initialize this runtimePlatform
     */
    public ChatPlatform(JarvisCore jarvisCore, Configuration configuration) {
        super(jarvisCore, configuration);
    }
}
