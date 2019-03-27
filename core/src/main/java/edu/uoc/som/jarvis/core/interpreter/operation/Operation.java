package edu.uoc.som.jarvis.core.interpreter.operation;

import edu.uoc.som.jarvis.core.interpreter.OperationProvider;

import java.util.List;

/**
 * A concrete operation that can be computed by the common interpreter.
 * <p>
 * {@link Operation} instances are created by an {@link OperationProvider} from the abstract
 * {@link edu.uoc.som.jarvis.common.OperationCall}s defined in the common metamodel.
 */
public interface Operation {

    /**
     * Invokes the {@link Operation} on the provided {@code source} with the given {@code args}.
     *
     * @param source the {@link Object} to invoke the {@link Operation} on
     * @param args   the arguments of the operation to invoke
     * @return the execution result (not that this result may be {@code null})
     */
    Object invoke(Object source, List<Object> args);
}
