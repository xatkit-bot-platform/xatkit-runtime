package edu.uoc.som.jarvis.core.interpreter.operation.object;

import edu.uoc.som.jarvis.core.interpreter.operation.Operation;

import java.util.List;
import java.util.Objects;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;

/**
 * Checks if the provided {@code source} is {@code null}.
 */
public class IsNullOperation implements Operation {

    /**
     * Returns whether the provided {@code source} {@link Object} is {@code null}.
     * <p>
     * This method returns the same value as {@link Objects#isNull(Object)}.
     *
     * @param source the {@link Object} to invoke the {@link IsNullOperation} on
     * @param args   the arguments of the operation to invoke
     * @return {@code true} if the provided {@code source} is {@code null}, {@code false} otherwise
     * @throws IllegalArgumentException if the provided {@code args} is not empty
     */
    @Override
    public Object invoke(Object source, List<Object> args) {
        checkArgument(args.size() == 0, "Cannot compute isNull operation, expected 0 arguments, found %s", args.size());
        return Objects.isNull(source);
    }
}
