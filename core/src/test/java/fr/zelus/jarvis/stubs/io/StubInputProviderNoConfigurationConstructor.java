package fr.zelus.jarvis.stubs.io;

        import fr.zelus.jarvis.core.platform.io.RuntimeEventProvider;
        import fr.zelus.jarvis.stubs.EmptyRuntimePlatform;

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