package fr.zelus.jarvis.stubs.action;

import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.JarvisModule;

public class StubJarvisAction extends JarvisAction {

    private boolean actionProcessed;

    public StubJarvisAction(JarvisModule containingModule) {
        super(containingModule);
    }

    public boolean isActionProcessed() {
        return actionProcessed;
    }

    @Override
    public void run() {
        this.actionProcessed = true;
    }
}
