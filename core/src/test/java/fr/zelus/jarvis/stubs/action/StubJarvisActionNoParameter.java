package fr.zelus.jarvis.stubs.action;

import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.JarvisModule;
import fr.zelus.jarvis.core.session.JarvisSession;

public class StubJarvisActionNoParameter extends JarvisAction {

    public StubJarvisActionNoParameter(JarvisModule containingModule, JarvisSession session) {
        super(containingModule, session);
    }

    @Override
    public Object call() {
        return null;
    }
}
