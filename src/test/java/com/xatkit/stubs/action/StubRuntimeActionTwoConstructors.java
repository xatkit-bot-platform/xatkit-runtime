package com.xatkit.stubs.action;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.execution.StateContext;

import java.util.List;

public class StubRuntimeActionTwoConstructors extends RuntimeAction {

    private String param;

    private List<String> listParam;

    public StubRuntimeActionTwoConstructors(RuntimePlatform runtimePlatform, StateContext context, String param) {
        super(runtimePlatform, context);
        this.param = param;
    }

    public StubRuntimeActionTwoConstructors(RuntimePlatform runtimePlatform, StateContext context, List<String>
            listParam) {
        super(runtimePlatform, context);
        this.listParam = listParam;
    }

    public String getParam() {
        return this.param;
    }

    public List<String> getListParam() {
        return this.listParam;
    }

    @Override
    public Object compute() {
        return null;
    }
}
