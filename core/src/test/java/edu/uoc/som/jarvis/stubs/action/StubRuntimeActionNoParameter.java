package edu.uoc.som.jarvis.stubs.action;

import edu.uoc.som.jarvis.core.platform.RuntimePlatform;
import edu.uoc.som.jarvis.core.platform.action.RuntimeAction;
import edu.uoc.som.jarvis.core.session.JarvisSession;

public class StubRuntimeActionNoParameter extends RuntimeAction {

    public StubRuntimeActionNoParameter(RuntimePlatform runtimePlatform, JarvisSession session) {
        super(runtimePlatform, session);
    }

    @Override
    public Object compute() {
        return null;
    }
}
