package com.xatkit.core.platform.action;

import com.xatkit.core.ExecutionService;
import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.platform.ActionDefinition;
import lombok.Getter;
import lombok.NonNull;

import java.util.concurrent.Callable;

/**
 * The concrete implementation of an {@link ActionDefinition} definition.
 * <p>
 * A {@link RuntimeAction} represents an atomic action that are automatically executed by the
 * {@link ExecutionService}
 * component. Instances of this class are created by the associated {@link RuntimePlatform} from an input
 * {@link RecognizedIntent}.
 * <p>
 * Note that {@link RuntimeAction}s implementations must be stored in the <i>action</i> package of their associated
 * concrete {@link RuntimePlatform} implementation to enable their automated loading. For example, the action
 * <i>MyAction</i> defined in the platform <i>myPlatformPackage.MyPlatform</i> should be stored in the package
 * <i>myPlatformPackage.action</i>
 *
 * @param <T> the concrete {@link RuntimePlatform} subclass type containing the action
 * @see ActionDefinition
 * @see ExecutionService
 * @see RuntimePlatform
 */
public abstract class RuntimeAction<T extends RuntimePlatform> implements Callable<RuntimeActionResult> {

    /**
     * The {@link RuntimePlatform} subclass containing this action.
     */
    protected T runtimePlatform;

    /**
     * The {@link XatkitSession} associated to this action.
     */
    @Getter
    protected XatkitSession session;

    /**
     * Constructs a new {@link RuntimeAction} with the provided {@code runtimePlatform} and {@code session}.
     *
     * @param runtimePlatform the {@link RuntimePlatform} containing this action
     * @param session         the {@link XatkitSession} associated to this action
     * @throws NullPointerException if the provided {@code runtimePlatform} or {@code session} is {@code null}
     */
    public RuntimeAction(@NonNull T runtimePlatform, @NonNull XatkitSession session) {
        this.runtimePlatform = runtimePlatform;
        this.session = session;
    }

    /**
     * A hook method that is called after {@link RuntimeAction}.
     * <p>
     * This method can be extended by subclasses to add post-construction computation, such as setting additional
     * fields, checking invariants once the {@link RuntimeAction} has been initialized, etc.
     */
    public void init() {

    }

    /**
     * Disable the default constructor, RuntimeActions must be constructed with their containing runtimePlatform.
     */
    private RuntimeAction() {
        /*
         * Disable the default constructor, RuntimeActions must be constructed with their containing runtimePlatform.
         */
    }

    /**
     * Runs the {@link RuntimeAction} and returns its result wrapped in a {@link RuntimeActionResult}.
     * <p>
     * This method should not be called manually, and is handled by the {@link ExecutionService} component that
     * manages and executes {@link RuntimeAction}s.
     * <p>
     * This method does not throw any {@link Exception} if the underlying {@link RuntimeAction}'s computation does not
     * complete. Exceptions thrown during the {@link RuntimeAction}'s computation can be accessed through the
     * {@link RuntimeActionResult#getThrowable()} method.
     *
     * @return the {@link RuntimeActionResult} containing the raw result of the computation and monitoring information
     * @see ExecutionService
     * @see RuntimeActionResult
     */
    @Override
    public RuntimeActionResult call() {
        Object computationResult = null;
        Throwable callThrowable = null;
        long before = System.currentTimeMillis();
        try {
            computationResult = compute();
        } catch (Throwable e) {
            callThrowable = e;
        }
        long after = System.currentTimeMillis();
        /*
         * Construct the RuntimeAction result from the gathered information. Note that the constructor accepts a null
         * value for the thrownException parameter, that will set accordingly the isError() helper.
         */
        return new RuntimeActionResult(computationResult, callThrowable, (after - before));
    }

    /**
     * The concrete implementation of the {@link RuntimeAction}'s computation.
     * <p>
     * This method is internally called by the {@link #call()} method to perform the raw computation and wrap the
     * results in a {@link RuntimeActionResult}. Note that {@link #compute()} can return raw computation result, and
     * do not have to deal with setting the monitoring information of the created {@link RuntimeActionResult}.
     * <p>
     * This method should be overriden by subclasses to implement the {@link RuntimeAction}'s computation logic.
     *
     * @return the raw result of the {@link RuntimeAction}'s computation
     * @throws Exception if an error occurred when computing the {@link RuntimeAction}
     */
    protected abstract Object compute() throws Exception;

}
