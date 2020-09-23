package com.xatkit.core.platform.action;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.execution.StateContext;
import fr.inria.atlanmod.commons.log.Log;
import lombok.Getter;
import lombok.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.concurrent.Callable;

/**
 * Wraps an executable action performed by a bot.
 * <p>
 * {@link RuntimeAction} are managed by a {@link RuntimePlatform} that contains shareable information such as
 * platform credentials, installation identifier, or database connection. Subclasses can access their containing
 * platform using {@code this.getRuntimePlatform()}.
 *
 * @param <T> the {@link RuntimePlatform} containing the action
 * @see RuntimePlatform
 */
public abstract class RuntimeAction<T extends RuntimePlatform> implements Callable<RuntimeActionResult> {

    /**
     * The {@link RuntimePlatform} subclass containing this action.
     */
    protected T runtimePlatform;

    /**
     * The {@link StateContext} associated to this action.
     */
    @Getter
    protected StateContext context;

    /**
     * Constructs a {@link RuntimeAction} managed by the provided {@code platform} with the given {@code context}.
     * @param platform the {@link RuntimePlatform} managing this action
     * @param context the current {@link StateContext}
     */
    public RuntimeAction(@NonNull T platform, @NonNull StateContext context) {
        this.runtimePlatform = platform;
        this.context = context;
    }

    /**
     * A hook method that is called after {@link RuntimeAction} construction.
     * <p>
     * This method can be extended by subclasses to add post-construction computation, such as setting additional
     * fields, checking invariants once the {@link RuntimeAction} has been initialized, etc.
     */
    public void init() {

    }

    /**
     * Disable the default constructor, RuntimeActions must be constructed with their containing platform.
     */
    private RuntimeAction() {
        /*
         * Disable the default constructor, RuntimeActions must be constructed with their containing platform.
         */
    }

    /**
     * Runs the {@link RuntimeAction} and returns its result wrapped in a {@link RuntimeActionResult}.
     * <p>
     * This method does not throw any {@link Exception} if the underlying {@link RuntimeAction}'s computation does not
     * complete. Exceptions thrown during the {@link RuntimeAction}'s computation can be accessed through the
     * {@link RuntimeActionResult#getThrowable()} method.
     *
     * @return the {@link RuntimeActionResult} containing the raw result of the computation and monitoring information
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
            Log.error("An error occurred when executing the action {0}", this.getClass().getSimpleName());
            printStackTrace(callThrowable);
        }
        long after = System.currentTimeMillis();
        Log.info("Action {0} executed in {1} ms", this.getClass().getSimpleName(), (after - before));
        /*
         * Construct the RuntimeAction result from the gathered information. Note that the constructor accepts a null
         * value for the thrownException parameter, that will set accordingly the isError() helper.
         */
        return new RuntimeActionResult(computationResult, callThrowable, (after - before));
    }

    /**
     * Prints the stack trace of the provided {@code throwable} in the default output.
     * @param throwable the {@link Throwable} to print the stack trace of
     */
    private void printStackTrace(Throwable throwable) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter printWriter = new PrintWriter(baos, true);
        throwable.printStackTrace(printWriter);
        Log.error("{0}", baos.toString());
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
