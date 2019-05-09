package edu.uoc.som.jarvis.core.interpreter.operation.object;

import edu.uoc.som.jarvis.core.interpreter.operation.Operation;

import java.util.List;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

/**
 * Checks if the provided {@code source} is different from {@code null}.
 */
public class NonNullOperation implements Operation {

    /**
     * Returns whether the provided {@code source} is different from {@code null}.
     * <p>
     * This method returns the same value as {@link java.util.Objects#nonNull(Object)}.
     *
     * @param source the {@link Object} to invoke the {@link NonNullOperation} on
     * @param args   the arguments of the operation to invoke
     * @return {@code true} if the provided {@code source} is different from {@code null}, {@code false} otherwise
     * @throws IllegalArgumentException if the provided {@code args} is not empty
     */
    @Override
    public Object invoke(Object source, List<Object> args) {
        checkArgument(args.size() == 0, "Cannot compute nonNull operation, expected 0 arguments, found %s",
                args.size());
        return nonNull(source);
    }
}
