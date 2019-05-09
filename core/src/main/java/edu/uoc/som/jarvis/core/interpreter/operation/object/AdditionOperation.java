package edu.uoc.som.jarvis.core.interpreter.operation.object;

import edu.uoc.som.jarvis.core.interpreter.operation.Operation;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;

/**
 * A generic addition {@link Operation}.
 * <p>
 * This {@link Operation} can process {@link String} and {@link Integer} sources. The former are handled using
 * {@link String#concat(String)}, while the second one is handled by an arithmetic addition.
 */
public class AdditionOperation implements Operation {

    /**
     * Computes a generic addition over the provided {@code source} and {@code args}.
     * <p>
     * This method performs a {@link String} concatenation if the provided {@code source} is a {@link String}, or an
     * arithmetic addition in case it is an {@link Integer}.
     *
     * @param source the {@link Object} to invoke the {@link AdditionOperation} on
     * @param args   the arguments of the operation to invoke
     * @return the result of the addition
     * @throws IllegalArgumentException if the provided {@code args} does not exactly contain one element (i.e. the
     *                                  element to add to {@code source})
     */
    @Override
    public Object invoke(Object source, List<Object> args) {
        checkArgument(args.size() == 1, "Cannot compute + operation, expected 1 argument, found %s", args.size());
        if (source instanceof String) {
            /*
             * Perform a concatenation (use Objects.toString to avoid NPEs)
             */
            return ((String) source).concat(Objects.toString(args.get(0)));
        } else if (source instanceof Integer) {
            return (Integer) source + (Integer) args.get(0);
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Cannot compute + operation on {0}, {1}", source,
                    args.get(0)));
        }
    }
}
