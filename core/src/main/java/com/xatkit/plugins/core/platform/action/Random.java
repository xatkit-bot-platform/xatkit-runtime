package com.xatkit.plugins.core.platform.action;

import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.plugins.core.platform.CorePlatform;

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
     * @param session         the {@link XatkitSession} associated to this action
     * @param bound           the upper bound (exclusive) of random number to generate
     * @throws NullPointerException if the provided {@code runtimePlatform} or {@code session} is {@code null}
     */
    public Random(CorePlatform runtimePlatform, XatkitSession session, Integer bound) {
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
