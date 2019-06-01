package com.xatkit.core.interpreter;

import com.xatkit.common.OperationCall;
import com.xatkit.core.interpreter.operation.Operation;

/**
 * Provides concrete {@link Operation} instances from abstract {@link OperationCall}s.
 * <p>
 * This provider allows to bind abstract operations to specific implementations, and can be implemented by any object
 * to provide custom implementation of abstract operations to the interpreter.
 */
public interface OperationProvider {

    /**
     * Returns the concrete {@link Operation} associated to the provided abstract {@link OperationCall}.
     *
     * @param abstractOperation the {@link OperationCall} to retrieve the implementation from
     * @return the concrete {@link Operation} associated to the provided abstract {@link OperationCall}
     */
    Operation getOperation(OperationCall abstractOperation);
}
