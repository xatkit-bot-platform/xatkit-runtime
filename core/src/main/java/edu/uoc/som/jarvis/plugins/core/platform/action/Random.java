package edu.uoc.som.jarvis.plugins.core.platform.action;

import edu.uoc.som.jarvis.core.platform.action.RuntimeAction;
import edu.uoc.som.jarvis.core.session.JarvisSession;
import edu.uoc.som.jarvis.plugins.core.platform.CorePlatform;

/**
 * A {@link RuntimeAction} that returns a random number smaller than the provided {@code bound}.
 * <p>
 * This action generates {@link Integer} numbers between {@code 0} (inclusive) and {@code bount} (exclusive).
 */
public class Random extends RuntimeAction<CorePlatform> {

    /**
     * The upper bound (exclusive) of random numbers to generate.
     */
    private Integer bound;

    /**
     * Constructs a new {@link Random} action from the provided {@code runtimePlatform}, {@code session}, and {@code
     * bound}.
     *
     * @param runtimePlatform the {@link CorePlatform} containing this action
     * @param session         the {@link JarvisSession} associated to this action
     * @param bound           the upper bound (exclusive) of random number to generate
     * @throws NullPointerException if the provided {@code runtimePlatform} or {@code session} is {@code null}
     */
    public Random(CorePlatform runtimePlatform, JarvisSession session, Integer bound) {
        super(runtimePlatform, session);
        this.bound = bound;
    }

    /**
     * Returns a random number smaller than the provided {@code bound}.
     *
     * @return the random number
     */
    @Override
    public Object compute() {
        return new java.util.Random().nextInt(bound);
    }
}
