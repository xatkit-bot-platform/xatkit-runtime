package fr.zelus.jarvis.stubs.action;

import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.JarvisModule;
import fr.zelus.jarvis.core.session.JarvisSession;

public class ErroringStubJarvisAction extends JarvisAction {

    private boolean actionProcessed;

    public ErroringStubJarvisAction(JarvisModule containingModule) {
        super(containingModule, new JarvisSession("id"));
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
