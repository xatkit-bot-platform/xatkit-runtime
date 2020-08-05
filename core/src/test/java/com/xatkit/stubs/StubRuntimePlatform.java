package com.xatkit.stubs;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.stubs.action.ErroringStubRuntimeAction;
import com.xatkit.stubs.action.StubRuntimeAction;

public class StubRuntimePlatform extends RuntimePlatform {

    private StubRuntimeAction runtimeAction;

    private ErroringStubRuntimeAction erroringRuntimeAction;

    public StubRuntimePlatform() {
        init();
    }

    public void init() {
        this.runtimeAction = new StubRuntimeAction(this);
        this.erroringRuntimeAction = new ErroringStubRuntimeAction(this);
    }

    public StubRuntimeAction getAction() {
        return runtimeAction;
    }

    public ErroringStubRuntimeAction getErroringAction() {
        return erroringRuntimeAction;
    }

}
