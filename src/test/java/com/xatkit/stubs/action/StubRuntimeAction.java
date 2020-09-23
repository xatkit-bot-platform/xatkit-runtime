package com.xatkit.stubs.action;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.StateContext;

public class StubRuntimeAction extends RuntimeAction {

    private boolean actionProcessed;

    private static StateContext getStateContext() {
        StateContext context = ExecutionFactory.eINSTANCE.createStateContext();
        context.setContextId("id");
        return context;
    }

    public StubRuntimeAction(RuntimePlatform runtimePlatform) {
        super(runtimePlatform, getStateContext());
    }

    public boolean isActionProcessed() {
        return actionProcessed;
    }

    @Override
    public Object compute() {
        this.actionProcessed = true;
        return null;
    }
}
