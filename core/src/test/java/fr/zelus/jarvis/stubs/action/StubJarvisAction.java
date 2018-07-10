package fr.zelus.jarvis.stubs.action;

import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.JarvisModule;
import fr.zelus.jarvis.core.session.JarvisSession;

public class StubJarvisAction extends JarvisAction {

    private boolean actionProcessed;

    public StubJarvisAction(JarvisModule containingModule) {
        super(containingModule, new JarvisSession("id"));
    }

    public boolean isActionProcessed() {
        return actionProcessed;
    }

    @Override
    public Object call() {
        this.actionProcessed = true;
        return null;
    }
}
