package com.xatkit.stubs;

import com.xatkit.core.XatkitCore;
import com.xatkit.core.interpreter.ExecutionContext;
import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.execution.ActionInstance;
import com.xatkit.stubs.action.ErroringStubRuntimeAction;
import com.xatkit.stubs.action.StubRuntimeAction;
import org.apache.commons.configuration2.Configuration;

import java.text.MessageFormat;

public class StubRuntimePlatform extends RuntimePlatform {

    private StubRuntimeAction runtimeAction;

    private ErroringStubRuntimeAction erroringRuntimeAction;

    public StubRuntimePlatform(XatkitCore xatkitCore, Configuration configuration) {
        super(xatkitCore, configuration);
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
    public RuntimeAction createRuntimeAction(ActionInstance actionInstance, XatkitSession session,
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
