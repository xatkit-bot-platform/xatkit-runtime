package com.xatkit.core.interpreter.operation.object;


import com.xatkit.core.interpreter.operation.Operation;

import java.util.List;
import java.util.Objects;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;

/**
 * A boolean {@code equals} {@link Operation}.
 */
public class EqualsOperation implements Operation {

    /**
     * Computes a boolean {@code equals} operation over the provided {@code source} and {@code args}.
     *
     * @param source the {@link Object} to invoke the {@link EqualsOperation} on
     * @param args   the arguments of the operation to invoke
     * @return the result of the boolean {@code equals} operation
     * @throws IllegalArgumentException if the provided {@code args} does not exactly contain one element (i.e. the
     *                                  second element of the {@code and} operation)
     */
    @Override
    public Object invoke(Object source, List<Object> args) {
        checkArgument(args.size() == 1, "Cannot compute and operation, expected 1 argument, found %s", args.size());
        /*
         * Float comparisons are strange: 2.0f == 2 is true, but new Float(2.0f).equals(new Integer(2)) is false, we
         * box everything in Floats if one of the operand is a Float to be sure the comparison is correct.
         */
        if ((source instanceof Float && args.get(0) instanceof Number) || (source instanceof Number && args.get(0) instanceof Float)) {
            return Float.compare(((Number) source).floatValue(), ((Number) args.get(0)).floatValue()) == 0;
        }
        return Objects.equals(source, args.get(0));
    }
}
