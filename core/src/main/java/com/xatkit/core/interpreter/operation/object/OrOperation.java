package com.xatkit.core.interpreter.operation.object;

import com.xatkit.core.interpreter.operation.Operation;

import java.text.MessageFormat;
import java.util.List;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;

/**
 * A boolean {@code or} {@link Operation}.
 */
public class OrOperation implements Operation {

    /**
     * Computes a boolean {@code or} operation over the provided {@code source} and {@code args}.
     *
     * @param source the {@link Object} to invoke the {@link OrOperation} on
     * @param args   the arguments of the operation to invoke
     * @return the result of the boolean {@code or} operation
     * @throws IllegalArgumentException if the provided {@code args} does not exactly contain one element (i.e. the
     *                                  second element of the {@code or} operation)
     */
    @Override
    public Object invoke(Object source, List<Object> args) {
        checkArgument(args.size() == 1, "Cannot compute or operation, expected 1 argument, found %s", args.size());
        if (source instanceof Boolean) {
            return (Boolean) source || (Boolean) args.get(0);
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Cannot compute or operation on {0}, {1}", source
                    , args.get(0)));
        }
    }
}
