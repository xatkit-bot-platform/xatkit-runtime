package com.xatkit.core.interpreter.operation.object;

import com.xatkit.core.interpreter.operation.Operation;

import javax.annotation.Nonnull;

/**
 * Computes a <i>greater than</i> comparison between the provided {@code source} and {@code args}.
 * <p>
 * This {@link Operation} accepts a single mandatory {@code args} element (the element to compare against the
 * provided {@code source}). Note that both the {@code source} and the {@code args} elements must be {@link Number}
 * instances.
 */
public class GreaterOperation extends ArithmeticOperation {

    /**
     * Computes a <i>greater than</i> comparison between the provided {@code source} and {@code target}.
     *
     * @param source the {@link Number} to invoke the {@link GreaterOperation} on
     * @param target the arguments of the operation to invoke
     * @return {@code true} if {@code source} is greater than {@code target}, {@code false} otherwise
     */
    @Override
    protected Object doOperation(@Nonnull Number source, @Nonnull Number target) {
        return source.floatValue() > target.floatValue();
    }
}
