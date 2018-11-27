package fr.zelus.jarvis.stubs.action;

import fr.zelus.jarvis.core.platform.action.RuntimeAction;
import fr.zelus.jarvis.core.platform.RuntimePlatform;
import fr.zelus.jarvis.core.session.JarvisSession;

public class StubRuntimeActionNoParameter extends RuntimeAction {

    public StubRuntimeActionNoParameter(RuntimePlatform runtimePlatform, JarvisSession session) {
        super(runtimePlatform, session);
    }

    @Override
    public Object compute() {
        return null;
    }
}
