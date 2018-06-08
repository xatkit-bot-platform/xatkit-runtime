package fr.zelus.jarvis.stubs.action;

import fr.zelus.jarvis.core.JarvisAction;

public class StubJarvisAction extends JarvisAction {

    private boolean actionProcessed;

    public boolean isActionProcessed() {
        return actionProcessed;
    }

    @Override
    public void run() {
        this.actionProcessed = true;
    }
}
