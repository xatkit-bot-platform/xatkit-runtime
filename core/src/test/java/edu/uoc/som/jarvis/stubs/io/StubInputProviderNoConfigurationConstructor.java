package edu.uoc.som.jarvis.stubs.io;

        import edu.uoc.som.jarvis.core.platform.io.RuntimeEventProvider;
import edu.uoc.som.jarvis.stubs.EmptyRuntimePlatform;

public class StubInputProviderNoConfigurationConstructor extends RuntimeEventProvider<EmptyRuntimePlatform> {

    public StubInputProviderNoConfigurationConstructor(EmptyRuntimePlatform runtimePlatform) {
        super(runtimePlatform);
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