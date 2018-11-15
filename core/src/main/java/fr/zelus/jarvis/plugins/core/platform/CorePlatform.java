package fr.zelus.jarvis.plugins.core.platform;

import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.core.RuntimeAction;
import fr.zelus.jarvis.core.RuntimePlatform;

/**
 * A {@link RuntimePlatform} concrete implementation providing core functionality that can be used in orchestration models.
 * <p>
 * This runtimePlatform defines a set of high level {@link RuntimeAction}s:
 * <ul>
 * <li>{@link fr.zelus.jarvis.plugins.core.platform.action.GetTime}: return the current time</li>
 * <li>{@link fr.zelus.jarvis.plugins.core.platform.action.GetDate}: return the current date</li>
 * </ul>
 * <p>
 * This class is part of jarvis' core platforms, and can be used in an orchestration model by importing the
 * <i>CorePlatform</i> package.
 *
 * @see fr.zelus.jarvis.plugins.core.platform.action.GetTime
 * @see fr.zelus.jarvis.plugins.core.platform.action.GetDate
 */
public class CorePlatform extends RuntimePlatform {

    /**
     * Constructs a new {@link CorePlatform} from the provided {@link JarvisCore}.
     *
     * @param jarvisCore the {@link JarvisCore} instance associated to this runtimePlatform
     * @throws NullPointerException if the provided {@code jarvisCore} is {@code null}
     */
    public CorePlatform(JarvisCore jarvisCore) {
        super(jarvisCore);
    }
}
