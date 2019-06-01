package com.xatkit.stubs.action;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.session.JarvisSession;

public class StubRuntimeActionNoParameter extends RuntimeAction {

    public StubRuntimeActionNoParameter(RuntimePlatform runtimePlatform, JarvisSession session) {
        super(runtimePlatform, session);
    }

    @Override
    public Object compute() {
        return null;
    }
}
