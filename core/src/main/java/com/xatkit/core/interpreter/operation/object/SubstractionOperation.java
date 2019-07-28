package com.xatkit.core.interpreter.operation.object;

import com.xatkit.core.interpreter.operation.Operation;

import java.text.MessageFormat;
import java.util.List;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;

/**
 * A generic substraction {@link Operation}.
 * <p>
 * This {@link Operation} can process {@link Integer} sources, that are handled by an arithmetic substraction.
 */
public class SubstractionOperation implements Operation {

    /**
     * Computes a generic substraction over the provided {@code source} and {@code args}.
     *
     * @param source the {@link Object} to invoke the {@link SubstractionOperation} on
     * @param args   the arguments of the operation to invoke
     * @return the result of the substraction
     * @throws IllegalArgumentException if the provided {@code args} does not exactly contain one element (i.e. the
     *                                  element to subtract to the {@code source})
     */
    @Override
    public Object invoke(Object source, List<Object> args) {
        checkArgument(args.size() == 1, "Cannot compute - operation, expected 1 argument, found %s", args.size());
        if (source instanceof Integer) {
            return (Integer) source - (Integer) args.get(0);
        } else if(source instanceof Float) {
            return (Float) source - (Float) args.get(0);
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Cannot compute - operation on {0}, {1}", source,
                    args.get(0)));
        }
    }
}
