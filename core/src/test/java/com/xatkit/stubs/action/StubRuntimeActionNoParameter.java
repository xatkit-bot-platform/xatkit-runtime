package com.xatkit.stubs.action;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.session.XatkitSession;

public class StubRuntimeActionNoParameter extends RuntimeAction {

    public StubRuntimeActionNoParameter(RuntimePlatform runtimePlatform, XatkitSession session) {
        super(runtimePlatform, session);
    }

    @Override
    public Object compute() {
        return null;
    }
}
