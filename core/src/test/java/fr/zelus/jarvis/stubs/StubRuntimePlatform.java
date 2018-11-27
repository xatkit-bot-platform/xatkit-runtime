package fr.zelus.jarvis.stubs;

import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.core.platform.action.RuntimeAction;
import fr.zelus.jarvis.core.platform.RuntimePlatform;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.execution.ActionInstance;
import fr.zelus.jarvis.stubs.action.ErroringStubRuntimeAction;
import fr.zelus.jarvis.stubs.action.StubRuntimeAction;
import org.apache.commons.configuration2.Configuration;

import java.text.MessageFormat;

public class StubRuntimePlatform extends RuntimePlatform {

    private StubRuntimeAction runtimeAction;

    private ErroringStubRuntimeAction erroringRuntimeAction;

    public StubRuntimePlatform(JarvisCore jarvisCore, Configuration configuration) {
        super(jarvisCore, configuration);
        init();
    }

    public void init() {
        this.runtimeAction = new StubRuntimeAction(this);
        this.erroringRuntimeAction = new ErroringStubRuntimeAction(this);
    }

    public StubRuntimeAction getAction() {
        return runtimeAction;
    }

    public ErroringStubRuntimeAction getErroringAction() {
        return erroringRuntimeAction;
    }

    @Override
    public RuntimeAction createRuntimeAction(ActionInstance actionInstance, JarvisSession session) {
        if(actionInstance.getAction().getName().equals("StubRuntimeAction")) {
            return runtimeAction;
        } else if(actionInstance.getAction().getName().equals("ErroringStubRuntimeAction")) {
            return erroringRuntimeAction;
        } else {
            throw new RuntimeException(MessageFormat.format("Cannot create the action {0}", actionInstance.getAction()
                    .getName()));
        }
    }

}
