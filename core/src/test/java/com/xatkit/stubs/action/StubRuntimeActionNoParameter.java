package com.xatkit.stubs.action;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.execution.StateContext;

public class StubRuntimeActionNoParameter extends RuntimeAction {

    public StubRuntimeActionNoParameter(RuntimePlatform runtimePlatform, StateContext context) {
        super(runtimePlatform, context);
    }

    @Override
    public Object compute() {
        return null;
    }
}
