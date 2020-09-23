package com.xatkit.stubs.action;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.action.RuntimeMessageAction;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.StateContext;
import fr.inria.atlanmod.commons.log.Log;

import java.util.UUID;

public class StubRuntimeMessageAction extends RuntimeMessageAction {

    public static String RESULT = "result";

    protected int attempts;

    private StateContext clientStateContext;

    public StubRuntimeMessageAction(RuntimePlatform runtimePlatform, StateContext context, String rawMessage) {
        super(runtimePlatform, context, rawMessage);
        this.clientStateContext = ExecutionFactory.eINSTANCE.createStateContext();
        this.clientStateContext.setContextId(UUID.randomUUID().toString());
        attempts = 0;
    }

    @Override
    protected Object compute() throws Exception {
        Log.info("Computing {0}, message stub: {1}", this.getClass().getSimpleName(), this.message);
        attempts++;
        return RESULT;
    }

    @Override
    protected StateContext getClientStateContext() {
        return clientStateContext;
    }

    public int getAttempts() {
        return attempts;
    }
}
