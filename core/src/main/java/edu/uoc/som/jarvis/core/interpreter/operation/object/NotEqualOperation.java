package edu.uoc.som.jarvis.core.interpreter.operation.object;


import edu.uoc.som.jarvis.core.interpreter.operation.Operation;

import java.util.List;
import java.util.Objects;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;

/**
 * A boolean {@code not equal} {@link Operation}.
 */
public class NotEqualOperation implements Operation {

    /**
     * Computes a boolean {@code not equal} operation over the provided {@code source} and {@code args}.
     *
     * @param source the {@link Object} to invoke the {@link NotEqualOperation} on
     * @param args   the arguments of the operation to invoke
     * @return the result of the boolean {@code not equal} operation
     * @throws IllegalArgumentException if the provided {@code args} does not exactly contain one element (i.e. the
     *                                  second element of the {@code and} operation)
     */
    @Override
    public Object invoke(Object source, List<Object> args) {
        checkArgument(args.size() == 1, "Cannot compute and operation, expected 1 argument, found %s", args.size());
        return !Objects.equals(source, args.get(0));
    }
}
