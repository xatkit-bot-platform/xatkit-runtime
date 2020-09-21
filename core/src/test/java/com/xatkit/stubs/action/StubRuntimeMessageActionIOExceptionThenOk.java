package com.xatkit.stubs.action;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.execution.StateContext;

import java.io.IOException;

public class StubRuntimeMessageActionIOExceptionThenOk extends StubRuntimeMessageAction {

    public StubRuntimeMessageActionIOExceptionThenOk(RuntimePlatform runtimePlatform, StateContext context, String
            rawMessage) {
        super(runtimePlatform, context, rawMessage);
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
