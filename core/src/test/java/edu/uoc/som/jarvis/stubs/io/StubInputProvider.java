package edu.uoc.som.jarvis.stubs.io;

import edu.uoc.som.jarvis.core.platform.io.RuntimeEventProvider;
import edu.uoc.som.jarvis.stubs.EmptyRuntimePlatform;
import org.apache.commons.configuration2.Configuration;

public class StubInputProvider extends RuntimeEventProvider<EmptyRuntimePlatform> {

    public StubInputProvider(EmptyRuntimePlatform runtimePlatform) {
        super(runtimePlatform);
    }

    public StubInputProvider(EmptyRuntimePlatform runtimePlatform, Configuration configuration) {
        this(runtimePlatform);
    }

    @Override
    public void run() {
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {

            }
        }
    }

}