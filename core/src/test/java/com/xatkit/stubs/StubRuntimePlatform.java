package edu.uoc.som.jarvis.stubs;

import edu.uoc.som.jarvis.core.JarvisCore;
import edu.uoc.som.jarvis.core.interpreter.ExecutionContext;
import edu.uoc.som.jarvis.core.platform.RuntimePlatform;
import edu.uoc.som.jarvis.core.platform.action.RuntimeAction;
import edu.uoc.som.jarvis.core.session.JarvisSession;
import edu.uoc.som.jarvis.execution.ActionInstance;
import edu.uoc.som.jarvis.stubs.action.ErroringStubRuntimeAction;
import edu.uoc.som.jarvis.stubs.action.StubRuntimeAction;
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
    public RuntimeAction createRuntimeAction(ActionInstance actionInstance, JarvisSession session,
                                             ExecutionContext context) {
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
