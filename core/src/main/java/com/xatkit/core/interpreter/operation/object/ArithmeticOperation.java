package com.xatkit.core.interpreter.operation.object;

import com.xatkit.core.interpreter.operation.Operation;

import javax.annotation.Nonnull;
import java.util.List;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * A generic arithmetic operation.
 * <p>
 * This abstract class contains common behavior reused by all the concrete arithmetic operations (in particular
 * {@code source} and {@code args} checks). The core logic of subclasses' operations is implemented in the
 * {@link #doOperation(Number, Number)} method.
 */
public abstract class ArithmeticOperation implements Operation {

    /**
     * Computes the {@link ArithmeticOperation} over the provided {@code source} and {@code args}.
     * <p>
     * This method performs generic checks reused by concrete arithmetic operations. The core logic of subclasses'
     * operations is implemented in the {@link #doOperation(Number, Number)} method.
     * <p>
     * This method checks that the provided {@code source} is an {@link Number} instance, and that the provided
     * {@code args} list contains a single element which is also an {@link Number} instance.
     *
     * @param source the {@link Object} to invoke the {@link Operation} on
     * @param args   the arguments of the operation to invoke
     * @return the result of the arithmetic operation
     * @throws NullPointerException     if the provided {@code source}, {@code args}, or {@code args}'s first element is
     *                                  {@code null}
     * @throws IllegalArgumentException if the provided {@code source} or {@code args}'s first element is not an
     *                                  {@link Number} instance
     */
    @Override
    public final Object invoke(Object source, List<Object> args) {
        checkNotNull(args, "Cannot compute %s operation on the provided argument list %s",
                this.getClass().getSimpleName(), args);
        checkArgument(args.size() == 1, "Cannot compute %s operation, expected 1 argument, found %s",
                this.getClass().getSimpleName(), args.size());
        checkNotNull(source, "Cannot compute %s operation on source element %s", this.getClass().getSimpleName(),
                source);
        checkArgument(source instanceof Number, "Cannot compute %s operation on source element %s, expected an " +
                        "%s, found %s", this.getClass().getSimpleName(), source, Number.class.getSimpleName(),
                source.getClass().getSimpleName());
        checkNotNull(args.get(0), "Cannot compute %s operation on target element %s",
                this.getClass().getSimpleName(), args.get(0));
        checkArgument(args.get(0) instanceof Number, "Cannot compute %s operation on target element %s, expected an " +
                        "%s, found %s", this.getClass().getSimpleName(), args.get(0), Number.class.getSimpleName(),
                source.getClass().getSimpleName());
        return doOperation((Number) source, (Number) args.get(0));
    }

    /**
     * The concrete arithmetic operation to compute.
     * <p>
     * This method is extended by concrete subclasses to define the operation's logic. Note that this method takes
     * two {@link Integer} instances as parameters.
     *
     * @param source the source of the arithmetic operation to perform
     * @param target the target of the arithmetic operation to perform
     * @return the result of the arithmetic operation
     */
    protected abstract Object doOperation(@Nonnull Number source, @Nonnull Number target);
}
