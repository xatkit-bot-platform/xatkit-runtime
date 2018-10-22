package fr.zelus.jarvis.stubs;

import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.core.JarvisModule;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.orchestration.ActionInstance;
import fr.zelus.jarvis.stubs.action.ErroringStubJarvisAction;
import fr.zelus.jarvis.stubs.action.StubJarvisAction;
import org.apache.commons.configuration2.Configuration;

import java.text.MessageFormat;

public class StubJarvisModule extends JarvisModule {

    private StubJarvisAction jarvisAction;

    private ErroringStubJarvisAction erroringJarvisAction;

    public StubJarvisModule(JarvisCore jarvisCore, Configuration configuration) {
        super(jarvisCore, configuration);
        this.jarvisAction = new StubJarvisAction(this);
        this.erroringJarvisAction = new ErroringStubJarvisAction(this);
    }

    public StubJarvisAction getAction() {
        return jarvisAction;
    }

    public ErroringStubJarvisAction getErroringAction() {
        return erroringJarvisAction;
    }

    @Override
    public JarvisAction createJarvisAction(ActionInstance actionInstance, JarvisSession session) {
        if(actionInstance.getAction().getName().equals("StubJarvisAction")) {
            return jarvisAction;
        } else if(actionInstance.getAction().getName().equals("ErroringStubJarvisAction")) {
            return erroringJarvisAction;
        } else {
            throw new RuntimeException(MessageFormat.format("Cannot create the action {0}", actionInstance.getAction()
                    .getName()));
        }
    }

}
