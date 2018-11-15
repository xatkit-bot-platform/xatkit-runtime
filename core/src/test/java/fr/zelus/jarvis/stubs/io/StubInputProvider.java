package fr.zelus.jarvis.stubs.io;

import fr.zelus.jarvis.io.EventProvider;
import fr.zelus.jarvis.stubs.EmptyRuntimePlatform;
import org.apache.commons.configuration2.Configuration;

public class StubInputProvider extends EventProvider<EmptyRuntimePlatform> {

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