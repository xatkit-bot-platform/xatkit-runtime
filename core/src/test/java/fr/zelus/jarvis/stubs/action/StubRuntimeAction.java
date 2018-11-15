package fr.zelus.jarvis.stubs.action;

import fr.zelus.jarvis.core.RuntimeAction;
import fr.zelus.jarvis.core.RuntimePlatform;
import fr.zelus.jarvis.core.session.JarvisSession;

public class StubRuntimeAction extends RuntimeAction {

    private boolean actionProcessed;

    public StubRuntimeAction(RuntimePlatform runtimePlatform) {
        super(runtimePlatform, new JarvisSession("id"));
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
