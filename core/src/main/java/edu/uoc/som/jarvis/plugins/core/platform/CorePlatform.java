package edu.uoc.som.jarvis.plugins.core.platform;

import edu.uoc.som.jarvis.plugins.core.platform.action.GetDate;
import edu.uoc.som.jarvis.plugins.core.platform.action.GetTime;
import edu.uoc.som.jarvis.core.JarvisCore;
import edu.uoc.som.jarvis.core.platform.action.RuntimeAction;
import edu.uoc.som.jarvis.core.platform.RuntimePlatform;

/**
 * A {@link RuntimePlatform} concrete implementation providing core functionality that can be used in execution models.
 * <p>
 * This runtimePlatform defines a set of high level {@link RuntimeAction}s:
 * <ul>
 * <li>{@link GetTime}: return the current time</li>
 * <li>{@link GetDate}: return the current date</li>
 * </ul>
 * <p>
 * This class is part of jarvis' core platforms, and can be used in an execution model by importing the
 * <i>CorePlatform</i> package.
 *
 * @see GetTime
 * @see GetDate
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
