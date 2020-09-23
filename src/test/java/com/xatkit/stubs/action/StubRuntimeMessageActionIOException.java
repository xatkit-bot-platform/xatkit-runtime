package com.xatkit.stubs.action;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.execution.StateContext;

import java.io.IOException;

public class StubRuntimeMessageActionIOException extends StubRuntimeMessageAction {

    public StubRuntimeMessageActionIOException(RuntimePlatform runtimePlatform, StateContext context, String
            rawMessage) {
        super(runtimePlatform, context, rawMessage);
    }

    @Override
    protected Object compute() throws IOException {
        attempts++;
        throw new IOException("StubRuntimeMessageActionIOException");
    }
}
