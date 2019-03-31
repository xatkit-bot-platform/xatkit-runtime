package edu.uoc.som.jarvis.core.interpreter.operation.object;

import edu.uoc.som.jarvis.common.OperationCall;
import edu.uoc.som.jarvis.core.interpreter.OperationProvider;
import edu.uoc.som.jarvis.core.interpreter.operation.Operation;

/**
 * A generic {@link OperationProvider} returning reflexive {@link Operation}s.
 * <p>
 * The returned {@link Operation}s are wrapper around reflexive calls of their source object's method defined by the
 * provided {@link OperationCall} name and arguments.
 * <p>
 * This class can be extended by specific {@link OperationProvider}s to return fallback {@link Operation}s when
 * object-specific {@link Operation}s can't be returned.
 */
public class ObjectOperationProvider implements OperationProvider {

    /**
     * Returns a reflexive {@link Operation} from the provided {@link OperationCall}.
     * <p>
     * The returned {@link Operation} is a wrapper around reflexive calls of its source object's method defined by the
     * provided {@link OperationCall} name and arguments (see {@link ObjectOperation}).
     *
     * @param abstractOperation the {@link OperationCall} to retrieve the implementation from
     * @return a reflexive {@link Operation} from the provided {@link OperationCall}
     * @see ObjectOperation
     */
    @Override
    public Operation getOperation(OperationCall abstractOperation) {
        if(abstractOperation.getName().equals("+")) {
            return new AdditionOperation();
        } else if(abstractOperation.getName().equals("or")) {
            return new OrOperation();
        } else if (abstractOperation.getName().equals("and")) {
            return new AndOperation();
        } else {
            return new ObjectOperation(abstractOperation.getName());
        }
    }
}
