package edu.uoc.som.jarvis.stubs.action;

import edu.uoc.som.jarvis.core.platform.RuntimePlatform;
import edu.uoc.som.jarvis.core.platform.action.RuntimeAction;
import edu.uoc.som.jarvis.core.session.JarvisSession;

public class ErroringStubRuntimeAction extends RuntimeAction {

    private boolean actionProcessed;

    public ErroringStubRuntimeAction(RuntimePlatform runtimePlatform) {
        super(runtimePlatform, new JarvisSession("id"));
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
