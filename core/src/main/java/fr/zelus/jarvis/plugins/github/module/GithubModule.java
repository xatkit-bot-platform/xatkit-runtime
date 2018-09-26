package fr.zelus.jarvis.plugins.github.module;

import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.core.JarvisModule;
import org.apache.commons.configuration2.Configuration;

public class GithubModule extends JarvisModule {

    /**
     * Constructs a new {@link GithubModule} from the provided {@link JarvisCore}.
     *
     * @param jarvisCore the {@link JarvisCore} instance associated to this module
     * @param configuration the {@link Configuration} used to customize Github events and actions

     * @throws NullPointerException if the provided {@code jarvisCore} or {@code configuration} is {@code null}
     */
    public GithubModule(JarvisCore jarvisCore, Configuration configuration) {
        super(jarvisCore, configuration);
    }
}
