package com.xatkit.stubs.action;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.StateContext;

public class ErroringStubRuntimeAction extends RuntimeAction {

    private boolean actionProcessed;

    private static StateContext getStateContext() {
        StateContext context = ExecutionFactory.eINSTANCE.createStateContext();
        context.setContextId("id");
        return context;
    }

    public ErroringStubRuntimeAction(RuntimePlatform runtimePlatform) {
        super(runtimePlatform, getStateContext());
    }

    public boolean isActionProcessed() {
        return actionProcessed;
    }

    @Override
    public Object compute() {
        this.actionProcessed = true;
        throw new RuntimeException("Error when running the action");
    }
}
