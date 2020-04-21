package com.xatkit.stubs;

import com.xatkit.core.XatkitCore;
import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.stubs.action.ErroringStubRuntimeAction;
import com.xatkit.stubs.action.StubRuntimeAction;
import com.xatkit.util.ExecutionModelUtils;
import org.apache.commons.configuration2.Configuration;
import org.eclipse.xtext.xbase.XMemberFeatureCall;

import java.text.MessageFormat;
import java.util.List;

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
    public RuntimeAction createRuntimeAction(XMemberFeatureCall actionCall, List<Object> arguments,
                                             XatkitSession session) {
        if (ExecutionModelUtils.getActionName(actionCall).equals("StubRuntimeAction")) {
            return runtimeAction;
        } else if (ExecutionModelUtils.getActionName(actionCall).equals("ErroringStubRuntimeAction")) {
            return erroringRuntimeAction;
        } else {
            throw new RuntimeException(MessageFormat.format("Cannot create the action {0}",
                    ExecutionModelUtils.getActionName(actionCall)));
        }
    }

}
