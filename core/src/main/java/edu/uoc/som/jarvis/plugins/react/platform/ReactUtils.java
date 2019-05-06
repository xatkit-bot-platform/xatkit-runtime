package edu.uoc.som.jarvis.plugins.react.platform;

import edu.uoc.som.jarvis.plugins.chat.ChatUtils;

/**
 * An utility interface that holds jarvis-react related helpers and context keys.
 */
public interface ReactUtils extends ChatUtils {

    /**
     * The {@link edu.uoc.som.jarvis.core.session.RuntimeContexts} key used to store React-related information.
     */
    public static final String REACT_CONTEXT_KEY = "react";
}
