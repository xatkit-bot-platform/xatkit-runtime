package edu.uoc.som.jarvis.stubs.action;

import edu.uoc.som.jarvis.core.platform.RuntimePlatform;
import edu.uoc.som.jarvis.core.session.JarvisSession;

import java.io.IOException;

public class StubRuntimeMessageActionIOException extends StubRuntimeMessageAction {

    public StubRuntimeMessageActionIOException(RuntimePlatform runtimePlatform, JarvisSession session, String
            rawMessage) {
        super(runtimePlatform, session, rawMessage);
    }

    @Override
    protected Object compute() throws IOException {
        attempts++;
        throw new IOException("StubRuntimeMessageActionIOException");
    }
}
