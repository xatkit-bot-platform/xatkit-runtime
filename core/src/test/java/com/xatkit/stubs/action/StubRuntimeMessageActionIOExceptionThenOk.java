package com.xatkit.stubs.action;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.session.XatkitSession;

import java.io.IOException;

public class StubRuntimeMessageActionIOExceptionThenOk extends StubRuntimeMessageAction {

    public StubRuntimeMessageActionIOExceptionThenOk(RuntimePlatform runtimePlatform, XatkitSession session, String
            rawMessage) {
        super(runtimePlatform, session, rawMessage);
    }

    @Override
    protected Object compute() throws IOException {
        attempts++;
        if(attempts == 1) {
            throw new IOException("StubRuntimeMessageActionIOException");
        } else {
            return RESULT;
        }
    }



}
