package fr.zelus.jarvis.core;

import com.google.cloud.dialogflow.v2.Intent;
import fr.zelus.jarvis.spotify.SpotifyModule;

/**
 * Represents a set of semantically related functions that accepts {@link Intent}s and create {@link JarvisAction}s
 * from them.
 * <p>
 * {@link JarvisModule}s are used to represent action libraries, for example a set of Spotify-related actions (see
 * {@link SpotifyModule}). Handling an {@link Intent} consist of processing its
 * content (context variables, previous {@link Intent}, etc) and returning a {@link JarvisAction} representing the
 * action to perform. The returned {@link JarvisAction} is then processed by the
 * {@link JarvisCore} component, that orchestrate the different {@link JarvisModule}s.
 *
 * @see JarvisCore
 * @see JarvisAction
 */
public interface JarvisModule {

    /**
     * Checks whether the module can process the provided {@code intent}.
     * <p>
     * <b>Note:</b> this method should not modify the provided {@code intent}.
     *
     * @param intent the DialogFlow {@link Intent} to check
     * @return {@code true} if the module can process the provided {@code intent}, {@code false} otherwise
     */
    boolean acceptIntent(final Intent intent);

    /**
     * Processes the provided {@code intent}.
     * <p>
     * Processing an {@link Intent} does not have a direct impact on other {@link JarvisModule}s: each {@link Intent}
     * is processed by all the {@link JarvisModule} that accept it.
     * <p>
     * <b>Note:</b> this method should not modify the provided {@code intent}.
     *
     * @param intent
     */
    JarvisAction handleIntent(final Intent intent);

}
