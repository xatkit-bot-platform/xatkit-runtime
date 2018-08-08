package fr.zelus.jarvis.plugins.github.module;

import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.core.JarvisModule;

public class GithubModule extends JarvisModule {

    /**
     * Constructs a new {@link GithubModule} from the provided {@link JarvisCore}.
     *
     * @param jarvisCore the {@link JarvisCore} instance associated to this module
     * @throws NullPointerException if the provided {@code jarvisCore} is {@code null}
     */
    public GithubModule(JarvisCore jarvisCore) {
        super(jarvisCore);
    }
}
