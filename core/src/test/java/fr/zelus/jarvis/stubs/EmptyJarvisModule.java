package fr.zelus.jarvis.stubs;

import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.core.JarvisModule;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.orchestration.ActionInstance;

/**
 * An empty {@link JarvisModule} used to test {@link JarvisModule}-related methods.
 * <p>
 * See {@link StubJarvisModule} to create a stub {@link JarvisModule} that provided preset
 * {@link StubJarvisModule#getAction()} and
 * {@link StubJarvisModule#createJarvisAction(ActionInstance, JarvisSession)} methods.
 */
public class EmptyJarvisModule extends JarvisModule {

    /**
     * Constructs a new {@link EmptyJarvisModule} from the provided {@link JarvisCore}.
     *
     * @param jarvisCore the {@link JarvisCore} instance associated to this module
     * @throws NullPointerException if the provided {@code jarvisCore} is {@code null}
     */
    public EmptyJarvisModule(JarvisCore jarvisCore) {
        super(jarvisCore);
    }
}
