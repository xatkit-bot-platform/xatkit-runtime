package fr.zelus.jarvis.stubs;

import fr.zelus.jarvis.core.JarvisModule;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.intent.EventInstance;
import fr.zelus.jarvis.orchestration.ActionInstance;

/**
 * An empty {@link JarvisModule} used to test {@link JarvisModule}-related methods.
 * <p>
 * See {@link StubJarvisModule} to create a stub {@link JarvisModule} that provided preset
 * {@link StubJarvisModule#getAction()} and
 * {@link StubJarvisModule#createJarvisAction(ActionInstance, EventInstance, JarvisSession)} methods.
 */
public class EmptyJarvisModule extends JarvisModule {
}
