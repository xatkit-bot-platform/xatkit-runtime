package com.xatkit.stubs.action;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.action.RuntimeMessageAction;
import com.xatkit.core.session.XatkitSession;
import fr.inria.atlanmod.commons.log.Log;

import java.util.UUID;

public class StubRuntimeMessageAction extends RuntimeMessageAction {

    public static String RESULT = "result";

    protected int attempts;

    private XatkitSession clientSession = new XatkitSession(UUID.randomUUID().toString());

    public StubRuntimeMessageAction(RuntimePlatform runtimePlatform, XatkitSession session, String rawMessage) {
        super(runtimePlatform, session, rawMessage);
        attempts = 0;
    }

    @Override
    protected Object compute() throws Exception {
        Log.info("Computing {0}, message stub: {1}", this.getClass().getSimpleName(), this.message);
        attempts++;
        return RESULT;
    }

    @Override
    protected XatkitSession getClientStateContext() {
        return clientSession;
    }

    public int getAttempts() {
        return attempts;
    }
}
