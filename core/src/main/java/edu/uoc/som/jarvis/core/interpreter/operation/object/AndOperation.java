package edu.uoc.som.jarvis.core.interpreter.operation.object;


import edu.uoc.som.jarvis.core.interpreter.operation.Operation;

import java.text.MessageFormat;
import java.util.List;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;

/**
 * A boolean {@code and} {@link Operation}.
 */
public class AndOperation implements Operation {

    /**
     * Computes a boolean {@code and} operation over the provided {@code source} and {@code args}.
     *
     * @param source the {@link Object} to invoke the {@link AndOperation} on
     * @param args   the arguments of the operation to invoke
     * @return the result of the boolean {@code and} operation
     * @throws IllegalArgumentException if the provided {@code args} does not exactly contain one element (i.e. the
     *                                  second element of the {@code and} operation)
     */
    @Override
    public Object invoke(Object source, List<Object> args) {
        checkArgument(args.size() == 1, "Cannot compute and operation, expected 1 argument, found %s", args.size());
        if (source instanceof Boolean) {
            return (Boolean) source && (Boolean) args.get(0);
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Cannot compute and operation on {0}, {1}",
                    source, args.get(0)));
        }
    }
}
